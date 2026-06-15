package org.main.massdig.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MassdigScreen extends Screen {
    private static final int ROW = 24;

    private final Screen parent;
    private int layoutX;
    private int layoutTop;
    private int layoutWidth;
    private int leftX;
    private int rightX;
    private int columnWidth;
    private int mainY;
    private int footerY;
    private boolean compact;

    public MassdigScreen(Screen parent) {
        super(Component.translatable("massdig.hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compact = width < 760;
        layoutWidth = Math.min(compact ? 420 : 640, width - 24);
        layoutX = (width - layoutWidth) / 2;
        layoutTop = Math.max(12, height / 2 - (compact ? 190 : 132));
        columnWidth = compact ? layoutWidth : (layoutWidth - 10) / 2;
        leftX = layoutX;
        rightX = compact ? layoutX : layoutX + columnWidth + 10;
        mainY = layoutTop + 42;

        addMainActions();
        addHelpActions();
        addFooterActions();
    }

    private void addMainActions() {
        int y = mainY;
        addRenderableWidget(button(fastMiningMessage(), leftX, y, columnWidth, "massdig.tooltip.fast_mining", button -> {
            MassdigClient.config().fastMiningEnabled = !MassdigClient.config().fastMiningEnabled;
            MassdigClient.config().save();
            rebuild();
        }));
        addRenderableWidget(labelButton(Component.translatable("massdig.hub.fast.short"), leftX, y + ROW, columnWidth));

        y += ROW * 3;
        addRenderableWidget(button(radiusEnabledMessage(), leftX, y, columnWidth, "massdig.tooltip.radius_enabled", button -> {
            MassdigClient.toggle();
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.hub.configure_radius", radiusShortStatus()),
                leftX, y + ROW, columnWidth, "massdig.hub.radius_tooltip", button -> {
                    Minecraft.getInstance().setScreen(new RadiusDrillScreen(this));
                }));
        addRenderableWidget(labelButton(Component.translatable("massdig.hub.radius.short"), leftX, y + ROW * 2, columnWidth));

        y += ROW * 4;
        addRenderableWidget(button(Component.translatable("massdig.hub.open_autodig"), leftX, y, columnWidth,
                "massdig.tooltip.auto_jobs", button -> Minecraft.getInstance().setScreen(new AutoDigScreen(this))));
        addRenderableWidget(labelButton(Component.translatable("massdig.hub.auto.short"), leftX, y + ROW, columnWidth));
    }

    private void addHelpActions() {
        int x = compact ? leftX : rightX;
        int y = compact ? mainY + ROW * 10 : mainY;

        addRenderableWidget(labelButton(Component.translatable("massdig.hub.pick.title"), x, y, columnWidth));
        addRenderableWidget(labelButton(Component.translatable("massdig.hub.pick.fast"), x, y + ROW, columnWidth));
        addRenderableWidget(labelButton(Component.translatable("massdig.hub.pick.radius"), x, y + ROW * 2, columnWidth));
        addRenderableWidget(labelButton(Component.translatable("massdig.hub.pick.auto"), x, y + ROW * 3, columnWidth));

        addRenderableWidget(button(Component.translatable("massdig.hub.safe_profile"), x, y + ROW * 5, columnWidth,
                "massdig.hub.safe_profile_tooltip", button -> {
                    MassdigPreset.SERVER.apply(MassdigClient.config());
                    rebuild();
                }));
        addRenderableWidget(button(Component.translatable("massdig.hub.guide"), x, y + ROW * 6, columnWidth,
                "massdig.hub.guide_tooltip", button -> Minecraft.getInstance().setScreen(new MassdigGuideScreen(this))));
    }

    private void addFooterActions() {
        footerY = compact ? mainY + ROW * 18 : mainY + ROW * 9;
        int half = (layoutWidth - 10) / 2;
        addRenderableWidget(Button.builder(Component.translatable("massdig.screen.reset"), button -> {
                    MassdigPreset.NORMAL.apply(MassdigClient.config());
                    rebuild();
                })
                .bounds(layoutX, footerY, half, 20)
                .tooltip(Tooltip.create(Component.translatable("massdig.tooltip.reset")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> Minecraft.getInstance().setScreen(parent))
                .bounds(layoutX + half + 10, footerY, half, 20)
                .build());
    }

    private Button button(Component message, int x, int y, int width, String tooltip, Button.OnPress onPress) {
        return Button.builder(message, onPress)
                .bounds(x, y, width, 20)
                .tooltip(Tooltip.create(Component.translatable(tooltip)))
                .build();
    }

    private Button labelButton(Component message, int x, int y, int width) {
        Button label = Button.builder(message, button -> {
        }).bounds(x, y, width, 20).build();
        label.active = false;
        return label;
    }

    private void rebuild() {
        Minecraft.getInstance().setScreen(new MassdigScreen(parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, layoutTop, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("massdig.hub.subtitle"), width / 2, layoutTop + 14, 0xA0A0A0);
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

    private static Component radiusShortStatus() {
        int side = MassdigClient.getRadius() * 2 + 1;
        return Component.literal(side + "x" + side).append(", ")
                .append(Component.translatable(MassdigClient.config().shape().nameKey()));
    }

    private static Component labelValue(String key, Component value) {
        return Component.empty().append(Component.translatable(key)).append(": ").append(value);
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "massdig.value.on" : "massdig.value.off");
    }
}
