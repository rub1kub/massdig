package org.main.massdig.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MassdigGuideScreen extends Screen {
    private final Screen parent;
    private int layoutX;
    private int layoutTop;
    private int layoutWidth;
    private int columnWidth;
    private int leftX;
    private int rightX;
    private int contentY;
    private int footerY;
    private boolean compact;

    public MassdigGuideScreen(Screen parent) {
        super(Component.translatable("massdig.guide.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compact = width < 760;
        layoutWidth = Math.min(compact ? 430 : 820, width - 24);
        layoutX = (width - layoutWidth) / 2;
        layoutTop = Math.max(10, height / 2 - (compact ? 218 : 146));
        columnWidth = compact ? layoutWidth : (layoutWidth - 12) / 2;
        leftX = layoutX;
        rightX = compact ? layoutX : layoutX + columnWidth + 12;
        contentY = layoutTop + 44;
        footerY = contentY + (compact ? 332 : 236);

        int buttonWidth = (layoutWidth - 20) / 3;
        addRenderableWidget(Button.builder(Component.translatable("massdig.guide.open_radius"), button -> {
                    Minecraft.getInstance().setScreen(new RadiusDrillScreen(parent));
                })
                .bounds(layoutX, footerY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.hub.radius_tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("massdig.guide.open_auto"), button -> {
                    Minecraft.getInstance().setScreen(new AutoDigScreen(parent));
                })
                .bounds(layoutX + buttonWidth + 10, footerY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.auto_jobs")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("massdig.auto.screen.back"), button -> {
                    Minecraft.getInstance().setScreen(parent);
                })
                .bounds(layoutX + (buttonWidth + 10) * 2, footerY, buttonWidth, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, layoutTop, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("massdig.guide.subtitle"), width / 2, layoutTop + 14, 0xA0A0A0);

        drawBlock(graphics, leftX, contentY, "massdig.guide.choose.title",
                "massdig.guide.choose.1", "massdig.guide.choose.2", "massdig.guide.choose.3", 0xFFD36E);
        drawBlock(graphics, compact ? leftX : rightX, compact ? contentY + 78 : contentY,
                "massdig.guide.fast.title", "massdig.guide.fast.1", "massdig.guide.fast.2", "massdig.guide.fast.3", 0x39A9FF);
        drawBlock(graphics, leftX, compact ? contentY + 156 : contentY + 118,
                "massdig.guide.radius.title", "massdig.guide.radius.1", "massdig.guide.radius.2", "massdig.guide.radius.3", 0xFF9F1C);
        drawBlock(graphics, compact ? leftX : rightX, compact ? contentY + 234 : contentY + 118,
                "massdig.guide.auto.title", "massdig.guide.auto.1", "massdig.guide.auto.2", "massdig.guide.auto.3", 0x39F56B);
    }

    private void drawBlock(GuiGraphicsExtractor graphics, int x, int y, String titleKey,
                           String line1Key, String line2Key, String line3Key, int accent) {
        graphics.fill(x, y, x + columnWidth, y + 68, 0xAA101014);
        graphics.fill(x, y, x + columnWidth, y + 2, accent);
        graphics.text(font, Component.translatable(titleKey), x + 10, y + 9, 0xFFFFFF);
        graphics.text(font, Component.translatable(line1Key), x + 10, y + 25, 0xB8B8B8);
        graphics.text(font, Component.translatable(line2Key), x + 10, y + 38, 0xB8B8B8);
        graphics.text(font, Component.translatable(line3Key), x + 10, y + 51, 0x8F8F8F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
