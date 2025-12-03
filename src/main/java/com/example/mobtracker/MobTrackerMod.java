package com.example.mobtracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobTrackerMod implements ModInitializer {
    public static final String MOD_ID = "mobtracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Configuration
    public static MobTrackerConfig CONFIG = new MobTrackerConfig();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(),
        "mobtracker_config.json"
    );
    
    // Runtime tracking
    private static final Map<UUID, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();
    private static boolean isActive = true;
    
    // Key bindings
    private static KeyBinding toggleKey;
    private static KeyBinding configKey;
    private static KeyBinding addEntityKey;
    
    @Override
    public void onInitialize() {
        LOGGER.info("MobTracker Mod Initializing...");
        loadConfig();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MobTrackerCommands.register(dispatcher);
        });
        
        LOGGER.info("MobTracker Mod Initialized!");
    }
    
    public static void initializeClient() {
        LOGGER.info("Initializing MobTracker client...");
        
        // Register key bindings
        registerKeyBindings();
        
        // Register events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            onClientTick(client);
        });
        
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            onHudRender(drawContext, tickDelta);
        });
    }
    
    private static void registerKeyBindings() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mobtracker.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.mobtracker.main"
        ));
        
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mobtracker.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "category.mobtracker.main"
        ));
        
        addEntityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mobtracker.addentity",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.mobtracker.main"
        ));
    }
    
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        
        // Handle key presses
        if (toggleKey.wasPressed()) toggleActive();
        if (configKey.wasPressed() && client.player != null) {
            client.setScreen(new ConfigScreen());
        }
        if (addEntityKey.wasPressed() && client.player != null) {
            addTargetedEntity(client);
        }
        
        if (!isActive) return;
        
        // Update tracking
        trackedEntities.entrySet().removeIf(entry -> {
            Entity entity = client.world.getEntityById(entry.getValue().entityId);
            return entity == null || !entity.isAlive() || 
                   client.player.squaredDistanceTo(entity) > CONFIG.trackingRange * CONFIG.trackingRange;
        });
        
        // Find new entities
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity) continue;
            
            Text customName = entity.getCustomName();
            if (customName != null && !customName.getString().isEmpty()) {
                String nameString = customName.getString();
                
                boolean shouldTrack = CONFIG.trackedNametags.stream()
                    .anyMatch(tag -> nameString.toUpperCase().contains(tag.toUpperCase()));
                
                if (shouldTrack && !trackedEntities.containsKey(entity.getUuid())) {
                    trackedEntities.put(entity.getUuid(), new TrackedEntity(entity));
                    sendMessage(client.player, 
                        "§a✓ Tracking: §e" + nameString + 
                        " §7(" + entity.getType().getName().getString() + ")");
                }
            }
        }
        
        // Update existing
        for (TrackedEntity tracked : trackedEntities.values()) {
            tracked.update(client.world, client.player);
        }
    }
    
    private static void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !CONFIG.showHud) return;
        if (!isActive || trackedEntities.isEmpty()) return;
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = screenWidth - 180;
        int y = 10;
        
        // Background
        drawContext.fill(x - 5, y - 5, x + 175, y + 20 + (trackedEntities.size() * 12), 0x80000000);
        
        // Header
        drawContext.drawText(textRenderer, 
            Text.literal("§6§lMobTracker §7(" + trackedEntities.size() + ")"), 
            x, y, 0xFFFFFF, true);
        
        y += 15;
        
        // List entities
        List<TrackedEntity> sorted = new ArrayList<>(trackedEntities.values());
        sorted.sort(Comparator.comparingDouble(e -> e.distance));
        
        for (TrackedEntity tracked : sorted) {
            if (y > screenHeight - 30) break;
            
            String display = String.format("§f%s §7- §e%.1fm", 
                tracked.name.length() > 15 ? tracked.name.substring(0, 15) + "..." : tracked.name,
                tracked.distance);
            
            drawContext.drawText(textRenderer, 
                Text.literal(display), 
                x, y, 0xFFFFFF, true);
            y += 12;
        }
        
        // Status indicator
        drawContext.fill(screenWidth - 20, 5, screenWidth - 5, 20, isActive ? 0xFF00FF00 : 0xFFFF0000);
        drawContext.drawText(textRenderer, 
            Text.literal(isActive ? "ON" : "OFF"), 
            screenWidth - 17, 8, 0x000000, false);
    }
    
    // Public API
    public static void toggleActive() {
        isActive = !isActive;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (isActive) {
                sendMessage(client.player, "§a§l✓ MOBTRACKER ACTIVATED");
                sendMessage(client.player, "§7Tracking: " + String.join(", ", CONFIG.trackedNametags));
            } else {
                sendMessage(client.player, "§c§l✗ MOBTRACKER DEACTIVATED");
            }
        }
    }
    
    public static void addNametag(String nametag) {
        String upperName = nametag.toUpperCase();
        if (!CONFIG.trackedNametags.contains(upperName)) {
            CONFIG.trackedNametags.add(upperName);
            saveConfig();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                sendMessage(client.player, "§a✓ Added: §e" + nametag);
            }
        }
    }
    
    public static void removeNametag(String nametag) {
        String upperName = nametag.toUpperCase();
        if (CONFIG.trackedNametags.remove(upperName)) {
            saveConfig();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                sendMessage(client.player, "§c✗ Removed: §e" + nametag);
            }
        }
    }
    
    public static void setRange(int range) {
        CONFIG.trackingRange = Math.max(10, Math.min(500, range));
        saveConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            sendMessage(client.player, "§a✓ Range: §e" + range + " blocks");
        }
    }
    
    public static void clearTracked() {
        trackedEntities.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            sendMessage(client.player, "§c✗ Cleared all tracked");
        }
    }
    
    public static void showStatus(PlayerEntity player) {
        sendMessage(player, 
            "§6=== MobTracker ===\n" +
            "§eActive: §f" + (isActive ? "§aYES" : "§cNO") + "\n" +
            "§eRange: §f" + CONFIG.trackingRange + " blocks\n" +
            "§eTracking: §f" + trackedEntities.size() + " entities\n" +
            "§eNametags: §f" + CONFIG.trackedNametags.size() + "\n" +
            "§7Press §eH §7toggle, §eJ §7config, §eK §7add");
    }
    
    public static void listNametags(PlayerEntity player) {
        if (CONFIG.trackedNametags.isEmpty()) {
            sendMessage(player, "§7No nametags. Use §e/tracker add <name>");
            return;
        }
        sendMessage(player, "§6Tracked Nametags:");
        List<String> sorted = new ArrayList<>(CONFIG.trackedNametags);
        Collections.sort(sorted);
        for (String tag : sorted) {
            sendMessage(player, "  §e- " + tag);
        }
    }
    
    private static void addTargetedEntity(MinecraftClient client) {
        Entity target = client.crosshairTarget != null ? client.crosshairTarget.getEntity() : null;
        if (target != null && !(target instanceof PlayerEntity)) {
            Text customName = target.getCustomName();
            if (customName != null && !customName.getString().isEmpty()) {
                addNametag(customName.getString());
            } else {
                sendMessage(client.player, "§cEntity has no nametag!");
            }
        } else {
            sendMessage(client.player, "§cNo entity targeted!");
        }
    }
    
    private static void sendMessage(PlayerEntity player, String message) {
        if (player != null) {
            for (String line : message.split("\n")) {
                player.sendMessage(Text.literal(line), false);
            }
        }
    }
    
    private static void loadConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                CONFIG = gson.fromJson(reader, MobTrackerConfig.class);
            } catch (IOException e) {
                CONFIG = new MobTrackerConfig();
            }
        } else {
            CONFIG = new MobTrackerConfig();
            saveConfig();
        }
    }
    
    public static void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(CONFIG, writer);
        } catch (IOException e) {}
    }
    
    public static boolean isActive() { return isActive; }
    public static Set<String> getTrackedNametags() { return CONFIG.trackedNametags; }
    
    private static class TrackedEntity {
        public int entityId;
        public String name;
        public double distance;
        public Vec3d lastPos;
        
        public TrackedEntity(Entity entity) {
            this.entityId = entity.getId();
            this.name = entity.getCustomName() != null ? 
                entity.getCustomName().getString() : "Unnamed";
            this.lastPos = entity.getPos();
        }
        
        public void update(net.minecraft.world.World world, PlayerEntity player) {
            Entity entity = world.getEntityById(entityId);
            if (entity != null && entity.isAlive()) {
                this.lastPos = entity.getPos();
                if (player != null) {
                    this.distance = player.getPos().distanceTo(this.lastPos);
                }
            }
        }
    }
}
