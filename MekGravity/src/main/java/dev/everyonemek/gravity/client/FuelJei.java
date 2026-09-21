package dev.everyonemek.gravity.client;
import dev.everyonemek.gravity.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
@JeiPlugin
public final class FuelJei implements IModPlugin {
    private static final RecipeType<FuelRecipe> TYPE=RecipeType.create(MekGravity.ID,"matter_fuel",FuelRecipe.class);
    public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath(MekGravity.ID,"jei");}
    public void registerCategories(IRecipeCategoryRegistration r){if(mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())r.addRecipeCategories(new Category(r.getJeiHelpers().getGuiHelper()));}
    public void registerRecipes(IRecipeRegistration r){var level=Minecraft.getInstance().level;if(level!=null&&mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())r.addRecipes(TYPE,level.getRecipeManager().getAllRecipesFor(Content.FUEL_TYPE.get()).stream().map(h->h.value()).toList());}
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){if(mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad()){r.addRecipeCatalyst(new ItemStack(Content.CONTROLLER),TYPE);r.addRecipeCatalyst(new ItemStack(Content.PARTS.get(PartBlock.Kind.FUEL)),TYPE);}}
    private static final class Category implements IRecipeCategory<FuelRecipe> {
        private final IDrawable icon,slot;
        Category(mezz.jei.api.helpers.IGuiHelper h){icon=h.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(Content.CONTROLLER));slot=h.getSlotDrawable();}
        public RecipeType<FuelRecipe> getRecipeType(){return TYPE;}public Component getTitle(){return Content.text("matter_fuel");}public IDrawable getIcon(){return icon;}
        public int getWidth(){return 150;}public int getHeight(){return 46;}
        public void setRecipe(IRecipeLayoutBuilder b,FuelRecipe r,IFocusGroup f){b.addSlot(RecipeIngredientRole.INPUT,4,5).setBackground(slot,-1,-1).addItemStacks(java.util.Arrays.stream(r.ingredient().getItems()).map(s->s.copyWithCount(r.count())).toList());}
        public void draw(FuelRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){g.drawString(Minecraft.getInstance().font,Content.text("fuel_energy",ReactorScreen.fe(r.energy())),27,9,0x444444,false);g.drawString(Minecraft.getInstance().font,Content.text("fuel_budget_hint"),3,32,0x444444,false);}
    }
}
