package com.example.mobtracker;

import java.util.HashSet;
import java.util.Set;

public class MobTrackerConfig {
    public Set<String> trackedNametags = new HashSet<>();
    public int trackingRange = 100;
    public boolean showHud = true;
    
    public MobTrackerConfig() {
        trackedNametags.add("BOSS");
        trackedNametags.add("MINIBOSS");
        trackedNametags.add("TRACK_ME");
        trackedNametags.add("SPECIAL");
        trackedNametags.add("GUARD");
        trackedNametags.add("TARGET");
    }
}
