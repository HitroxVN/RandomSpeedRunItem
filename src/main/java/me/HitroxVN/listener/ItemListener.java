package me.HitroxVN.listener;

import me.HitroxVN.Main;
import me.HitroxVN.game.GameManager;
import me.HitroxVN.game.PlayerSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

        // Chặn tương tác trong Roulette GUI
        if (title.equals(rouletteTitle)) {
            e.setCancelled(true);
            return;
        }

        // Xử lý Pagination trong Edit GUI
        if (title.startsWith(editTitlePrefix)) {
            // RawSlot < 54 là trong GUI rương
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

    private void checkItem(Player player, ItemStack item) {
        PlayerSession session = gameManager.getSession(player);
        if (session == null || session.isFinished())
            return;

        if (item.getType() == session.getTargetItem()) {
            gameManager.finish(player);
        }
    }
}