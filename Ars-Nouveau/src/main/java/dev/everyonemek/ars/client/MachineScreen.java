package dev.everyonemek.ars.client;

import dev.everyonemek.ars.MachineKind;
import dev.everyonemek.ars.MachineMenu;
import dev.everyonemek.ars.SourceMachine;
import dev.everyonemek.ars.RecipeChoices;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiChemicalBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MachineScreen extends GuiConfigurableTile<SourceMachine, MachineMenu> {
    private List<RecipeChoices.Choice> choices = List.of();
    private int choicesMode = -1;
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 238;
        imageHeight = 264;
        inventoryLabelX = 38;
        inventoryLabelY = 168;
        dynamicSlots = true;
    }

    private static Component text(String key) { return Component.translatable("gui.arsmekanism." + key); }

    private void button(int x, int y, int width, Supplier<Component> label, int action) {
        addRenderableWidget(new MekanismButton(this, x, y, width, 16, label.get(), (button, mx, my) -> {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
            return true;
        }) {
            @Override public void tick() { super.tick(); setMessage(label.get()); }
        });
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.energy(), 224, 22, 72));
        addRenderableWidget(new GuiEnergyTab(this, tile.energy(), tile::getActive));
        if (tile.kind().usesSource()) addRenderableWidget(new GuiChemicalBar(this, GuiChemicalBar.getProvider(tile.sourceTank(), List.of(tile.sourceTank())),
              18, 78, 180, 8, true));
        addRenderableWidget(new GuiInnerScreen(this, 18, 94, 180, 16, () -> List.of(statusText())));
        if (tile.kind().advanced()) { addAdvancedElements(); return; }
        if (tile.kind().processesItems()) {
            ProgressType arrow = ProgressType.SMALL_RIGHT;
            addRenderableWidget(new GuiProgress(tile::progress, arrow, this, (121 + 163 - arrow.getWidth()) / 2, 47 - arrow.getHeight() / 2));
            addRecipeControls();
        } else if (tile.kind() == MachineKind.SOURCE_GENERATOR) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 26, 180, 16,
                  () -> List.of(Component.translatable("gui.arsmekanism.generation_rate", tile.sourceRate()))));
        } else {
            button(18, 26, 180, () -> text("converter_mode." + tile.mode()), 0);
            button(18, 47, 180, () -> Component.translatable("gui.arsmekanism.ars_side",
                  Component.translatable(tile.arsSide().getTranslationKey())), 1);
            addRenderableWidget(new GuiInnerScreen(this, 18, 114, 180, 16,
                  () -> List.of(Component.translatable("gui.arsmekanism.transfer_rate", tile.sourceRate()))));
        }
    }

    private Component quantity(String key, int value) {
        return Component.translatable("gui.arsmekanism." + key, value < 0 ? text("not_connected") : value);
    }

    private Component potionQuantity(String key, int value) {
        return Component.translatable("gui.arsmekanism." + key, value < 0 ? text("not_connected")
              : Component.translatable("gui.arsmekanism.potion_bottles", BigDecimal.valueOf(value, 2).stripTrailingZeros().toPlainString()));
    }

    private Component statusText() {
        if (tile.kind().worldController()) {
            if (tile.status() == SourceMachine.NO_TARGET || tile.status() == SourceMachine.NO_CREATURE || tile.status() == SourceMachine.IDLE)
                return text(tile.kind().id + ".status." + tile.status());
            if (tile.status() == SourceMachine.NO_CONTAINER) return text("controller_missing_jar");
            if (tile.kind() == MachineKind.RITUAL_CONTROLLER && tile.status() == SourceMachine.MISSING_INPUT) return text("insert_tablet");
        }
        return text("status." + tile.status());
    }

    private RecipeChoices.Choice selectedChoice() {
        if (choicesMode != tile.mode()) {
            choices = RecipeChoices.available(tile.getLevel(), tile.kind(), tile.mode()); choicesMode = tile.mode();
        }
        String id = tile.recipeLock().isEmpty() ? tile.selectedRecipe() : tile.recipeLock();
        return choices.stream().filter(c -> c.id().toString().equals(id)).findFirst().orElse(null);
    }

    private void addRecipeControls() {
        boolean glyph = tile.kind() == MachineKind.GLYPH_SCRIBE;
        boolean apparatus = tile.kind() == MachineKind.ENCHANTING_APPARATUS;
        int x = apparatus ? 104 : 18, width = apparatus ? 94 : glyph ? 118 : 180;
        if (apparatus) button(18, 112, 80, () -> text("apparatus_mode." + tile.mode()), 0);
        addRenderableWidget(new MekanismButton(this, x, 112, width, 16, text(glyph ? "choose_glyph" : "choose_recipe"),
              (button, mx, my) -> { addWindow(new GuiRecipeSelector(this, tile, menu.containerId)); return true; }));
        if (glyph) button(142, 112, 56, () -> text("deposit_experience"), 4);
        addRenderableWidget(new GuiInnerScreen(this, 18, 132, 180, 30, () -> {
            var choice = selectedChoice();
            Component state = text(tile.recipeLock().isEmpty() ? glyph ? "no_selection" : "automatic" : "selected_recipe");
            return choice == null ? List.of(state, text(tile.recipeLock().isEmpty() ? "choose_hint" : "choose_again"))
                  : List.of(choice.name(), tile.recipeLock().isEmpty() ? state : GuiRecipeSelector.cost(choice));
        }).tooltip(() -> {
            var choice = selectedChoice();
            return choice == null ? List.of(text("choose_hint")) : GuiRecipeSelector.details(choice);
        }));
    }

    private void addAdvancedElements() {
        if (tile.kind().processesItems()) {
            ProgressType arrow = ProgressType.SMALL_RIGHT;
            addRenderableWidget(new GuiProgress(tile::progress, arrow, this, (121 + 163 - arrow.getWidth()) / 2, 47 - arrow.getHeight() / 2));
        }
        if (tile.kind() == MachineKind.GLYPH_SCRIBE) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 76, 180, 14,
                  () -> List.of(Component.translatable("gui.arsmekanism.experience", tile.experience()))));
        } else if (tile.kind().modes() > 1) {
            String key = tile.kind().creatureController() ? "filter_mode" : tile.kind().id + "_mode";
            int x = tile.kind() == MachineKind.DRYGMY_STATION ? 84 : 18;
            int width = tile.kind() == MachineKind.DRYGMY_STATION ? 114 : tile.kind() == MachineKind.RITUAL_CONTROLLER ? 118 : 180;
            button(x, 112, width, () -> text(key + "." + tile.mode()), 0);
            if (tile.kind() == MachineKind.RITUAL_CONTROLLER) button(142, 112, 56, () -> text("start_ritual"), 7);
        }
        if (tile.kind().recipeSelectable()) {
            addRecipeControls();
        } else if (tile.kind() == MachineKind.SOURCE_EXTRACTOR) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 132, 180, 16, () -> List.of(
                  Component.translatable("gui.arsmekanism.heat", tile.heat(), SourceMachine.HEAT_CAPACITY))));
        } else if (tile.kind() == MachineKind.POTION_MIXER) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 26, 180, 45, () -> List.of(
                  potionQuantity("potion_left", tile.observedPrimary()), potionQuantity("potion_right", tile.observedSecondary()), potionQuantity("potion_top", tile.observedTertiary()))));
            addRenderableWidget(new GuiProgress(tile::progress, ProgressType.LARGE_RIGHT, this, 80, 120));
        } else if (tile.kind() == MachineKind.POTION_BOTTLER) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 132, 180, 16, () -> List.of(potionQuantity("potion_front", tile.observedPrimary()))));
        } else if (tile.kind() == MachineKind.DRYGMY_STATION) {
            addRenderableWidget(new GuiInnerScreen(this, 84, 132, 114, 30, () -> tile.internalDrygmy() ? List.of(
                  Component.translatable("gui.arsmekanism.jar_population", tile.observedSecondary(), tile.observedTertiary()),
                  Component.translatable("gui.arsmekanism.harvest_summary", tile.observedPrimary(),
                        BigDecimal.valueOf(tile.drygmyCycleTicks()).divide(BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString()),
                  quantity("pending_items", tile.drygmyPendingItems())) : List.of(text("external_henge"),
                  quantity("drygmy_bonus", tile.observedPrimary()), tile.observedSecondary() < 0 ? text("not_connected")
                        : Component.translatable("gui.arsmekanism.native_progress", tile.observedSecondary(), tile.observedTertiary()))));
        } else if (tile.kind() == MachineKind.WHIRLISPRIG_STATION) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 132, 180, 30, () -> List.of(
                  quantity("grove_score", tile.observedPrimary()), quantity("grove_diversity", tile.observedSecondary()))));
        } else if (tile.kind() == MachineKind.RITUAL_CONTROLLER) {
            addRenderableWidget(new GuiInnerScreen(this, 18, 132, 180, 16, () -> List.of(text("ritual_state." + Math.max(0, tile.observedPrimary())))));
        }
    }

    @Override
    protected void drawForegroundText(GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        renderInventoryText(graphics);
        if (tile.kind().processesItems()) {
            if (tile.kind().inputCount() > 1) drawText(graphics, text(tile.kind() == MachineKind.DRYGMY_STATION ? "mob_jars" : tile.kind() == MachineKind.IMBUEMENT_CHAMBER ? "catalysts"
                  : tile.kind().worldController() ? "collected" : "materials"), 18, 19);
            if (tile.kind() == MachineKind.GLYPH_SCRIBE || tile.kind().creatureController() || tile.kind() == MachineKind.RITUAL_CONTROLLER)
                drawText(graphics, text(tile.kind() == MachineKind.GLYPH_SCRIBE ? "spell_book"
                      : tile.kind().creatureController() ? "filter" : "tablet"), 99, 19);
            drawText(graphics, text("output"), 164, 19);
            if (tile.kind() == MachineKind.GLYPH_SCRIBE) drawText(graphics, Component.literal("XP"), 209, 49);
            if (tile.kind() == MachineKind.RITUAL_CONTROLLER) drawText(graphics, text("augment"), 124, 63);
            if (tile.kind() == MachineKind.DRYGMY_STATION) drawText(graphics, text("collected"), 18, 113);
        }
        super.drawForegroundText(graphics, mouseX, mouseY);
    }

    private void drawText(GuiGraphics graphics, Component label, int x, int y) {
        graphics.drawString(font, label, x, y, titleTextColor(), false);
    }
}
