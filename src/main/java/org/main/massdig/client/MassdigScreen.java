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

    private final Screen parent;
    private int layoutX;
    private int layoutTop;
    private int layoutWidth;
    private int columnWidth;
    private int columnGap;
    private int fastX;
    private int radiusX;
    private int advancedX;
    private int fastY;
    private int radiusY;
    private int advancedY;
    private int doneY;
    private boolean compactColumns;

    public MassdigScreen(Screen parent) {
        super(Component.translatable("massdig.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compactColumns = width < 560;
        columnGap = 10;
        layoutWidth = Math.min(compactColumns ? 390 : 610, width - 24);
        layoutX = (width - layoutWidth) / 2;
        layoutTop = Math.max(8, height / 2 - (compactColumns ? 168 : 132));
        columnWidth = compactColumns ? (layoutWidth - columnGap) / 2 : (layoutWidth - columnGap * 2) / 3;

        fastX = layoutX;
        radiusX = layoutX + columnWidth + columnGap;
        advancedX = compactColumns ? layoutX : layoutX + (columnWidth + columnGap) * 2;
        fastY = layoutTop + 62;
        radiusY = fastY;
        advancedY = compactColumns ? layoutTop + 174 : fastY;
        doneY = advancedY + 174;

        addRenderableWidget(Button.builder(fastMiningMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.fastMiningEnabled = !config.fastMiningEnabled;
            config.save();
            button.setMessage(fastMiningMessage());
        }).bounds(fastX, fastY, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.fast_mining"))).build());

        addRenderableWidget(Button.builder(radiusEnabledMessage(), button -> {
            MassdigClient.toggle();
            button.setMessage(radiusEnabledMessage());
        }).bounds(radiusX, radiusY, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.radius_enabled"))).build());
        addRenderableWidget(new RadiusSlider(radiusX, radiusY + 24, columnWidth, 20));

        Button mode = Button.builder(modeMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.setMode(config.mode().next());
            config.save();
            button.setMessage(modeMessage());
            button.setTooltip(Tooltip.create(modeHint()));
        }).bounds(radiusX, radiusY + 48, columnWidth, 20).tooltip(Tooltip.create(modeHint())).build();
        addRenderableWidget(mode);

        Button shape = Button.builder(shapeMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.setShape(config.shape().next());
            config.save();
            button.setMessage(shapeMessage());
            button.setTooltip(Tooltip.create(shapeHint()));
        }).bounds(radiusX, radiusY + 72, columnWidth, 20).tooltip(Tooltip.create(shapeHint())).build();
        addRenderableWidget(shape);

        addRenderableWidget(Button.builder(previewMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.showPreview = !config.showPreview;
            config.save();
            button.setMessage(previewMessage());
        }).bounds(radiusX, radiusY + 96, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.preview"))).build());

        addRenderableWidget(Button.builder(presetMessage(), button -> {
            MassdigPreset preset = MassdigClient.config().preset().next();
            preset.apply(MassdigClient.config());
            Minecraft.getInstance().setScreen(new MassdigScreen(parent));
        }).bounds(advancedX, advancedY, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.preset"))).build());
        addRenderableWidget(new PacketLimitSlider(advancedX, advancedY + 24, columnWidth, 20));
        addRenderableWidget(Button.builder(autoSlowdownMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.autoSlowdown = !config.autoSlowdown;
            config.save();
            button.setMessage(autoSlowdownMessage());
        }).bounds(advancedX, advancedY + 48, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.auto_slowdown"))).build());
        addRenderableWidget(Button.builder(keepWithinReachMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.keepWithinReach = !config.keepWithinReach;
            config.save();
            button.setMessage(keepWithinReachMessage());
        }).bounds(advancedX, advancedY + 72, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.keep_within_reach"))).build());
        addRenderableWidget(Button.builder(hardFirstMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.hardBlocksFirst = !config.hardBlocksFirst;
            config.save();
            button.setMessage(hardFirstMessage());
        }).bounds(advancedX, advancedY + 96, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.hard_first"))).build());
        addRenderableWidget(new SafetySlider(advancedX, advancedY + 120, columnWidth, 20));
        addRenderableWidget(Button.builder(sameBlockOnlyMessage(), button -> {
            MassdigConfig config = MassdigClient.config();
            config.sameBlockOnly = !config.sameBlockOnly;
            config.save();
            button.setMessage(sameBlockOnlyMessage());
        }).bounds(advancedX, advancedY + 144, columnWidth, 20).tooltip(Tooltip.create(Component.translatable("massdig.tooltip.same_block_only"))).build());

        int halfWidth = (layoutWidth - columnGap) / 2;
        addRenderableWidget(Button.builder(Component.translatable("massdig.screen.reset"), button -> {
                    MassdigPreset.NORMAL.apply(MassdigClient.config());
                    Minecraft.getInstance().setScreen(new MassdigScreen(parent));
                })
                .bounds(layoutX, doneY, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.reset")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> Minecraft.getInstance().setScreen(parent))
                .bounds(layoutX + halfWidth + columnGap, doneY, halfWidth, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, layoutTop, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("massdig.screen.subtitle"), width / 2, layoutTop + 14, 0xA0A0A0);

        drawSection(graphics, fastX, fastY - 22, Component.translatable("massdig.section.fast"), Component.translatable("massdig.section.fast_hint"));
        drawSection(graphics, radiusX, radiusY - 22, Component.translatable("massdig.section.radius"), Component.translatable("massdig.section.radius_hint"));
        drawSection(graphics, advancedX, advancedY - 22, Component.translatable("massdig.section.advanced"), Component.translatable("massdig.section.advanced_hint"));
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

    private static Component previewMessage() {
        return labelValue("massdig.screen.preview", onOff(MassdigClient.config().showPreview));
    }

    private static Component presetMessage() {
        return labelValue("massdig.screen.preset", Component.translatable(MassdigClient.config().preset().nameKey()));
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

    private static Component sameBlockOnlyMessage() {
        return labelValue("massdig.screen.same_block_only", onOff(MassdigClient.config().sameBlockOnly));
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
