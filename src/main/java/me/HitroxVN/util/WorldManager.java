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
            String templateBase = Main.getInstance().getConfig().getString("worlds.template-name", "template_world");
            
            // Danh sach cac moi truong can copy
            String[] envs = {"", "_nether", "_the_end"};
            
            for (String env : envs) {
                File templateFolder = new File(Bukkit.getWorldContainer(), templateBase + env);
                if (!templateFolder.exists()) {
                    if (env.equals("")) return false; // Overworld bat buoc phai co
                    continue; // Nether/End co the khong co thi bỏ qua
                }

                File targetFolder = new File(Bukkit.getWorldContainer(), FOLDER_PREFIX + uuid.toString() + env);
                try {
                    if (targetFolder.exists()) FileUtils.deleteDirectory(targetFolder);

                    FileFilter filter = file -> {
                        String name = file.getName();
                        return !name.equals("session.lock") && !name.equals("uid.dat");
                    };
                    FileUtils.copyDirectory(templateFolder, targetFolder, filter);
                } catch (IOException e) {
                    return false;
                }
            }
            return true;
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

    public static CompletableFuture<org.bukkit.Location> getRandomLocation(World world) {
        CompletableFuture<org.bukkit.Location> future = new CompletableFuture<>();
        java.util.Random random = new java.util.Random();
        int range = Main.getInstance().getConfig().getInt("worlds.random-range", 5000);

        attemptRandomLocation(world, random, range, 0, future);
        return future;
    }

    private static void attemptRandomLocation(World world, java.util.Random random, int range, int attempts, CompletableFuture<org.bukkit.Location> future) {
        if (attempts >= 50) {
            future.complete(world.getSpawnLocation());
            return;
        }

        int x = random.nextInt(range * 2) - range;
        int z = random.nextInt(range * 2) - range;

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            int y = world.getHighestBlockYAt(x, z);
            org.bukkit.block.Block block = world.getBlockAt(x, y, z);
            Biome biome = world.getBiome(x, y, z);

            if (block.getType() != org.bukkit.Material.WATER && !isOcean(biome)) {
                future.complete(new org.bukkit.Location(world, x + 0.5, y + 1, z + 0.5));
            } else {
                // Thử lại trên main thread để an toàn cho đệ quy
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> 
                    attemptRandomLocation(world, random, range, attempts + 1, future)
                );
            }
        });
    }

    private static boolean isOcean(Biome biome) {
        String key = biome.getKey().getKey().toUpperCase();
        return key.contains("OCEAN") || key.contains("RIVER") || key.contains("BEACH");
    }

    public static void deleteWorld(UUID uuid) {
        String[] envs = {"", "_nether", "_the_end"};
        
        for (String env : envs) {
            String worldName = FOLDER_PREFIX + uuid.toString() + env;
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                world.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()));
                Bukkit.unloadWorld(world, false);
            }

            CompletableFuture.runAsync(() -> {
                File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                try {
                    Thread.sleep(2000); // Đợi lâu hơn một chút để unload 3 world
                    if (worldFolder.exists()) FileUtils.deleteDirectory(worldFolder);
                } catch (Exception e) {
                    // Ignore
                }
            });
        }
    }
}
