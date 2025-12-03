package com.example.mobtracker;

import net.fabricmc.api.ClientModInitializer;

public class MobTrackerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MobTrackerMod.initializeClient();
    }
}
