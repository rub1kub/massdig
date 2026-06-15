package org.main.massdig.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RadiusDrillScreen extends Screen {
    private static final int MAX_RADIUS = 6;
    private static final int ROW = 24;

    private final Screen parent;
    private final boolean advanced;
    private int layoutX;
    private int layoutTop;
    private int layoutWidth;
    private int columnWidth;
    private int gap;
    private int leftX;
    private int middleX;
    private int rightX;
    private int controlsY;
    private int footerY;
    private boolean compact;

    public RadiusDrillScreen(Screen parent) {
        this(parent, false);
    }

    private RadiusDrillScreen(Screen parent, boolean advanced) {
        super(Component.translatable("massdig.radius.screen.title"));
        this.parent = parent;
        this.advanced = advanced;
    }

    @Override
    protected void init() {
        compact = width < 760;
        gap = 10;
        layoutWidth = Math.min(compact ? 430 : 820, width - 24);
        layoutX = (width - layoutWidth) / 2;
        layoutTop = Math.max(8, height / 2 - (compact ? 218 : 146));
        columnWidth = compact ? (layoutWidth - gap) / 2 : (layoutWidth - gap * 2) / 3;
        controlsY = layoutTop + 58;

        leftX = layoutX;
        middleX = layoutX + columnWidth + gap;
        rightX = compact ? layoutX : layoutX + (columnWidth + gap) * 2;

        addBasicControls(leftX, controlsY);
        addSafetyControls(middleX, controlsY);
        int advancedY = compact ? controlsY + ROW * 8 : controlsY;
        if (advanced) {
            addAdvancedControls(rightX, advancedY);
        } else {
            addFriendlyHelp(rightX, advancedY);
        }

        footerY = controlsY + (compact && advanced ? ROW * 15 : ROW * 8);
        addFooterControls();
    }

    private void addBasicControls(int x, int y) {
        addRenderableWidget(button(radiusEnabledMessage(), x, y, "massdig.tooltip.radius_enabled", b -> {
            MassdigClient.toggle();
            rebuild();
        }));
        addRenderableWidget(button(presetMessage(), x, y + ROW, "massdig.tooltip.preset", b -> {
            MassdigClient.config().preset().next().apply(MassdigClient.config());
            rebuild();
        }));
        addRenderableWidget(new RadiusSlider(x, y + ROW * 2, columnWidth, 20));
        addRenderableWidget(button(shapeMessage(), x, y + ROW * 3, "massdig.tooltip.shape", b -> {
            MassdigClient.config().setShape(MassdigClient.config().shape().next());
            MassdigClient.config().save();
            b.setMessage(shapeMessage());
            b.setTooltip(Tooltip.create(shapeHint()));
        }));
        addRenderableWidget(button(matchModeMessage(), x, y + ROW * 4, "massdig.tooltip.match", b -> {
            MassdigClient.config().setMatchMode(MassdigClient.config().matchMode().next());
            MassdigClient.config().save();
            b.setMessage(matchModeMessage());
            b.setTooltip(Tooltip.create(matchModeHint()));
        }));
        addRenderableWidget(button(protectionMessage(), x, y + ROW * 5, "massdig.tooltip.protection", b -> {
            MassdigClient.config().setProtection(MassdigClient.config().protection().next());
            MassdigClient.config().save();
            b.setMessage(protectionMessage());
            b.setTooltip(Tooltip.create(protectionHint()));
        }));
        addRenderableWidget(button(modeMessage(), x, y + ROW * 6, "massdig.tooltip.mode", b -> {
            MassdigClient.config().setMode(MassdigClient.config().mode().next());
            MassdigClient.config().save();
            b.setMessage(modeMessage());
            b.setTooltip(Tooltip.create(modeHint()));
        }));
    }

    private void addSafetyControls(int x, int y) {
        addRenderableWidget(button(previewMessage(), x, y, "massdig.tooltip.preview", b -> {
            MassdigClient.config().showPreview = !MassdigClient.config().showPreview;
            MassdigClient.config().save();
            b.setMessage(previewMessage());
        }));
        addRenderableWidget(button(protectUsefulMessage(), x, y + ROW, "massdig.tooltip.protect_useful", b -> {
            MassdigClient.config().protectUsefulBlocks = !MassdigClient.config().protectUsefulBlocks;
            MassdigClient.config().save();
            b.setMessage(protectUsefulMessage());
        }));
        addRenderableWidget(button(playerSpaceMessage(), x, y + ROW * 2, "massdig.tooltip.player_space", b -> {
            MassdigClient.config().protectPlayerSpace = !MassdigClient.config().protectPlayerSpace;
            MassdigClient.config().save();
            b.setMessage(playerSpaceMessage());
        }));
        addRenderableWidget(button(avoidLavaMessage(), x, y + ROW * 3, "massdig.tooltip.avoid_lava", b -> {
            MassdigClient.config().avoidLava = !MassdigClient.config().avoidLava;
            MassdigClient.config().save();
            b.setMessage(avoidLavaMessage());
        }));
        addRenderableWidget(button(keepWithinReachMessage(), x, y + ROW * 4, "massdig.tooltip.keep_within_reach", b -> {
            MassdigClient.config().keepWithinReach = !MassdigClient.config().keepWithinReach;
            MassdigClient.config().save();
            b.setMessage(keepWithinReachMessage());
        }));
        addRenderableWidget(button(autoTuneMessage(), x, y + ROW * 5, "massdig.tooltip.auto_tune", b -> {
            MassdigClient.config().smartAutoTune = !MassdigClient.config().smartAutoTune;
            MassdigClient.config().save();
            b.setMessage(autoTuneMessage());
        }));
        addRenderableWidget(button(autoSlowdownMessage(), x, y + ROW * 6, "massdig.tooltip.auto_slowdown", b -> {
            MassdigClient.config().autoSlowdown = !MassdigClient.config().autoSlowdown;
            MassdigClient.config().save();
            b.setMessage(autoSlowdownMessage());
        }));
    }

    private void addFriendlyHelp(int x, int y) {
        addRenderableWidget(button(Component.translatable("massdig.radius.screen.advanced"), x, y + ROW * 5,
                "massdig.radius.tooltip.advanced", b -> Minecraft.getInstance().setScreen(new RadiusDrillScreen(parent, true))));
        addRenderableWidget(button(Component.translatable("massdig.hub.guide"), x, y + ROW * 6,
                "massdig.hub.guide_tooltip", b -> Minecraft.getInstance().setScreen(new MassdigGuideScreen(this))));
    }

    private void addAdvancedControls(int x, int y) {
        addRenderableWidget(button(hudMessage(), x, y, "massdig.tooltip.hud", b -> {
            MassdigClient.config().hudEnabled = !MassdigClient.config().hudEnabled;
            MassdigClient.config().save();
            b.setMessage(hudMessage());
        }));
        addRenderableWidget(button(skippedPreviewMessage(), x, y + ROW, "massdig.tooltip.skipped_preview", b -> {
            MassdigClient.config().showSkippedPreview = !MassdigClient.config().showSkippedPreview;
            MassdigClient.config().save();
            b.setMessage(skippedPreviewMessage());
        }));
        addRenderableWidget(button(pauseScreenMessage(), x, y + ROW * 2, "massdig.tooltip.pause_screen", b -> {
            MassdigClient.config().pauseWhenScreenOpen = !MassdigClient.config().pauseWhenScreenOpen;
            MassdigClient.config().save();
            b.setMessage(pauseScreenMessage());
        }));
        addRenderableWidget(button(hardFirstMessage(), x, y + ROW * 3, "massdig.tooltip.hard_first", b -> {
            MassdigClient.config().hardBlocksFirst = !MassdigClient.config().hardBlocksFirst;
            MassdigClient.config().save();
            b.setMessage(hardFirstMessage());
        }));
        addRenderableWidget(button(protectFragileMessage(), x, y + ROW * 4, "massdig.tooltip.protect_fragile", b -> {
            MassdigClient.config().protectFragileBlocks = !MassdigClient.config().protectFragileBlocks;
            MassdigClient.config().save();
            b.setMessage(protectFragileMessage());
        }));
        addRenderableWidget(button(lowHealthMessage(), x, y + ROW * 5, "massdig.tooltip.low_health", b -> {
            MassdigClient.config().pauseOnLowHealth = !MassdigClient.config().pauseOnLowHealth;
            MassdigClient.config().save();
            b.setMessage(lowHealthMessage());
        }));
        addRenderableWidget(new PacketLimitSlider(x, y + ROW * 6, columnWidth, 20));
        addRenderableWidget(new SafetySlider(x, y + ROW * 7, columnWidth, 20));
    }

    private void addFooterControls() {
        int buttonWidth = (layoutWidth - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.translatable("massdig.screen.reset"), button -> {
                    MassdigPreset.NORMAL.apply(MassdigClient.config());
                    rebuild();
                })
                .bounds(layoutX, footerY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.reset")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable(advanced ? "massdig.radius.screen.simple" : "massdig.radius.screen.advanced"), button -> {
                    Minecraft.getInstance().setScreen(new RadiusDrillScreen(parent, !advanced));
                })
                .bounds(layoutX + buttonWidth + gap, footerY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.radius.tooltip.advanced")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("massdig.auto.screen.back"), button -> {
                    Minecraft.getInstance().setScreen(parent);
                })
                .bounds(layoutX + (buttonWidth + gap) * 2, footerY, buttonWidth, 20)
                .build());
    }

    private Button button(Component message, int x, int y, String tooltip, Button.OnPress onPress) {
        return Button.builder(message, onPress)
                .bounds(x, y, columnWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(tooltip)))
                .build();
    }

    private void rebuild() {
        Minecraft.getInstance().setScreen(new RadiusDrillScreen(parent, advanced));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, layoutTop, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("massdig.radius.screen.subtitle"), width / 2, layoutTop + 14, 0xA0A0A0);

        drawSection(graphics, leftX, controlsY - 22, Component.translatable("massdig.radius.section.basic"),
                Component.translatable("massdig.radius.section.basic_hint"), 0xFFD36E);
        drawSection(graphics, middleX, controlsY - 22, Component.translatable("massdig.radius.section.safety"),
                Component.translatable("massdig.radius.section.safety_hint"), 0x39A9FF);
        int advancedY = compact ? controlsY + ROW * 8 : controlsY;
        drawSection(graphics, rightX, advancedY - 22,
                Component.translatable(advanced ? "massdig.radius.section.advanced" : "massdig.radius.section.help"),
                Component.translatable(advanced ? "massdig.radius.section.advanced_hint" : "massdig.radius.section.help_hint"),
                advanced ? 0xFF9F1C : 0x39F56B);

        if (!advanced) {
            drawHelp(graphics, rightX, advancedY);
        }
    }

    private void drawSection(GuiGraphicsExtractor graphics, int x, int y, Component title, Component hint, int color) {
        graphics.text(font, title, x, y, color);
        graphics.text(font, hint, x, y + 10, 0x8F8F8F);
    }

    private void drawHelp(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + columnWidth, y + ROW * 5 - 4, 0xAA101014);
        graphics.text(font, Component.translatable("massdig.radius.help.line1"), x + 10, y + 12, 0xFFFFFF);
        graphics.text(font, Component.translatable("massdig.radius.help.line2"), x + 10, y + 28, 0xB8B8B8);
        graphics.text(font, Component.translatable("massdig.radius.help.line3"), x + 10, y + 44, 0xB8B8B8);
        graphics.text(font, Component.translatable("massdig.radius.help.line4"), x + 10, y + 60, 0x8F8F8F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
