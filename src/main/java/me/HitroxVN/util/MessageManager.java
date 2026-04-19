package me.HitroxVN.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.HitroxVN.Main;

import java.io.File;

public class MessageManager {

    private FileConfiguration config;
    private File file;

    public MessageManager() {
        reload();
    }

    public void reload() {
        file = new File(Main.getInstance().getDataFolder(), "strings.yml");
        if (!file.exists()) {
            Main.getInstance().saveResource("strings.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String getRaw(String path) {
        return config.getString(path, "Missing string: " + path);
    }

    public Component getComponent(String path, String... replacements) {
        String message = getRaw(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    public String getLegacyString(String path, String... replacements) {
        String message = getRaw(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message.replace("&", "§");
    }
}
