package me.HitroxVN.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import me.HitroxVN.Main;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RecordManager {

    private FileConfiguration config;
    private File file;

    public RecordManager() {
        reload();
    }

    public void reload() {
        file = new File(Main.getInstance().getDataFolder(), "records.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public long getBestTime(UUID uuid, Material item) {
        return config.getLong(uuid.toString() + "." + item.name(), -1);
    }

    public void setBestTime(UUID uuid, Material item, long time) {
        long currentBest = getBestTime(uuid, item);
        if (currentBest == -1 || time < currentBest) {
            config.set(uuid.toString() + "." + item.name(), time);
            save();
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
