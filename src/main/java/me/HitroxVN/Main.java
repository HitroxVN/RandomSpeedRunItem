package me.HitroxVN;

import org.bukkit.plugin.java.JavaPlugin;
import me.HitroxVN.command.StartCommand;
import me.HitroxVN.game.GameManager;
import me.HitroxVN.listener.ItemListener;
import me.HitroxVN.util.MessageManager;
import me.HitroxVN.database.DatabaseManager;

public class Main extends JavaPlugin {

    private static Main instance;
    private GameManager gameManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadStorage();

        this.messageManager = new MessageManager();
        this.gameManager = new GameManager();

        StartCommand speedrunCommand = new StartCommand(gameManager);
        getCommand("randomspeedrun").setExecutor(speedrunCommand);
        getCommand("randomspeedrun").setTabCompleter(speedrunCommand);

        getServer().getPluginManager().registerEvents(new ItemListener(gameManager), this);
    }

    public void reloadStorage() {
        if (databaseManager != null)
            databaseManager.close();
        this.databaseManager = new DatabaseManager();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    @Override
    public void onDisable() {
        if (databaseManager != null)
            databaseManager.close();
    }

    public static Main getInstance() {
        return instance;
    }
}