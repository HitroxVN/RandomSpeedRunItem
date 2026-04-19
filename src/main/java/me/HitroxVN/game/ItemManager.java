package me.HitroxVN.game;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ItemManager {

    private final List<Material> items = Arrays.asList(
            Material.DIAMOND,
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.OAK_LOG,
            Material.COOKED_BEEF,
            Material.BREAD
    );

    private final Random random = new Random();

    public Material getRandomItem() {
        return items.get(random.nextInt(items.size()));
    }
}