package me.HitroxVN.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.HitroxVN.game.GameManager;

public class StartCommand implements CommandExecutor {

    private final GameManager gameManager;

    public StartCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player))
            return true;

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cUsage: /rspeedrunitem start");
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            gameManager.start(player);
        }

        return true;
    }
}