package com.example.mobtracker.screen;

import com.example.mobtracker.MobTrackerMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private TextFieldWidget addField;
    private List<String> nametagList;
    private int scroll = 0;
    
    public ConfigScreen() {
        super(Text.literal("MobTracker Config"));
        this.nametagList = new ArrayList<>(MobTrackerMod.getTrackedNametags());
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 40;
        
        // Title
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("§l§6MobTracker Configuration"),
            btn -> {}).position(centerX - 100, 10).size(200, 20).build());
        
        // Status
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Status: " + (MobTrackerMod.isActive() ? "§aON" : "§cOFF")),
            btn -> MobTrackerMod.toggleActive())
            .position(centerX - 100, y).size(200, 20).build());
        y += 25;
        
        // Add field
        addField = new TextFieldWidget(textRenderer, centerX - 100, y, 160, 20, Text.literal(""));
        this.addSelectableChild(addField);
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> {
            if (!addField.getText().isEmpty()) {
                MobTrackerMod.addNametag(addField.getText());
                addField.setText("");
                nametagList = new ArrayList<>(MobTrackerMod.getTrackedNametags());
            }
        }).position(centerX + 65, y).size(35, 20).build());
        y += 25;
        
        // Range
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Range: " + MobTrackerMod.CONFIG.trackingRange + " blocks"),
            btn -> client.setScreen(new RangeScreen()))
            .position(centerX - 100, y).size(200, 20).build());
        y += 30;
        
        // List header
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("§lNametags (" + nametagList.size() + ")"),
            btn -> {}).position(centerX - 100, y).size(200, 20).build());
        y += 25;
        
        // List nametags
        int visible = Math.min(nametagList.size() - scroll, 8);
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx < nametagList.size()) {
                String tag = nametagList.get(idx);
                this.addDrawableChild(ButtonWidget.builder(Text.literal("§e" + tag), btn -> {
                    MobTrackerMod.removeNametag(tag);
                    nametagList = new ArrayList<>(MobTrackerMod.getTrackedNametags());
                }).position(centerX - 100, y + (i * 25)).size(180, 20).build());
            }
        }
        
        // Bottom buttons
        y = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), btn -> {
            for (String tag : new ArrayList<>(nametagList)) {
                MobTrackerMod.removeNametag(tag);
            }
            nametagList.clear();
        }).position(centerX - 150, y).size(100, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
            .position(centerX - 50, y).size(100, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Help"), btn -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(
                    "§6MobTracker:\n§7H=toggle, J=config, K=add nametag"
                ), false);
            }
        }).position(centerX + 50, y).size(100, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void close() {
        client.setScreen(null);
    }
}

class RangeScreen extends Screen {
    private TextFieldWidget field;
    
    public RangeScreen() {
        super(Text.literal("Set Range"));
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        field = new TextFieldWidget(textRenderer, centerX - 50, centerY - 30, 100, 20,
            Text.literal(String.valueOf(MobTrackerMod.CONFIG.trackingRange)));
        field.setMaxLength(3);
        this.addSelectableChild(field);
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), btn -> {
            try {
                int range = Integer.parseInt(field.getText());
                if (range >= 10 && range <= 500) {
                    MobTrackerMod.setRange(range);
                    close();
                }
            } catch (Exception e) {}
        }).position(centerX - 50, centerY).size(100, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close())
            .position(centerX - 50, centerY + 25).size(100, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, "Enter range (10-500):", 
            width / 2 - 60, height / 2 - 50, 0xFFFFFF, true);
    }
    
    @Override
    public void close() {
        client.setScreen(new ConfigScreen());
    }
}
