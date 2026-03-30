package com.walrusone.skywarsreloaded.config;

import com.walrusone.skywarsreloaded.enums.Vote;

public class CustomChestType {

    private final String id;
    private final String displayName;
    private final String material;
    private final int slot;
    private final String chestFile;
    private final String centerChestFile;
    private final Vote vote;

    public CustomChestType(String id, String displayName, String material, int slot,
                           String chestFile, String centerChestFile, Vote vote) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
        this.chestFile = chestFile;
        this.centerChestFile = centerChestFile;
        this.vote = vote;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

    public String getChestFile() {
        return chestFile;
    }

    public String getCenterChestFile() {
        return centerChestFile;
    }

    public Vote getVote() {
        return vote;
    }
}
