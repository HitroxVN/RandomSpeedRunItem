package me.HitroxVN;

import org.bukkit.plugin.java.JavaPlugin;
import me.HitroxVN.command.StartCommand;
import me.HitroxVN.game.GameManager;
import me.HitroxVN.listener.ItemListener;

public class Main extends JavaPlugin {

    private static Main instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;

        gameManager = new GameManager();

        getCommand("rspeedrunitem").setExecutor(new StartCommand(gameManager));

        getServer().getPluginManager().registerEvents(new ItemListener(gameManager), this);

        saveDefaultConfig();
    }

    public static Main getInstance() {
        return instance;
    }
}