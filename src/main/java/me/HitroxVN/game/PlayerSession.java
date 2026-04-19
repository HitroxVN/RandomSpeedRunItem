package me.HitroxVN.game;

import org.bukkit.Material;

import java.util.UUID;

public class PlayerSession {

    private UUID uuid;
    private Material targetItem;
    private long startTime;
    private boolean finished;
    private long personalBest;

    public PlayerSession(UUID uuid, Material targetItem, long personalBest) {
        this.uuid = uuid;
        this.targetItem = targetItem;
        this.startTime = System.currentTimeMillis();
        this.finished = false;
        this.personalBest = personalBest;
    }

    public long getPersonalBest() {
        return personalBest;
    }

    public Material getTargetItem() {
        return targetItem;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}