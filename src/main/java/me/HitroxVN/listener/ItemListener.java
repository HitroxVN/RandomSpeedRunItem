package me.HitroxVN.listener;

import me.HitroxVN.game.GameManager;
import me.HitroxVN.game.PlayerSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class ItemListener implements Listener {

    private final GameManager gameManager;

    public ItemListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        Player player = (Player) e.getEntity();
        PlayerSession session = gameManager.getSession(player);

        if (session == null || session.isFinished()) return;

        if (e.getItem().getItemStack().getType() == session.getTargetItem()) {
            gameManager.finish(player);
        }
    }
}