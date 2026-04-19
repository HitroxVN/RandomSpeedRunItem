package me.HitroxVN.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import me.HitroxVN.game.GameManager;
import me.HitroxVN.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StartCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;

    public StartCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reload(sender);
            }
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /" + label + " <start|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                gameManager.start(player);
                break;
            case "edit":
                if (player.hasPermission("randomspeedrun.admin")) {
                    gameManager.openEditGUI(player, 0);
                } else {
                    player.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                }
                break;
            case "reload":
                reload(player);
                break;
            default:
                player.sendMessage("§cUsage: /" + label + " <start|reload>");
                break;
        }

        return true;
    }

    private void reload(CommandSender sender) {
        Main.getInstance().reloadConfig();
        Main.getInstance().getMessageManager().reload();
        Main.getInstance().reloadStorage();
        gameManager.getItemManager().reload();
        sender.sendMessage("§a[RandomSpeedRunItem] Đã reload cấu hình và vật phẩm thành công!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("start");
            subcommands.add("reload");
            subcommands.add("edit");
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}