package org.main.massdig.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public final class AutoDigScreen extends Screen {
    private static final int ROW = 24;

    private final Screen parent;
    private int layoutX;
    private int layoutTop;
    private int layoutWidth;
    private int columnWidth;
    private int columnGap;
    private int taskX;
    private int sizeX;
    private int toolsX;
    private int runX;
    private int taskY;
    private int doneY;
    private boolean compactColumns;

    public AutoDigScreen(Screen parent) {
        super(Component.translatable("massdig.auto.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compactColumns = width < 760;
        columnGap = 10;
        layoutWidth = Math.min(compactColumns ? 430 : 820, width - 24);
        layoutX = (width - layoutWidth) / 2;
        layoutTop = Math.max(8, height / 2 - (compactColumns ? 212 : 132));
        columnWidth = compactColumns ? (layoutWidth - columnGap) / 2 : (layoutWidth - columnGap * 3) / 4;

        taskX = layoutX;
        sizeX = layoutX + columnWidth + columnGap;
        toolsX = compactColumns ? layoutX : layoutX + (columnWidth + columnGap) * 2;
        runX = compactColumns ? sizeX : layoutX + (columnWidth + columnGap) * 3;
        taskY = layoutTop + 58;
        int sectionHeight = compactColumns ? 190 : 280;
        int secondRowY = compactColumns ? taskY + sectionHeight : taskY;
        doneY = (compactColumns ? secondRowY : taskY) + sectionHeight;

        addTaskControls(taskX, taskY);
        addSizeControls(sizeX, taskY);
        addToolControls(toolsX, secondRowY);
        addRunControls(runX, secondRowY);

        int halfWidth = (layoutWidth - columnGap) / 2;
        addRenderableWidget(Button.builder(Component.translatable("massdig.auto.screen.back"), button -> Minecraft.getInstance().setScreen(parent))
                .bounds(layoutX, doneY, halfWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> Minecraft.getInstance().setScreen(null))
                .bounds(layoutX + halfWidth + columnGap, doneY, halfWidth, 20)
                .build());
    }

    private void addTaskControls(int x, int y) {
        addRenderableWidget(button(typeMessage(), x, y, "massdig.auto.tooltip.type", b -> {
            MassdigClient.config().setAutoJobType(MassdigClient.config().autoJobType().next());
            MassdigClient.config().save();
            b.setMessage(typeMessage());
            b.setTooltip(Tooltip.create(typeHint()));
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.point_a"), x, y + ROW, "massdig.auto.tooltip.point_a", b -> {
            AutoDigController.setPointAFromLook(Minecraft.getInstance());
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.point_b"), x, y + ROW * 2, "massdig.auto.tooltip.point_b", b -> {
            AutoDigController.setPointBFromLook(Minecraft.getInstance());
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.preview"), x, y + ROW * 3, "massdig.auto.tooltip.preview", b -> {
            AutoDigController.rebuildPlan(Minecraft.getInstance());
            rebuild();
        }));
        addRenderableWidget(button(keepOresMessage(), x, y + ROW * 4, "massdig.auto.tooltip.keep_ores", b -> {
            MassdigClient.config().autoKeepOres = !MassdigClient.config().autoKeepOres;
            MassdigClient.config().save();
            b.setMessage(keepOresMessage());
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.expand"), x, y + ROW * 5, "massdig.auto.tooltip.expand", b -> {
            AutoDigController.expandSelection(Minecraft.getInstance(), 1);
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.shrink"), x, y + ROW * 6, "massdig.auto.tooltip.shrink", b -> {
            AutoDigController.expandSelection(Minecraft.getInstance(), -1);
            rebuild();
        }));
    }

    private void addSizeControls(int x, int y) {
        addRenderableWidget(new WidthSlider(x, y, columnWidth, 20));
        addRenderableWidget(new HeightSlider(x, y + ROW, columnWidth, 20));
        addRenderableWidget(new LengthSlider(x, y + ROW * 2, columnWidth, 20));
        addRenderableWidget(new DepthSlider(x, y + ROW * 3, columnWidth, 20));
        addRenderableWidget(new BranchSpacingSlider(x, y + ROW * 4, columnWidth, 20));
        addRenderableWidget(new BranchLengthSlider(x, y + ROW * 5, columnWidth, 20));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.use_look"), x, y + ROW * 6, "massdig.auto.tooltip.use_look", b -> {
            AutoDigController.setPointAFromLook(Minecraft.getInstance());
            AutoDigController.rebuildPlan(Minecraft.getInstance());
            rebuild();
        }));
    }

    private void addToolControls(int x, int y) {
        addRenderableWidget(button(toolSwitchMessage(), x, y, "massdig.auto.tooltip.tool_switch", b -> {
            MassdigClient.config().autoToolSwitch = !MassdigClient.config().autoToolSwitch;
            MassdigClient.config().save();
            b.setMessage(toolSwitchMessage());
        }));
        addRenderableWidget(new DurabilitySlider(x, y + ROW, columnWidth, 20));
        addRenderableWidget(button(noToolMessage(), x, y + ROW * 2, "massdig.auto.tooltip.no_tool", b -> {
            MassdigClient.config().autoStopWhenNoTool = !MassdigClient.config().autoStopWhenNoTool;
            MassdigClient.config().save();
            b.setMessage(noToolMessage());
        }));
        addRenderableWidget(button(inventoryFullMessage(), x, y + ROW * 3, "massdig.auto.tooltip.inventory", b -> {
            MassdigClient.config().autoStopWhenInventoryFull = !MassdigClient.config().autoStopWhenInventoryFull;
            MassdigClient.config().save();
            b.setMessage(inventoryFullMessage());
        }));
        addRenderableWidget(button(autopilotMessage(), x, y + ROW * 4, "massdig.auto.tooltip.autopilot", b -> {
            MassdigClient.config().autoAutopilot = !MassdigClient.config().autoAutopilot;
            MassdigClient.config().save();
            b.setMessage(autopilotMessage());
        }));
        addRenderableWidget(button(bothBranchesMessage(), x, y + ROW * 5, "massdig.auto.tooltip.both_branches", b -> {
            MassdigClient.config().autoBothBranches = !MassdigClient.config().autoBothBranches;
            MassdigClient.config().save();
            b.setMessage(bothBranchesMessage());
        }));
        addRenderableWidget(button(orderMessage(), x, y + ROW * 6, "massdig.auto.tooltip.order", b -> {
            MassdigClient.config().setAutoPlanOrder(MassdigClient.config().autoPlanOrder().next());
            MassdigClient.config().save();
            b.setMessage(orderMessage());
        }));
    }

    private void addRunControls(int x, int y) {
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.start_pause"), x, y, "massdig.auto.tooltip.start_pause", b -> {
            AutoDigController.toggleRunning(Minecraft.getInstance());
            if (AutoDigController.state() != AutoDigState.RUNNING) {
                rebuild();
            }
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.stop"), x, y + ROW, "massdig.auto.tooltip.stop", b -> {
            AutoDigController.stop(Minecraft.getInstance());
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.open_main"), x, y + ROW * 2, "massdig.auto.tooltip.open_main", b -> {
            Minecraft.getInstance().setScreen(new MassdigScreen(this));
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.layer_down"), x, y + ROW * 3, "massdig.auto.tooltip.layer", b -> {
            AutoDigController.layerDown(Minecraft.getInstance());
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.layer_up"), x, y + ROW * 4, "massdig.auto.tooltip.layer", b -> {
            AutoDigController.layerUp(Minecraft.getInstance());
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.move_up"), x, y + ROW * 5, "massdig.auto.tooltip.move_y", b -> {
            AutoDigController.moveSelection(Minecraft.getInstance(), Direction.UP, 1);
            rebuild();
        }));
        addRenderableWidget(button(Component.translatable("massdig.auto.screen.move_down"), x, y + ROW * 6, "massdig.auto.tooltip.move_y", b -> {
            AutoDigController.moveSelection(Minecraft.getInstance(), Direction.DOWN, 1);
            rebuild();
        }));
    }

    private Button button(Component message, int x, int y, String tooltip, Button.OnPress onPress) {
        return Button.builder(message, onPress)
                .bounds(x, y, columnWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(tooltip)))
                .build();
    }

    private void rebuild() {
        Minecraft.getInstance().setScreen(new AutoDigScreen(parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, layoutTop, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("massdig.auto.screen.subtitle"), width / 2, layoutTop + 14, 0xA0A0A0);

        int sectionHeight = compactColumns ? 190 : 280;
        int secondRowY = compactColumns ? taskY + sectionHeight : taskY;
        drawSection(graphics, taskX, taskY - 22, Component.translatable("massdig.auto.section.task"), Component.translatable("massdig.auto.section.task_hint"));
        drawSection(graphics, sizeX, taskY - 22, Component.translatable("massdig.auto.section.size"), Component.translatable("massdig.auto.section.size_hint"));
        drawSection(graphics, toolsX, secondRowY - 22, Component.translatable("massdig.auto.section.tools"), Component.translatable("massdig.auto.section.tools_hint"));
        drawSection(graphics, runX, secondRowY - 22, Component.translatable("massdig.auto.section.run"), Component.translatable("massdig.auto.section.run_hint"));

        int infoY = doneY - 34;
        AutoDigController.drawMiniMap(graphics, layoutX, doneY - 106, Math.min(220, layoutWidth / 2 - 8), 64);
        graphics.text(font, pointLine("A", AutoDigController.pointA()), layoutX, infoY, 0xB8E986);
        graphics.text(font, pointLine("B", AutoDigController.pointB()), layoutX, infoY + 11, 0xB8E986);
        graphics.text(font, Component.empty()
                .append(Component.translatable("massdig.auto.screen.plan"))
                .append(": ")
                .append(Integer.toString(AutoDigController.plannedCount()))
                .append(" / ")
                .append(Component.translatable("massdig.auto.screen.protected"))
                .append(" ")
                .append(Integer.toString(AutoDigController.protectedCount()))
                .append(" / ")
                .append(Component.translatable("massdig.auto.screen.eta"))
                .append(" ")
                .append(Integer.toString(AutoDigController.estimatedSeconds()))
                .append("s"), layoutX + layoutWidth / 2, infoY, 0xA0A0A0);
        graphics.text(font, Component.empty()
                .append(Component.translatable("massdig.auto.screen.state"))
                .append(": ")
                .append(Component.literal(AutoDigController.state().name()))
                .append("  Y:")
                .append(Integer.toString(MassdigClient.config().autoLayerY))
                .append("  ")
                .append(AutoDigController.status()), layoutX + layoutWidth / 2, infoY + 11, 0xA0A0A0);
    }

    private Component pointLine(String label, BlockPos pos) {
        if (pos == null) {
            return Component.literal(label + ": -");
        }
        return Component.literal(label + ": " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    private void drawSection(GuiGraphicsExtractor graphics, int x, int y, Component title, Component hint) {
        graphics.text(font, title, x, y, 0x39F56B);
        graphics.text(font, hint, x, y + 10, 0x8F8F8F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component typeMessage() {
        return labelValue("massdig.auto.screen.type", Component.translatable(MassdigClient.config().autoJobType().nameKey()));
    }

    private static Component typeHint() {
        return Component.translatable(MassdigClient.config().autoJobType().hintKey());
    }

    private static Component keepOresMessage() {
        return labelValue("massdig.auto.screen.keep_ores", onOff(MassdigClient.config().autoKeepOres));
    }

    private static Component toolSwitchMessage() {
        return labelValue("massdig.auto.screen.tool_switch", onOff(MassdigClient.config().autoToolSwitch));
    }

    private static Component noToolMessage() {
        return labelValue("massdig.auto.screen.no_tool", onOff(MassdigClient.config().autoStopWhenNoTool));
    }

    private static Component inventoryFullMessage() {
        return labelValue("massdig.auto.screen.inventory", onOff(MassdigClient.config().autoStopWhenInventoryFull));
    }

    private static Component autopilotMessage() {
        return labelValue("massdig.auto.screen.autopilot", onOff(MassdigClient.config().autoAutopilot));
    }

    private static Component bothBranchesMessage() {
        return labelValue("massdig.auto.screen.both_branches", onOff(MassdigClient.config().autoBothBranches));
    }

    private static Component orderMessage() {
        return labelValue("massdig.auto.screen.order", Component.translatable(MassdigClient.config().autoPlanOrder().nameKey()));
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

    private abstract static class AutoSlider extends AbstractSliderButton {
        AutoSlider(int x, int y, int width, int height, Component message, double value, String tooltip) {
            super(x, y, width, height, message, value);
            setTooltip(Tooltip.create(Component.translatable(tooltip)));
        }
    }

    private static final class WidthSlider extends AutoSlider {
        WidthSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().autoWidth - 1) / 8.0D, "massdig.auto.tooltip.width");
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().autoWidth = 1 + (int) Math.round(value * 8.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.auto.screen.width", MassdigClient.config().autoWidth);
        }
    }

    private static final class HeightSlider extends AutoSlider {
        HeightSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().autoHeight - 1) / 5.0D, "massdig.auto.tooltip.height");
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().autoHeight = 1 + (int) Math.round(value * 5.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.auto.screen.height", MassdigClient.config().autoHeight);
        }
    }

    private static final class LengthSlider extends AutoSlider {
        LengthSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().autoLength - 1) / 255.0D, "massdig.auto.tooltip.length");
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().autoLength = 1 + (int) Math.round(value * 255.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.auto.screen.length", MassdigClient.config().autoLength);
        }
    }

    private static final class DepthSlider extends AutoSlider {
        DepthSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().autoDepth - 1) / 127.0D, "massdig.auto.tooltip.depth");
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().autoDepth = 1 + (int) Math.round(value * 127.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.auto.screen.depth", MassdigClient.config().autoDepth);
        }
    }

    private static final class DurabilitySlider extends AutoSlider {
        DurabilitySlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().autoMinToolDurability - 1) / 89.0D, "massdig.auto.tooltip.durability");
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().autoMinToolDurability = 1 + (int) Math.round(value * 89.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.auto.screen.durability", MassdigClient.config().autoMinToolDurability + "%");
        }
    }

    private static final class BranchSpacingSlider extends AutoSlider {
        BranchSpacingSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().autoBranchSpacing - 2) / 14.0D, "massdig.auto.tooltip.branch_spacing");
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().autoBranchSpacing = 2 + (int) Math.round(value * 14.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.auto.screen.branch_spacing", MassdigClient.config().autoBranchSpacing);
        }
    }

    private static final class BranchLengthSlider extends AutoSlider {
        BranchLengthSlider(int x, int y, int width, int height) {
            super(x, y, width, height, message(), (MassdigClient.config().autoBranchLength - 4) / 124.0D, "massdig.auto.tooltip.branch_length");
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            MassdigClient.config().autoBranchLength = 4 + (int) Math.round(value * 124.0D);
            MassdigClient.config().save();
        }

        private static Component message() {
            return labelValue("massdig.auto.screen.branch_length", MassdigClient.config().autoBranchLength);
        }
    }
}
