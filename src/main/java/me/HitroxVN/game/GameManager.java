package me.HitroxVN.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import me.HitroxVN.util.TimeUtil;
import me.HitroxVN.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();
    private final ItemManager itemManager = new ItemManager();

    public GameManager() {
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::updateDisplays, 20L, 20L);
    }

    private void updateDisplays() {
        String displayType = Main.getInstance().getConfig().getString("settings.display-type", "BOTH");
        
        for (Map.Entry<UUID, PlayerSession> entry : sessions.entrySet()) {
            PlayerSession session = entry.getValue();
            if (session.isFinished()) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            long elapsed = System.currentTimeMillis() - session.getStartTime();
            String timeStr = TimeUtil.format(elapsed);
            String itemName = session.getTargetItem().name().replace("_", " ");

            // ActionBar
            if (displayType.equalsIgnoreCase("ACTIONBAR") || displayType.equalsIgnoreCase("BOTH")) {
                player.sendActionBar(Main.getInstance().getMessageManager().getComponent("actionbar.format", 
                        "{item}", itemName, 
                        "{time}", timeStr));
            }

            // Scoreboard
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
            obj = board.registerNewObjective("rsrun", Criteria.DUMMY, LegacyComponentSerializer.legacyAmpersand().deserialize(title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Clear old scores (very simple way)
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        String pbStr = session.getPersonalBest() == -1 
                ? Main.getInstance().getMessageManager().getLegacyString("scoreboard.no-best") 
                : TimeUtil.format(session.getPersonalBest());

        obj.getScore("§1").setScore(6);
        obj.getScore(Main.getInstance().getMessageManager().getLegacyString("scoreboard.target")).setScore(5);
        obj.getScore("§e" + itemName).setScore(4);
        obj.getScore("§2").setScore(3);
        obj.getScore(Main.getInstance().getMessageManager().getLegacyString("scoreboard.time", "{time}", timeStr)).setScore(2);
        obj.getScore(Main.getInstance().getMessageManager().getLegacyString("scoreboard.best", "{time}", pbStr)).setScore(1);
    }

    public void start(Player player) {
        Material item = itemManager.getRandomItem();
        long pb = Main.getInstance().getRecordManager().getBestTime(player.getUniqueId(), item);

        PlayerSession session = new PlayerSession(player.getUniqueId(), item, pb);
        sessions.put(player.getUniqueId(), session);

        player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.target-item", "{item}", item.name()));
        player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.game-started"));
    }

    public void finish(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished())
            return;

        session.setFinished(true);

        long time = System.currentTimeMillis() - session.getStartTime();
        String timeStr = TimeUtil.format(time);

        // Lưu kỷ lục
        Main.getInstance().getRecordManager().setBestTime(player.getUniqueId(), session.getTargetItem(), time);

        player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.game-finished"));
        player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.elapsed-time", "{time}", timeStr));
        player.sendActionBar(Main.getInstance().getMessageManager().getComponent("messages.finish-actionbar", "{time}", timeStr));
        
        // Reset Scoreboard sau 5 giây
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (player.isOnline()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }, 100L);
    }

    public PlayerSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public ItemManager getItemManager() {
        return itemManager;
    }
}