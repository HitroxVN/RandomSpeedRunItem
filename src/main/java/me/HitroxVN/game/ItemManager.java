package me.HitroxVN.game;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.HitroxVN.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemManager {

    private final List<Material> items = new ArrayList<>();
    private final Random random = new Random();

    public ItemManager() {
        reload();
    }

    public void reload() {
        items.clear();
        File file = new File(Main.getInstance().getDataFolder(), "items.yml");
        if (!file.exists()) {
            Main.getInstance().saveResource("items.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> itemList = config.getStringList("items");
        for (String s : itemList) {
            try {
                items.add(Material.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public Material getRandomItem() {
        if (items.isEmpty()) return Material.DIRT;
        return items.get(random.nextInt(items.size()));
    }
}