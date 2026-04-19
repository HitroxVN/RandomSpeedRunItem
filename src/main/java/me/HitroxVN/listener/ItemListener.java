package me.HitroxVN.listener;

import me.HitroxVN.game.GameManager;
import me.HitroxVN.game.PlayerSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {

    private final GameManager gameManager;

    public ItemListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        checkItem(player, e.getItem().getItemStack());
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        checkItem(player, e.getRecipe().getResult());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        checkItem(player, clicked);
    }

    private void checkItem(Player player, ItemStack item) {
        PlayerSession session = gameManager.getSession(player);
        if (session == null || session.isFinished()) return;

        if (item.getType() == session.getTargetItem()) {
            gameManager.finish(player);
        }
    }
}