package me.HitroxVN.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;
import me.HitroxVN.util.TimeUtil;
import me.HitroxVN.util.WorldManager;
import me.HitroxVN.Main;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private final Map<UUID, PlayerSession> activeSessions = new ConcurrentHashMap<>();
    private final ItemManager itemManager = new ItemManager();

    private final LinkedList<UUID> waitingQueue = new LinkedList<>();
    private final Map<UUID, Material> pendingTargets = new HashMap<>();
    private final Map<UUID, CompletableFuture<Boolean>> worldPrepFutures = new HashMap<>();

    private final Map<UUID, Map<Integer, List<Material>>> editDrafts = new HashMap<>();
    private final Map<UUID, Integer> editPages = new HashMap<>();

    public GameManager() {
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::updateDisplays, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::processQueue, 40L, 40L);
    }

    private void processQueue() {
        if (waitingQueue.isEmpty())
            return;
        int maxConcurrent = Main.getInstance().getConfig().getInt("worlds.max-concurrent", 30);
        if (activeSessions.size() >= maxConcurrent)
            return;

        UUID nextUuid = waitingQueue.poll();
        Player player = Bukkit.getPlayer(nextUuid);
        if (player == null || !player.isOnline()) {
            pendingTargets.remove(nextUuid);
            return;
        }

        Material target = pendingTargets.remove(nextUuid);
        actualStartGame(player, target);

        int pos = 1;
        for (UUID uuid : waitingQueue) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.queue-position", "{pos}",
                        String.valueOf(pos++)));
        }
    }

    private void updateDisplays() {
        String displayType = Main.getInstance().getConfig().getString("settings.display-type", "BOTH");
        for (PlayerSession session : activeSessions.values()) {
            if (session.isFinished() || session.getStartTime() == 0)
                continue;
            Player player = Bukkit.getPlayer(session.getPlayerUUID());
            if (player == null || !player.isOnline())
                continue;

            long elapsed = System.currentTimeMillis() - session.getStartTime();
            String timeStr = TimeUtil.format(elapsed);
            String itemName = session.getTargetItem().name().replace("_", " ");

            if (displayType.equalsIgnoreCase("ACTIONBAR") || displayType.equalsIgnoreCase("BOTH")) {
                player.sendActionBar(Main.getInstance().getMessageManager().getComponent("actionbar.format", "{item}",
                        itemName, "{time}", timeStr));
            }
            if (displayType.equalsIgnoreCase("SCOREBOARD") || displayType.equalsIgnoreCase("BOTH")) {
                updateScoreboard(player, session, itemName, timeStr);
            }
        }
    }

    private void updateScoreboard(Player player, PlayerSession session, String itemName, String timeStr) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        Objective obj = board.getObjective("rsrun");
        if (obj == null) {
            String title = Main.getInstance().getConfig().getString("settings.scoreboard-title", "&b&lSPEEDRUN");
            obj = board.registerNewObjective("rsrun", Criteria.DUMMY,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        for (String entry : board.getEntries())
            board.resetScores(entry);

        String pbStr = session.getPersonalBest() == -1
                ? Main.getInstance().getMessageManager().getLegacyString("scoreboard.no-best")
                : TimeUtil.format(session.getPersonalBest());

        obj.getScore("§1").setScore(6);
        obj.getScore(Main.getInstance().getMessageManager().getLegacyString("scoreboard.target")).setScore(5);
        obj.getScore("§e" + itemName).setScore(4);
        obj.getScore("§2").setScore(3);
        obj.getScore(Main.getInstance().getMessageManager().getLegacyString("scoreboard.time", "{time}", timeStr))
                .setScore(2);
        obj.getScore(Main.getInstance().getMessageManager().getLegacyString("scoreboard.best", "{time}", pbStr))
                .setScore(1);
    }

    public void start(Player player) {
        if (activeSessions.containsKey(player.getUniqueId()) || waitingQueue.contains(player.getUniqueId())) {
            player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.world-already-in"));
            return;
        }
        if (Main.getInstance().getConfig().getBoolean("features.random-gui"))
            openRandomGUI(player);
        else
            beginCountdownSequence(player, itemManager.getRandomItem());
    }

    private void openRandomGUI(Player player) {
        String titleStr = Main.getInstance().getMessageManager().getLegacyString("gui.title");
        Inventory gui = Bukkit.createInventory(null, 27,
                LegacyComponentSerializer.legacySection().deserialize(titleStr));

        ItemStack pointer = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta pointerMeta = pointer.getItemMeta();
        pointerMeta.displayName(Main.getInstance().getMessageManager().getComponent("gui.roulette-pointer"));
        pointer.setItemMeta(pointerMeta);

        for (int i = 0; i < 27; i++) {
            if (i == 4 || i == 22)
                gui.setItem(i, pointer);
            else if (i < 9 || i > 17)
                gui.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        player.openInventory(gui);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            int maxTicks = 30 + new Random().nextInt(20);
            List<Material> cycle = new ArrayList<>();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                ticks++;
                cycle.add(0, itemManager.getRandomItem());
                if (cycle.size() > 9)
                    cycle.remove(9);
                for (int i = 0; i < 9; i++)
                    if (i < cycle.size())
                        gui.setItem(9 + i, new ItemStack(cycle.get(i)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                if (ticks >= maxTicks) {
                    Material winner = cycle.get(4);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                        player.closeInventory();
                        beginCountdownSequence(player, winner);
                    }, 20L);
                    this.cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 3L);
    }

    private void beginCountdownSequence(Player player, Material item) {
        int maxConcurrent = Main.getInstance().getConfig().getInt("worlds.max-concurrent", 30);
        if (activeSessions.size() < maxConcurrent)
            worldPrepFutures.put(player.getUniqueId(), WorldManager.prepareWorldFolder(player.getUniqueId()));

        if (Main.getInstance().getConfig().getBoolean("features.countdown"))
            startCountdown(player, item);
        else
            handleStartInput(player, item);
    }

    private void startCountdown(Player player, Material item) {
        String itemName = item.name().replace("_", " ");
        new org.bukkit.scheduler.BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    worldPrepFutures.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                if (count > 0) {
                    sendTitle(player, "countdown." + count, "countdown.subtitle", "{item}", itemName);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                } else if (count == 0) {
                    sendTitle(player, "countdown.go", "countdown.subtitle", "{item}", itemName);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    handleStartInput(player, item);
                    this.cancel();
                }
                count--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }

    private void handleStartInput(Player player, Material item) {
        int maxConcurrent = Main.getInstance().getConfig().getInt("worlds.max-concurrent", 30);
        if (activeSessions.size() >= maxConcurrent) {
            waitingQueue.add(player.getUniqueId());
            pendingTargets.put(player.getUniqueId(), item);
            player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.queue-join"));
            player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.queue-position", "{pos}",
                    String.valueOf(waitingQueue.size())));
        } else
            actualStartGame(player, item);
    }

    private void actualStartGame(Player player, Material item) {
        CompletableFuture<Boolean> prepFuture = worldPrepFutures.remove(player.getUniqueId());
        if (prepFuture == null)
            prepFuture = WorldManager.prepareWorldFolder(player.getUniqueId());

        if (!prepFuture.isDone()) {
            player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.world-finishing-up"));
        }

        prepFuture.thenAccept(success -> {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!success) {
                    player.sendMessage(
                            Main.getInstance().getMessageManager().getComponent("messages.world-init-error"));
                    return;
                }
                if (!player.isOnline()) {
                    WorldManager.deleteWorld(player.getUniqueId());
                    return;
                }

                long pb = Main.getInstance().getRecordManager().getBestTime(player.getUniqueId(), item);
                PlayerSession session = new PlayerSession(player.getUniqueId(), item, pb);
                session.setStartTime(0);
                session.setStartLocation(player.getLocation());
                session.setSavedInventory(player.getInventory().getContents());
                session.setSavedArmor(player.getInventory().getArmorContents());
                session.setSavedExp(player.getExp());
                session.setSavedLevel(player.getLevel());
                session.setSavedHealth(player.getHealth());
                session.setSavedFood(player.getFoodLevel());
                activeSessions.put(player.getUniqueId(), session);

                World world = WorldManager.loadWorld(player.getUniqueId());
                player.getInventory().clear();
                player.setExp(0);
                player.setLevel(0);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.teleport(WorldManager.getRandomLocation(world));
                session.setStartTime(System.currentTimeMillis());

                player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.target-item", "{item}",
                        item.name()));
                player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.game-started"));
            });
        });
    }

    private void sendTitle(Player player, String mainPath, String subPath, String... replacements) {
        Component main = Main.getInstance().getMessageManager().getComponent(mainPath, replacements);
        Component sub = Main.getInstance().getMessageManager().getComponent(subPath, replacements);
        Title title = Title.title(main, sub,
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(800), Duration.ofMillis(100)));
        player.showTitle(title);
    }

    public void finish(Player player) {
        PlayerSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.isFinished())
            return;
        session.setFinished(true);
        long time = System.currentTimeMillis() - session.getStartTime();
        String timeStr = TimeUtil.format(time);
        Main.getInstance().getRecordManager().setBestTime(player.getUniqueId(), session.getTargetItem(), time);

        player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.game-finished"));
        player.sendMessage(
                Main.getInstance().getMessageManager().getComponent("messages.elapsed-time", "{time}", timeStr));
        player.sendActionBar(
                Main.getInstance().getMessageManager().getComponent("messages.finish-actionbar", "{time}", timeStr));
        player.setInvulnerable(true);
        applyVictoryEffects(player);

        if (Main.getInstance().getConfig().getBoolean("features.broadcast-winner")) {
            Bukkit.broadcast(Main.getInstance().getMessageManager().getComponent("broadcast.finished", "{player}",
                    player.getName(), "{item}", session.getTargetItem().name().replace("_", " "), "{time}", timeStr));
        }

        int delaySeconds = Main.getInstance().getConfig().getInt("worlds.finish-delay", 5);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (player.isOnline()) {
                restorePlayer(player, session);
                WorldManager.deleteWorld(player.getUniqueId());
                activeSessions.remove(player.getUniqueId());
                processQueue();
            }
        }, delaySeconds * 20L);
    }

    public void restorePlayer(Player player, PlayerSession session) {
        if (player == null || !player.isOnline())
            return;
        player.teleport(session.getStartLocation());
        player.getInventory().setContents(session.getSavedInventory());
        player.getInventory().setArmorContents(session.getSavedArmor());
        player.setExp(session.getSavedExp());
        player.setLevel(session.getSavedLevel());
        player.setHealth(session.getSavedHealth() > 0 ? session.getSavedHealth() : 20);
        player.setFoodLevel(session.getSavedFood());
        player.setInvulnerable(false);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void quitEarly(Player player) {
        PlayerSession session = activeSessions.remove(player.getUniqueId());
        if (session != null)
            WorldManager.deleteWorld(player.getUniqueId());
        waitingQueue.remove(player.getUniqueId());
        pendingTargets.remove(player.getUniqueId());
        worldPrepFutures.remove(player.getUniqueId());
        processQueue();
    }

    private void applyVictoryEffects(Player player) {
        FileConfiguration config = Main.getInstance().getConfig();
        if (config.getBoolean("features.victory-sound"))
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        if (config.getBoolean("features.victory-particles"))
            player.spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        if (config.getBoolean("features.victory-fireworks"))
            spawnFirework(player.getLocation());
    }

    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
                .withFade(Color.WHITE).with(FireworkEffect.Type.BALL_LARGE).trail(true).flicker(true).build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
        fw.detonate();
    }

    public void openEditGUI(Player player, int page) {
        Map<Integer, List<Material>> playerDraft = editDrafts.get(player.getUniqueId());
        if (playerDraft == null) {
            playerDraft = new TreeMap<>();
            List<Material> allItems = itemManager.getItems();
            for (int i = 0; i < allItems.size(); i++)
                playerDraft.computeIfAbsent(i / 45, k -> new ArrayList<>()).add(allItems.get(i));
            editDrafts.put(player.getUniqueId(), playerDraft);
        }
        editPages.put(player.getUniqueId(), page);
        List<Material> currentPageItems = playerDraft.getOrDefault(page, new ArrayList<>());
        String titleStr = Main.getInstance().getMessageManager().getLegacyString("gui.edit-title", "{page}",
                String.valueOf(page + 1));
        Inventory gui = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacySection().deserialize(titleStr));
        for (int i = 0; i < 45; i++)
            if (i < currentPageItems.size())
                gui.setItem(i, new ItemStack(currentPageItems.get(i)));
        for (int i = 45; i < 54; i++)
            gui.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Main.getInstance().getMessageManager().getComponent("gui.prev-page"));
            prev.setItemMeta(prevMeta);
            gui.setItem(45, prev);
        }
        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.displayName(Main.getInstance().getMessageManager().getComponent("gui.next-page"));
        next.setItemMeta(nextMeta);
        gui.setItem(53, next);
        player.openInventory(gui);
    }

    public void saveDraft(Player player, Inventory inv) {
        Map<Integer, List<Material>> playerDraft = editDrafts.get(player.getUniqueId());
        if (playerDraft == null)
            return;
        int page = editPages.getOrDefault(player.getUniqueId(), 0);
        List<Material> pageItems = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR)
                pageItems.add(item.getType());
        }
        if (pageItems.isEmpty() && page > 0)
            playerDraft.remove(page);
        else
            playerDraft.put(page, pageItems);
    }

    public void finalizeEdit(Player player) {
        Map<Integer, List<Material>> playerDraft = editDrafts.remove(player.getUniqueId());
        if (playerDraft != null) {
            Set<Material> uniqueItems = new LinkedHashSet<>();
            for (List<Material> pageList : playerDraft.values())
                uniqueItems.addAll(pageList);
            itemManager.saveItems(new ArrayList<>(uniqueItems));
            player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.edit-saved"));
        }
        editPages.remove(player.getUniqueId());
    }

    public int getEditPage(Player player) {
        return editPages.getOrDefault(player.getUniqueId(), 0);
    }

    public PlayerSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public ItemManager getItemManager() {
        return itemManager;
    }
}