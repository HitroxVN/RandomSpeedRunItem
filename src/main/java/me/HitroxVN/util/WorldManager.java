package me.HitroxVN.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.apache.commons.io.FileUtils;
import me.HitroxVN.Main;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WorldManager {

    private static final String FOLDER_PREFIX = "sr_player_";

    public static CompletableFuture<Boolean> prepareWorldFolder(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String templateName = Main.getInstance().getConfig().getString("worlds.template-name", "template_world");
            File templateFolder = new File(Bukkit.getWorldContainer(), templateName);
            File targetFolder = new File(Bukkit.getWorldContainer(), FOLDER_PREFIX + uuid.toString());

            if (!templateFolder.exists()) {
                Bukkit.getLogger().severe("[Speedrun] KHONG TIM THAY '" + templateName + "'!");
                return false;
            }

            try {
                if (targetFolder.exists()) {
                    FileUtils.deleteDirectory(targetFolder);
                }

                FileFilter filter = file -> {
                    String name = file.getName();
                    return !name.equals("session.lock") && !name.equals("uid.dat");
                };

                FileUtils.copyDirectory(templateFolder, targetFolder, filter);
                return true;
            } catch (IOException e) {
                Bukkit.getLogger().warning("Loi khi copy world folder: " + e.getMessage());
                return false;
            }
        });
    }

    public static World loadWorld(UUID uuid) {
        String worldName = FOLDER_PREFIX + uuid.toString();
        WorldCreator wc = new WorldCreator(worldName);
        World world = wc.createWorld();
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            world.setAutoSave(false);
        }
        return world;
    }

    public static org.bukkit.Location getRandomLocation(World world) {
        java.util.Random random = new java.util.Random();
        int range = Main.getInstance().getConfig().getInt("worlds.random-range", 5000);
        
        int x, z, y;
        org.bukkit.block.Block block;
        int attempts = 0;

        while (attempts < 50) { // Gioi han 50 lan thu de tranh treo server
            attempts++;
            x = random.nextInt(range * 2) - range;
            z = random.nextInt(range * 2) - range;
            y = world.getHighestBlockYAt(x, z);
            
            block = world.getBlockAt(x, y, z);
            Biome biome = world.getBiome(x, y, z);

            // Kiem tra neu khong phai la nuoc va không phai Biome bien
            if (block.getType() != org.bukkit.Material.WATER && !isOcean(biome)) {
                return new org.bukkit.Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        
        // Neu sau 50 lan van khong tim thay (hiếm), tra ve vi tri spawn mac dinh
        return world.getSpawnLocation();
    }

    private static boolean isOcean(Biome biome) {
        String key = biome.getKey().getKey().toUpperCase();
        return key.contains("OCEAN") || key.contains("RIVER") || key.contains("BEACH");
    }

    public static void deleteWorld(UUID uuid) {
        String worldName = FOLDER_PREFIX + uuid.toString();
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            world.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()));
            Bukkit.unloadWorld(world, false);
        }

        CompletableFuture.runAsync(() -> {
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            try {
                Thread.sleep(1500); 
                if (worldFolder.exists()) {
                    FileUtils.deleteDirectory(worldFolder);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Khong the xoa folder " + worldName + ".");
            }
        });
    }
}
