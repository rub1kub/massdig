package org.main.massdig.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MassdigScreen extends Screen {
    private static final int CARD_GAP = 10;

    private final Screen parent;
    private int layoutX;
    private int layoutTop;
    private int layoutWidth;
    private int cardWidth;
    private int cardHeight;
    private int fastX;
    private int fastY;
    private int radiusX;
    private int radiusY;
    private int autoX;
    private int autoY;
    private int footerY;
    private boolean compact;

    public MassdigScreen(Screen parent) {
        super(Component.translatable("massdig.hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compact = width < 760;
        layoutWidth = Math.min(compact ? 430 : 920, width - 24);
        layoutX = (width - layoutWidth) / 2;
        layoutTop = Math.max(12, height / 2 - (compact ? 210 : 122));
        cardWidth = compact ? layoutWidth : (layoutWidth - CARD_GAP * 2) / 3;
        cardHeight = compact ? 82 : 124;

        fastX = layoutX;
        fastY = layoutTop + 46;
        radiusX = compact ? layoutX : layoutX + cardWidth + CARD_GAP;
        radiusY = compact ? fastY + cardHeight + CARD_GAP : fastY;
        autoX = compact ? layoutX : layoutX + (cardWidth + CARD_GAP) * 2;
        autoY = compact ? radiusY + cardHeight + CARD_GAP : fastY;
        footerY = (compact ? autoY : fastY) + cardHeight + 18;

        addModeButtons();
        addFooterButtons();
    }

    private void addModeButtons() {
        addRenderableWidget(Button.builder(fastMiningMessage(), button -> {
                    MassdigClient.config().fastMiningEnabled = !MassdigClient.config().fastMiningEnabled;
                    MassdigClient.config().save();
                    rebuild();
                })
                .bounds(fastX + 12, fastY + cardHeight - 32, cardWidth - 24, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.fast_mining")))
                .build());

        int radiusButtonWidth = (cardWidth - 30) / 2;
        addRenderableWidget(Button.builder(radiusEnabledMessage(), button -> {
                    MassdigClient.toggle();
                    rebuild();
                })
                .bounds(radiusX + 12, radiusY + cardHeight - 32, radiusButtonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.radius_enabled")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("massdig.hub.configure"), button -> {
                    Minecraft.getInstance().setScreen(new RadiusDrillScreen(this));
                })
                .bounds(radiusX + 18 + radiusButtonWidth, radiusY + cardHeight - 32, cardWidth - radiusButtonWidth - 30, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.hub.radius_tooltip")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("massdig.hub.open_autodig"), button -> {
                    Minecraft.getInstance().setScreen(new AutoDigScreen(this));
                })
                .bounds(autoX + 12, autoY + cardHeight - 32, cardWidth - 24, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.auto_jobs")))
                .build());
    }

    private void addFooterButtons() {
        int buttonWidth = (layoutWidth - CARD_GAP * 2) / 3;
        addRenderableWidget(Button.builder(Component.translatable("massdig.hub.guide"), button -> {
                    Minecraft.getInstance().setScreen(new MassdigGuideScreen(this));
                })
                .bounds(layoutX, footerY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.hub.guide_tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("massdig.hub.safe_profile"), button -> {
                    MassdigPreset.SERVER.apply(MassdigClient.config());
                    rebuild();
                })
                .bounds(layoutX + buttonWidth + CARD_GAP, footerY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.hub.safe_profile_tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
                    Minecraft.getInstance().setScreen(parent);
                })
                .bounds(layoutX + (buttonWidth + CARD_GAP) * 2, footerY, buttonWidth, 20)
                .build());
    }

    private void rebuild() {
        Minecraft.getInstance().setScreen(new MassdigScreen(parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, layoutTop, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("massdig.hub.subtitle"), width / 2, layoutTop + 14, 0xA0A0A0);

        drawCard(graphics, fastX, fastY, 0x6639A9FF,
                Component.translatable("massdig.hub.fast.title"),
                Component.translatable("massdig.hub.fast.status", onOff(MassdigClient.isFastMiningActive())),
                Component.translatable("massdig.hub.fast.line1"),
                Component.translatable("massdig.hub.fast.line2"));
        drawCard(graphics, radiusX, radiusY, 0x66FFD36E,
                Component.translatable("massdig.hub.radius.title"),
                radiusSummary(),
                Component.translatable("massdig.hub.radius.line1"),
                Component.translatable("massdig.hub.radius.line2"));
        drawCard(graphics, autoX, autoY, 0x6639F56B,
                Component.translatable("massdig.hub.auto.title"),
                Component.translatable("massdig.hub.auto.status", Component.literal(AutoDigController.state().name())),
                Component.translatable("massdig.hub.auto.line1"),
                Component.translatable("massdig.hub.auto.line2"));
    }

    private void drawCard(GuiGraphicsExtractor graphics, int x, int y, int accent, Component cardTitle,
                          Component status, Component line1, Component line2) {
        graphics.fill(x, y, x + cardWidth, y + cardHeight, 0xAA101014);
        graphics.fill(x, y, x + cardWidth, y + 2, accent);
        graphics.text(font, cardTitle, x + 12, y + 10, 0xFFFFFF);
        graphics.text(font, status, x + 12, y + 25, 0xFFD36E);
        graphics.text(font, line1, x + 12, y + 43, 0xB8B8B8);
        graphics.text(font, line2, x + 12, y + 55, 0x8F8F8F);
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

    private static Component radiusSummary() {
        int side = MassdigClient.getRadius() * 2 + 1;
        return Component.translatable("massdig.hub.radius.status",
                onOff(MassdigClient.isActive()),
                Component.literal(side + "x" + side),
                Component.translatable(MassdigClient.config().shape().nameKey()));
    }

    private static Component labelValue(String key, Component value) {
        return Component.empty().append(Component.translatable(key)).append(": ").append(value);
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "massdig.value.on" : "massdig.value.off");
    }
}
