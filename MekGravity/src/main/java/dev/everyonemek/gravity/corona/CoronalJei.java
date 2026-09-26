package dev.everyonemek.gravity.corona;
import mezz.jei.api.*;
import mezz.jei.api.constants.*;
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
public final class CoronalJei implements IModPlugin{
    private static final RecipeType<CoronalRecipe> TYPE=RecipeType.create("mekgravity","coronal_processing",CoronalRecipe.class);
    public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("mekgravity","coronal_jei");}
    public void registerCategories(IRecipeCategoryRegistration r){if(mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())r.addRecipeCategories(new Category(r.getJeiHelpers().getGuiHelper()));}
    public void registerRecipes(IRecipeRegistration r){var level=Minecraft.getInstance().level;if(level!=null&&mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())r.addRecipes(TYPE,level.getRecipeManager().getAllRecipesFor(CoronalContent.TYPE.get()).stream().map(h->h.value()).toList());}
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){if(mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())r.addRecipeCatalyst(new ItemStack(CoronalContent.BLOCK),TYPE,RecipeTypes.SMELTING,RecipeTypes.BLASTING);}
    private static final class Category implements IRecipeCategory<CoronalRecipe>{
        private final IDrawable icon,slot;
        Category(mezz.jei.api.helpers.IGuiHelper h){icon=h.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(CoronalContent.BLOCK));slot=h.getSlotDrawable();}
        public RecipeType<CoronalRecipe> getRecipeType(){return TYPE;}public Component getTitle(){return CoronalContent.text("category");}public IDrawable getIcon(){return icon;}public int getWidth(){return 190;}public int getHeight(){return 54;}
        public void setRecipe(IRecipeLayoutBuilder b,CoronalRecipe recipe,IFocusGroup focus){for(int i=0;i<recipe.inputs().size();i++){var input=recipe.inputs().get(i);b.addSlot(RecipeIngredientRole.INPUT,4+i*21,5).setBackground(slot,-1,-1).addItemStacks(java.util.Arrays.stream(input.ingredient().getItems()).map(s->s.copyWithCount(input.count())).toList());}b.addSlot(RecipeIngredientRole.OUTPUT,159,5).setBackground(slot,-1,-1).addItemStack(recipe.result());}
        public void draw(CoronalRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double x,double y){g.drawString(Minecraft.getInstance().font,Component.literal("→"),123,9,0x555555,false);g.drawString(Minecraft.getInstance().font,CoronalContent.text("recipe_info",recipe.tier()+1,recipe.ticks(),dev.everyonemek.gravity.client.ReactorScreen.fe(recipe.energy())),3,34,0x444444,false);}
    }
}
