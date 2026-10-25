package io.github.edilrey.nearlycraftable.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import io.github.edilrey.nearlycraftable.core.ItemKey;
import io.github.edilrey.nearlycraftable.core.MatchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class NearlyCraftableScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_ICONS = 5;
    private static final int SCROLLBAR_WIDTH = 6;

    private record MissingView(List<ItemStack> choices, int amount) {
    }

    private record Row(MatchResult result, ItemStack output, String lowerName, List<MissingView> missing) {
    }

    @Nullable
    private final Screen previous;
    private final List<Row> allRows = new ArrayList<>();
    private List<Row> visibleRows = List.of();
    private EditBox search;
    private int listLeft;
    private int listRight;
    private int listTop;
    private int listBottom;
    private int scroll;
    private boolean draggingScrollbar;

    public NearlyCraftableScreen(@Nullable Screen previous) {
        super(Component.translatable("screen.nearlycraftable.title"));
        this.previous = previous;
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen instanceof NearlyCraftableScreen) {
            return;
        }
        mc.setScreen(new NearlyCraftableScreen(mc.screen));
    }

    @Override
    protected void init() {
        int width = Math.min(320, this.width - 20);
        listLeft = (this.width - width) / 2;
        listRight = listLeft + width;
        listTop = 48;
        listBottom = this.height - 12;

        search = new EditBox(font, listLeft, 24, width, 18, Component.translatable("screen.nearlycraftable.search"));
        search.setHint(Component.translatable("screen.nearlycraftable.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(text -> applyFilter());
        addRenderableWidget(search);

        allRows.clear();
        for (MatchResult result : ResultCache.get(minecraft.player)) {
            allRows.add(toRow(result));
        }
        applyFilter();
    }

    private static ItemStack stackOf(ItemKey key) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(key.id())));
    }

    private static Row toRow(MatchResult result) {
        ItemStack output = stackOf(result.recipe().output());
        output.setCount(Math.max(1, result.recipe().outputCount()));
        List<MissingView> missing = new ArrayList<>();
        for (MatchResult.Missing m : result.missing()) {
            List<ItemStack> choices = new ArrayList<>();
            for (ItemKey key : m.allowed()) {
                choices.add(stackOf(key));
            }
            missing.add(new MissingView(choices, m.amount()));
        }
        return new Row(result, output, output.getHoverName().getString().toLowerCase(Locale.ROOT), missing);
    }

    private void applyFilter() {
        String text = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Row> filtered = new ArrayList<>();
        for (Row row : allRows) {
            if (text.isEmpty() || row.lowerName().contains(text)) {
                filtered.add(row);
            }
        }
        visibleRows = filtered;
        scroll = Math.min(scroll, maxScroll());
    }

    private int viewHeight() {
        return listBottom - listTop;
    }

    private int maxScroll() {
        return Math.max(0, visibleRows.size() * ROW_HEIGHT - viewHeight());
    }

    private ItemStack pick(MissingView view) {
        int index = (int) ((System.currentTimeMillis() / 1000) % view.choices().size());
        return view.choices().get(index);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);

        if (visibleRows.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.nearlycraftable.empty"), width / 2, listTop + 20, 0xAAAAAA);
        }

        g.enableScissor(listLeft, listTop, listRight, listBottom);
        Row hovered = null;
        int first = scroll / ROW_HEIGHT;
        for (int i = first; i < visibleRows.size(); i++) {
            int y = listTop + i * ROW_HEIGHT - scroll;
            if (y >= listBottom) {
                break;
            }
            Row row = visibleRows.get(i);
            boolean isHovered = mouseX >= listLeft && mouseX < listRight - SCROLLBAR_WIDTH - 2
                    && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + ROW_HEIGHT, listBottom);
            g.fill(listLeft, y, listRight - SCROLLBAR_WIDTH - 2, y + ROW_HEIGHT - 2, isHovered ? 0x80FFFFFF : 0x40000000);
            if (isHovered) {
                hovered = row;
            }
            renderRow(g, row, y);
        }
        g.disableScissor();

        renderScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);

        if (hovered != null) {
            g.renderComponentTooltip(font, tooltipFor(hovered), mouseX, mouseY);
        }
    }

    private void renderRow(GuiGraphics g, Row row, int y) {
        int iconX = listLeft + 4;
        int iconY = y + 3;
        g.renderItem(row.output(), iconX, iconY);
        if (row.output().getCount() > 1) {
            g.renderItemDecorations(font, row.output(), iconX, iconY);
        }

        int iconsWidth = Math.min(MAX_ICONS, row.missing().size()) * 20 + (row.missing().size() > MAX_ICONS ? 14 : 0);
        int nameX = iconX + 22;
        int nameWidth = listRight - SCROLLBAR_WIDTH - 6 - iconsWidth - nameX;
        String name = font.plainSubstrByWidth(row.output().getHoverName().getString(), Math.max(10, nameWidth));
        g.drawString(font, name, nameX, y + 8, 0xFFFFFF, false);

        int x = listRight - SCROLLBAR_WIDTH - 6 - iconsWidth;
        int shown = 0;
        for (MissingView view : row.missing()) {
            if (shown++ == MAX_ICONS) {
                g.drawString(font, "+" + (row.missing().size() - MAX_ICONS), x, y + 8, 0xFFAAAA, false);
                break;
            }
            ItemStack stack = pick(view);
            g.renderItem(stack, x, y + 3);
            g.renderItemDecorations(font, stack, x, y + 3, String.valueOf(view.amount()));
            x += 20;
        }
    }

    private List<Component> tooltipFor(Row row) {
        List<Component> lines = new ArrayList<>();
        lines.add(row.output().getHoverName());
        lines.add(Component.translatable("screen.nearlycraftable.missing_header").withStyle(ChatFormatting.RED));
        for (MissingView view : row.missing()) {
            ItemStack first = view.choices().get(0);
            int others = view.choices().size() - 1;
            if (others > 0) {
                lines.add(Component.translatable("screen.nearlycraftable.missing_any", view.amount(), first.getHoverName(), others)
                        .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("screen.nearlycraftable.missing_line", view.amount(), first.getHoverName())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(Component.translatable("screen.nearlycraftable.click_hint").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private void renderScrollbar(GuiGraphics g) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int x = listRight - SCROLLBAR_WIDTH;
        g.fill(x, listTop, x + SCROLLBAR_WIDTH, listBottom, 0x60000000);
        int total = visibleRows.size() * ROW_HEIGHT;
        int barHeight = Math.max(16, viewHeight() * viewHeight() / total);
        int barY = listTop + (int) ((long) (viewHeight() - barHeight) * scroll / max);
        g.fill(x, barY, x + SCROLLBAR_WIDTH, barY + barHeight, 0xFFAAAAAA);
    }

    private void dragScrollbarTo(double mouseY) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        double fraction = (mouseY - listTop) / viewHeight();
        scroll = (int) Math.max(0, Math.min(max, fraction * (max + viewHeight()) - viewHeight() / 2.0));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - delta * ROW_HEIGHT));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || mouseY < listTop || mouseY >= listBottom) {
            return false;
        }
        if (mouseX >= listRight - SCROLLBAR_WIDTH && mouseX < listRight && maxScroll() > 0) {
            draggingScrollbar = true;
            dragScrollbarTo(mouseY);
            return true;
        }
        if (mouseX >= listLeft && mouseX < listRight - SCROLLBAR_WIDTH - 2) {
            int index = ((int) mouseY - listTop + scroll) / ROW_HEIGHT;
            if (index >= 0 && index < visibleRows.size()) {
                Object ref = visibleRows.get(index).result().recipe().ref();
                if (ref instanceof EmiRecipe recipe) {
                    EmiApi.displayRecipe(recipe);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            dragScrollbarTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!search.isFocused() && ClientSetup.OPEN_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
