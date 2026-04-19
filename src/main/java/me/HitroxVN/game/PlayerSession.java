package me.HitroxVN.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerSession {

    private final UUID playerUUID;
    private final Material targetItem;
    private long startTime;
    private final long personalBest;
    private boolean finished;

    private Location gameSpawnLocation;
    private Location startLocation;
    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;
    private float savedExp;
    private int savedLevel;
    private double savedHealth;
    private int savedFood;

    public PlayerSession(UUID playerUUID, Material targetItem, long personalBest) {
        this.playerUUID = playerUUID;
        this.targetItem = targetItem;
        this.startTime = System.currentTimeMillis();
        this.personalBest = personalBest;
        this.finished = false;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Material getTargetItem() {
        return targetItem;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getPersonalBest() {
        return personalBest;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Location getGameSpawnLocation() {
        return gameSpawnLocation;
    }

    public void setGameSpawnLocation(Location gameSpawnLocation) {
        this.gameSpawnLocation = gameSpawnLocation;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public ItemStack[] getSavedInventory() {
        return savedInventory;
    }

    public void setSavedInventory(ItemStack[] savedInventory) {
        this.savedInventory = savedInventory;
    }

    public ItemStack[] getSavedArmor() {
        return savedArmor;
    }

    public void setSavedArmor(ItemStack[] savedArmor) {
        this.savedArmor = savedArmor;
    }

    public float getSavedExp() {
        return savedExp;
    }

    public void setSavedExp(float savedExp) {
        this.savedExp = savedExp;
    }

    public int getSavedLevel() {
        return savedLevel;
    }

    public void setSavedLevel(int savedLevel) {
        this.savedLevel = savedLevel;
    }

    public double getSavedHealth() {
        return savedHealth;
    }

    public void setSavedHealth(double savedHealth) {
        this.savedHealth = savedHealth;
    }

    public int getSavedFood() {
        return savedFood;
    }

    public void setSavedFood(int savedFood) {
        this.savedFood = savedFood;
    }
}