package me.HitroxVN.command;

import org.bukkit.Material;
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
            sender.sendMessage(
                    Main.getInstance().getMessageManager().getComponent("messages.command-usage", "{label}", label));
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
                    player.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.no-permission"));
                }
                break;
            case "reload":
                reload(player);
                break;
            case "top":
                if (args.length < 2) {
                    sender.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.command-usage",
                            "{label}", label));
                    return true;
                }
                Material target = Material.matchMaterial(args[1]);
                if (target == null) {
                    sender.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.invalid-item"));
                    return true;
                }
                gameManager.openLeaderboardGUI(player, target);
                break;
            default:
                sender.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.command-usage",
                        "{label}", label));
                break;
        }

        return true;
    }

    private void reload(CommandSender sender) {
        Main.getInstance().reloadConfig();
        Main.getInstance().getMessageManager().reload();
        Main.getInstance().reloadStorage();
        gameManager.getItemManager().reload();
        sender.sendMessage(Main.getInstance().getMessageManager().getComponent("messages.reload-success"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("start");
            subcommands.add("reload");
            subcommands.add("edit");
            subcommands.add("top");
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return gameManager.getItemManager().getItems().stream()
                    .map(m -> m.name().toLowerCase())
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}