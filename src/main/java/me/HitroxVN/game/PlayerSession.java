package me.HitroxVN.game;

import org.bukkit.Material;

import java.util.UUID;

public class PlayerSession {

    private UUID uuid;
    private Material targetItem;
    private long startTime;
    private boolean finished;

    public PlayerSession(UUID uuid, Material targetItem) {
        this.uuid = uuid;
        this.targetItem = targetItem;
        this.startTime = System.currentTimeMillis();
        this.finished = false;
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