package org.main.massdig.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MassdigScreen extends Screen {
    private static final int MAX_RADIUS = 6;
    private static final int ROW = 24;

    private final Screen parent;
    private int layoutX;
    private int layoutTop;
    private int layoutWidth;
    private int columnWidth;
    private int columnGap;
    private int mainX;
    private int radiusX;
    private int smartX;
    private int safetyX;
    private int mainY;
    private int radiusY;
    private int smartY;
    private int safetyY;
    private int doneY;
    private boolean compactColumns;

    public MassdigScreen(Screen parent) {
        super(Component.translatable("massdig.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compactColumns = width < 760;
        columnGap = 10;
        layoutWidth = Math.min(compactColumns ? 430 : 820, width - 24);
        layoutX = (width - layoutWidth) / 2;
        layoutTop = Math.max(8, height / 2 - (compactColumns ? 206 : 128));
        columnWidth = compactColumns ? (layoutWidth - columnGap) / 2 : (layoutWidth - columnGap * 3) / 4;

        mainX = layoutX;
        radiusX = layoutX + columnWidth + columnGap;
        smartX = compactColumns ? layoutX : layoutX + (columnWidth + columnGap) * 2;
        safetyX = compactColumns ? radiusX : layoutX + (columnWidth + columnGap) * 3;
        mainY = layoutTop + 58;
        radiusY = mainY;
        smartY = compactColumns ? mainY + 176 : mainY;
        safetyY = smartY;
        doneY = Math.max(smartY, mainY) + 176;

        addMainControls();
        addRadiusControls();
        addSmartControls();
        addSafetyControls();

        int halfWidth = (layoutWidth - columnGap) / 2;
        addRenderableWidget(Button.builder(Component.translatable("massdig.screen.reset"), button -> {
                    MassdigPreset.NORMAL.apply(MassdigClient.config());
                    rebuild();
                })
                .bounds(layoutX, doneY, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.reset")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> Minecraft.getInstance().setScreen(parent))
                .bounds(layoutX + halfWidth + columnGap, doneY, halfWidth, 20)
                .build());
    }

    private void addMainControls() {
        addRenderableWidget(button(fastMiningMessage(), mainX, mainY, "massdig.tooltip.fast_mining", b -> {
            MassdigClient.config().fastMiningEnabled = !MassdigClient.config().fastMiningEnabled;
            MassdigClient.config().save();
            b.setMessage(fastMiningMessage());
        }));
        addRenderableWidget(button(radiusEnabledMessage(), mainX, mainY + ROW, "massdig.tooltip.radius_enabled", b -> {
            MassdigClient.toggle();
            b.setMessage(radiusEnabledMessage());
        }));
        addRenderableWidget(button(hudMessage(), mainX, mainY + ROW * 2, "massdig.tooltip.hud", b -> {
            MassdigClient.config().hudEnabled = !MassdigClient.config().hudEnabled;
            MassdigClient.config().save();
            b.setMessage(hudMessage());
        }));
        addRenderableWidget(button(previewMessage(), mainX, mainY + ROW * 3, "massdig.tooltip.preview", b -> {
            MassdigClient.config().showPreview = !MassdigClient.config().showPreview;
            MassdigClient.config().save();
            b.setMessage(previewMessage());
        }));
        addRenderableWidget(button(skippedPreviewMessage(), mainX, mainY + ROW * 4, "massdig.tooltip.skipped_preview", b -> {
            MassdigClient.config().showSkippedPreview = !MassdigClient.config().showSkippedPreview;
            MassdigClient.config().save();
            b.setMessage(skippedPreviewMessage());
        }));
        addRenderableWidget(button(pauseScreenMessage(), mainX, mainY + ROW * 5, "massdig.tooltip.pause_screen", b -> {
            MassdigClient.config().pauseWhenScreenOpen = !MassdigClient.config().pauseWhenScreenOpen;
            MassdigClient.config().save();
            b.setMessage(pauseScreenMessage());
        }));
    }

    private void addRadiusControls() {
        addRenderableWidget(button(presetMessage(), radiusX, radiusY, "massdig.tooltip.preset", b -> {
            MassdigClient.config().preset().next().apply(MassdigClient.config());
            rebuild();
        }));
        addRenderableWidget(new RadiusSlider(radiusX, radiusY + ROW, columnWidth, 20));
        addRenderableWidget(button(modeMessage(), radiusX, radiusY + ROW * 2, "massdig.tooltip.mode", b -> {
            MassdigClient.config().setMode(MassdigClient.config().mode().next());
            MassdigClient.config().save();
            b.setMessage(modeMessage());
            b.setTooltip(Tooltip.create(modeHint()));
        }));
        addRenderableWidget(button(shapeMessage(), radiusX, radiusY + ROW * 3, "massdig.tooltip.shape", b -> {
            MassdigClient.config().setShape(MassdigClient.config().shape().next());
            MassdigClient.config().save();
            b.setMessage(shapeMessage());
            b.setTooltip(Tooltip.create(shapeHint()));
        }));
        addRenderableWidget(button(matchModeMessage(), radiusX, radiusY + ROW * 4, "massdig.tooltip.match", b -> {
            MassdigClient.config().setMatchMode(MassdigClient.config().matchMode().next());
            MassdigClient.config().save();
            b.setMessage(matchModeMessage());
            b.setTooltip(Tooltip.create(matchModeHint()));
        }));
    }

    private void addSmartControls() {
        addRenderableWidget(button(autoTuneMessage(), smartX, smartY, "massdig.tooltip.auto_tune", b -> {
            MassdigClient.config().smartAutoTune = !MassdigClient.config().smartAutoTune;
            MassdigClient.config().save();
            b.setMessage(autoTuneMessage());
        }));
        addRenderableWidget(button(protectionMessage(), smartX, smartY + ROW, "massdig.tooltip.protection", b -> {
            MassdigClient.config().setProtection(MassdigClient.config().protection().next());
            MassdigClient.config().save();
            b.setMessage(protectionMessage());
            b.setTooltip(Tooltip.create(protectionHint()));
        }));
        addRenderableWidget(button(autoSlowdownMessage(), smartX, smartY + ROW * 2, "massdig.tooltip.auto_slowdown", b -> {
            MassdigClient.config().autoSlowdown = !MassdigClient.config().autoSlowdown;
            MassdigClient.config().save();
            b.setMessage(autoSlowdownMessage());
        }));
        addRenderableWidget(button(hardFirstMessage(), smartX, smartY + ROW * 3, "massdig.tooltip.hard_first", b -> {
            MassdigClient.config().hardBlocksFirst = !MassdigClient.config().hardBlocksFirst;
            MassdigClient.config().save();
            b.setMessage(hardFirstMessage());
        }));
        addRenderableWidget(button(keepWithinReachMessage(), smartX, smartY + ROW * 4, "massdig.tooltip.keep_within_reach", b -> {
            MassdigClient.config().keepWithinReach = !MassdigClient.config().keepWithinReach;
            MassdigClient.config().save();
            b.setMessage(keepWithinReachMessage());
        }));
        addRenderableWidget(new PacketLimitSlider(smartX, smartY + ROW * 5, columnWidth, 20));
    }

    private void addSafetyControls() {
        addRenderableWidget(button(protectUsefulMessage(), safetyX, safetyY, "massdig.tooltip.protect_useful", b -> {
            MassdigClient.config().protectUsefulBlocks = !MassdigClient.config().protectUsefulBlocks;
            MassdigClient.config().save();
            b.setMessage(protectUsefulMessage());
        }));
        addRenderableWidget(button(protectFragileMessage(), safetyX, safetyY + ROW, "massdig.tooltip.protect_fragile", b -> {
            MassdigClient.config().protectFragileBlocks = !MassdigClient.config().protectFragileBlocks;
            MassdigClient.config().save();
            b.setMessage(protectFragileMessage());
        }));
        addRenderableWidget(button(playerSpaceMessage(), safetyX, safetyY + ROW * 2, "massdig.tooltip.player_space", b -> {
            MassdigClient.config().protectPlayerSpace = !MassdigClient.config().protectPlayerSpace;
            MassdigClient.config().save();
            b.setMessage(playerSpaceMessage());
        }));
        addRenderableWidget(button(avoidLavaMessage(), safetyX, safetyY + ROW * 3, "massdig.tooltip.avoid_lava", b -> {
            MassdigClient.config().avoidLava = !MassdigClient.config().avoidLava;
            MassdigClient.config().save();
            b.setMessage(avoidLavaMessage());
        }));
        addRenderableWidget(button(lowHealthMessage(), safetyX, safetyY + ROW * 4, "massdig.tooltip.low_health", b -> {
            MassdigClient.config().pauseOnLowHealth = !MassdigClient.config().pauseOnLowHealth;
            MassdigClient.config().save();
            b.setMessage(lowHealthMessage());
        }));
        addRenderableWidget(new SafetySlider(safetyX, safetyY + ROW * 5, columnWidth, 20));
    }

    private Button button(Component message, int x, int y, String tooltip, Button.OnPress onPress) {
        return Button.builder(message, onPress)
                .bounds(x, y, columnWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(tooltip)))
                .build();
    }

    private void rebuild() {
        Minecraft.getInstance().setScreen(new MassdigScreen(parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, layoutTop, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("massdig.screen.subtitle"), width / 2, layoutTop + 14, 0xA0A0A0);

        drawSection(graphics, mainX, mainY - 22, Component.translatable("massdig.section.main"), Component.translatable("massdig.section.main_hint"));
        drawSection(graphics, radiusX, radiusY - 22, Component.translatable("massdig.section.radius"), Component.translatable("massdig.section.radius_hint"));
        drawSection(graphics, smartX, smartY - 22, Component.translatable("massdig.section.smart"), Component.translatable("massdig.section.smart_hint"));
        drawSection(graphics, safetyX, safetyY - 22, Component.translatable("massdig.section.safety"), Component.translatable("massdig.section.safety_hint"));
    }

    private void drawSection(GuiGraphicsExtractor graphics, int x, int y, Component title, Component hint) {
        graphics.text(font, title, x, y, 0xFFD36E);
        graphics.text(font, hint, x, y + 10, 0x8F8F8F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component fastMiningMessage() {
        return labelValue("massdig.screen.fast_mining", onOff(MassdigClient.isFastMiningActive()));
    }

    private static Component radiusEnabledMessage() {
        return labelValue("massdig.screen.radius_enabled", onOff(MassdigClient.isActive()));
    }

    private static Component hudMessage() {
        return labelValue("massdig.screen.hud", onOff(MassdigClient.config().hudEnabled));
    }

    private static Component previewMessage() {
        return labelValue("massdig.screen.preview", onOff(MassdigClient.config().showPreview));
    }

    private static Component skippedPreviewMessage() {
        return labelValue("massdig.screen.skipped_preview", onOff(MassdigClient.config().showSkippedPreview));
    }

    private static Component pauseScreenMessage() {
        return labelValue("massdig.screen.pause_screen", onOff(MassdigClient.config().pauseWhenScreenOpen));
    }

    private static Component presetMessage() {
        return labelValue("massdig.screen.preset", Component.translatable(MassdigClient.config().preset().nameKey()));
    }

    private static Component modeMessage() {
        return labelValue("massdig.screen.mode", Component.translatable(MassdigClient.config().mode().nameKey()));
    }

    private static Component modeHint() {
        return Component.translatable(MassdigClient.config().mode().hintKey());
    }

    private static Component shapeMessage() {
        return labelValue("massdig.screen.shape", Component.translatable(MassdigClient.config().shape().nameKey()));
    }

    private static Component shapeHint() {
        return Component.translatable(MassdigClient.config().shape().hintKey());
    }

    private static Component matchModeMessage() {
        return labelValue("massdig.screen.match", Component.translatable(MassdigClient.config().matchMode().nameKey()));
    }

    private static Component matchModeHint() {
        return Component.translatable(MassdigClient.config().matchMode().hintKey());
    }

    private static Component autoTuneMessage() {
        return labelValue("massdig.screen.auto_tune", onOff(MassdigClient.config().smartAutoTune));
    }

    private static Component protectionMessage() {
        return labelValue("massdig.screen.protection", Component.translatable(MassdigClient.config().protection().nameKey()));
    }

    private static Component protectionHint() {
        return Component.translatable(MassdigClient.config().protection().hintKey());
    }

    private static Component hardFirstMessage() {
        return labelValue("massdig.screen.hard_first", onOff(MassdigClient.config().hardBlocksFirst));
    }

    private static Component autoSlowdownMessage() {
        return labelValue("massdig.screen.auto_slowdown", onOff(MassdigClient.config().autoSlowdown));
    }

    private static Component keepWithinReachMessage() {
        return labelValue("massdig.screen.keep_within_reach", onOff(MassdigClient.config().keepWithinReach));
    }

    private static Component protectUsefulMessage() {
        return labelValue("massdig.screen.protect_useful", onOff(MassdigClient.config().protectUsefulBlocks));
    }

    private static Component protectFragileMessage() {
        return labelValue("massdig.screen.protect_fragile", onOff(MassdigClient.config().protectFragileBlocks));
    }

    private static Component playerSpaceMessage() {
        return labelValue("massdig.screen.player_space", onOff(MassdigClient.config().protectPlayerSpace));
    }

    private static Component avoidLavaMessage() {
        return labelValue("massdig.screen.avoid_lava", onOff(MassdigClient.config().avoidLava));
    }

    private static Component lowHealthMessage() {
        return labelValue("massdig.screen.low_health", onOff(MassdigClient.config().pauseOnLowHealth));
    }

    private static Component labelValue(String key, Object value) {
        return labelValue(key, Component.literal(String.valueOf(value)));
    }

    private static Component labelValue(String key, Component value) {
        return Component.empty().append(Component.translatable(key)).append(": ").append(value);
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "massdig.value.on" : "massdig.value.off");
    }

    private static final class RadiusSlider extends AbstractSliderButton {
        RadiusSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.getRadius() - 1) / (double) (MAX_RADIUS - 1));
            setTooltip(Tooltip.create(Component.translatable("massdig.tooltip.radius")));
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.setRadius(1 + (int) Math.round(value * (MAX_RADIUS - 1)));
        }

        private static Component message() {
            return labelValue("massdig.screen.radius", MassdigClient.getRadius());
        }
    }

    private static final class PacketLimitSlider extends AbstractSliderButton {
        PacketLimitSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().packetLimitPerSecond - 40) / 220.0D);
            setTooltip(Tooltip.create(Component.translatable("massdig.tooltip.packet_limit")));
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().packetLimitPerSecond = 40 + (int) Math.round(value * 220.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.screen.packet_limit", MassdigClient.config().packetLimitPerSecond);
        }
    }

    private static final class SafetySlider extends AbstractSliderButton {
        SafetySlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), MassdigClient.config().safetyExtraTicks / 20.0D);
            setTooltip(Tooltip.create(Component.translatable("massdig.tooltip.safety")));
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().safetyExtraTicks = (int) Math.round(value * 20.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.screen.safety", "+" + MassdigClient.config().safetyExtraTicks);
        }
    }
}
