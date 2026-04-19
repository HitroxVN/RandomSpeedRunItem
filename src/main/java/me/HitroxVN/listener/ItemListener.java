package me.HitroxVN.listener;

import me.HitroxVN.Main;
import me.HitroxVN.game.GameManager;
import me.HitroxVN.game.PlayerSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.WorldCreator;

public class ItemListener implements Listener {

    private final GameManager gameManager;

    public ItemListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player))
            return;
        checkItem(player, e.getItem().getItemStack());
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;
        checkItem(player, e.getRecipe().getResult());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;

        String title = e.getView().getTitle();
        String rouletteTitle = Main.getInstance().getMessageManager().getLegacyString("gui.title");
        String editTitleRaw = Main.getInstance().getMessageManager().getLegacyString("gui.edit-title");
        String editTitlePrefix = editTitleRaw.contains("-") ? editTitleRaw.split("-")[0].trim() : editTitleRaw.trim();

        if (title.equals(rouletteTitle) || title.startsWith("§0Top 10:")) {
            e.setCancelled(true);
            return;
        }

        if (title.startsWith(editTitlePrefix)) {
            if (e.getRawSlot() >= 45 && e.getRawSlot() < 54) {
                e.setCancelled(true);
                int currentPage = gameManager.getEditPage(player);
                if (e.getRawSlot() == 45 && e.getCurrentItem() != null
                        && e.getCurrentItem().getType() == Material.ARROW) {
                    gameManager.saveDraft(player, e.getInventory());
                    gameManager.openEditGUI(player, currentPage - 1);
                } else if (e.getRawSlot() == 53 && e.getCurrentItem() != null
                        && e.getCurrentItem().getType() == Material.ARROW) {
                    gameManager.saveDraft(player, e.getInventory());
                    gameManager.openEditGUI(player, currentPage + 1);
                }
                return;
            }
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null)
            return;

        checkItem(player, clicked);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player))
            return;

        String title = e.getView().getTitle();
        String editTitleRaw = Main.getInstance().getMessageManager().getLegacyString("gui.edit-title");
        String editTitlePrefix = editTitleRaw.contains("-") ? editTitleRaw.split("-")[0].trim() : editTitleRaw.trim();

        if (title.startsWith(editTitlePrefix)) {
            gameManager.saveDraft(player, e.getInventory());
            Main.getInstance().getServer().getScheduler().runTaskLater(Main.getInstance(), () -> {
                if (player.getOpenInventory().getTopInventory()
                        .getType() != org.bukkit.event.inventory.InventoryType.CHEST ||
                        !player.getOpenInventory().getTitle().startsWith(editTitlePrefix)) {
                    gameManager.finalizeEdit(player);
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World currentWorld = player.getWorld();
        if (!currentWorld.getName().startsWith("sr_player_"))
            return;

        String baseName = currentWorld.getName();
        String rootName = baseName.replace("_nether", "").replace("_the_end", "");

        World.Environment targetEnv;
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            targetEnv = (currentWorld.getEnvironment() == World.Environment.NETHER) ? World.Environment.NORMAL
                    : World.Environment.NETHER;
        } else {
            targetEnv = (currentWorld.getEnvironment() == World.Environment.THE_END) ? World.Environment.NORMAL
                    : World.Environment.THE_END;
        }

        String targetName = rootName + (targetEnv == World.Environment.NETHER ? "_nether"
                : (targetEnv == World.Environment.THE_END ? "_the_end" : ""));

        World targetWorld = Bukkit.getWorld(targetName);
        if (targetWorld == null) {
            WorldCreator wc = new WorldCreator(targetName).environment(targetEnv);
            targetWorld = wc.createWorld();
            if (targetWorld != null) {
                targetWorld.setAutoSave(false);
                targetWorld.setKeepSpawnInMemory(false);
            }
        }

        if (targetWorld != null && event.getTo() != null) {
            event.getTo().setWorld(targetWorld);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerSession session = gameManager.getSession(player);
        if (session != null && !session.isFinished()) {
            if (event.isBedSpawn() || event.isAnchorSpawn()) {
                return;
            }

            if (session.getGameSpawnLocation() != null) {
                event.setRespawnLocation(session.getGameSpawnLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.quitEarly(event.getPlayer());
    }

    private void checkItem(Player player, ItemStack item) {
        PlayerSession session = gameManager.getSession(player);
        if (session == null || session.isFinished())
            return;

        if (item.getType() == session.getTargetItem()) {
            gameManager.finish(player);
        }
    }
}