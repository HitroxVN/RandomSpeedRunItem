package me.HitroxVN;

import org.bukkit.plugin.java.JavaPlugin;
import me.HitroxVN.command.StartCommand;
import me.HitroxVN.game.GameManager;
import me.HitroxVN.listener.ItemListener;

import me.HitroxVN.util.MessageManager;
import me.HitroxVN.util.RecordManager;

public class Main extends JavaPlugin {

    private static Main instance;
    private GameManager gameManager;
    private MessageManager messageManager;
    private RecordManager recordManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        messageManager = new MessageManager();
        recordManager = new RecordManager();
        gameManager = new GameManager();

        StartCommand speedrunCommand = new StartCommand(gameManager);
        getCommand("randomspeedrun").setExecutor(speedrunCommand);
        getCommand("randomspeedrun").setTabCompleter(speedrunCommand);

        getServer().getPluginManager().registerEvents(new ItemListener(gameManager), this);
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public RecordManager getRecordManager() {
        return recordManager;
    }

    public static Main getInstance() {
        return instance;
    }
}