package me.HitroxVN.game;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import me.HitroxVN.util.TimeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();
    private final ItemManager itemManager = new ItemManager();

    public void start(Player player) {
        Material item = itemManager.getRandomItem();

        PlayerSession session = new PlayerSession(player.getUniqueId(), item);
        sessions.put(player.getUniqueId(), session);

        player.sendMessage("§aItem của bạn: §e" + item.name());
        player.sendMessage("§aBắt đầu!");
    }

    public void finish(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isFinished())
            return;

        session.setFinished(true);

        long time = System.currentTimeMillis() - session.getStartTime();

        player.sendMessage("§aHoàn thành!");
        player.sendMessage("§eThời gian: " + TimeUtil.format(time));
    }

    public PlayerSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }
}