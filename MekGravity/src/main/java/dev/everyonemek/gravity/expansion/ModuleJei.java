package dev.everyonemek.gravity.expansion;
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
public final class ModuleJei implements IModPlugin{
    private static RecipeType<OrbitalRecipe> type(ModuleKind kind){return RecipeType.create("mekgravity",kind.id,OrbitalRecipe.class);}
    public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("mekgravity","orbital_jei");}
    public void registerCategories(IRecipeCategoryRegistration r){if(mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())for(var k:ModuleKind.values())if(k.processor())r.addRecipeCategories(new Category(k,r.getJeiHelpers().getGuiHelper()));}
    public void registerRecipes(IRecipeRegistration r){var level=Minecraft.getInstance().level;if(level!=null&&mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())for(var k:ModuleKind.values())if(k.processor())r.addRecipes(type(k),level.getRecipeManager().getAllRecipesFor(ModuleContent.TYPE.get()).stream().map(h->h.value()).filter(recipe->recipe.machine().equals(k.id)).toList());}
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r){if(mekanism.client.recipe_viewer.jei.MekanismJEI.shouldLoad())for(var k:ModuleKind.values())if(k.processor())r.addRecipeCatalyst(new ItemStack(ModuleContent.BLOCK.get(k)),type(k));}
    private static final class Category implements IRecipeCategory<OrbitalRecipe>{
        private final ModuleKind kind;private final IDrawable icon,slot;
        Category(ModuleKind k,mezz.jei.api.helpers.IGuiHelper h){kind=k;icon=h.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(ModuleContent.BLOCK.get(k)));slot=h.getSlotDrawable();}
        public RecipeType<OrbitalRecipe> getRecipeType(){return type(kind);}public Component getTitle(){return Component.translatable("block.mekgravity."+kind.id);}public IDrawable getIcon(){return icon;}public int getWidth(){return 190;}public int getHeight(){return 54;}
        public void setRecipe(IRecipeLayoutBuilder b,OrbitalRecipe recipe,IFocusGroup focus){for(int i=0;i<recipe.inputs().size();i++){var input=recipe.inputs().get(i);b.addSlot(RecipeIngredientRole.INPUT,4+i*21,5).setBackground(slot,-1,-1).addItemStacks(java.util.Arrays.stream(input.ingredient().getItems()).map(s->s.copyWithCount(input.count())).toList());}b.addSlot(RecipeIngredientRole.OUTPUT,159,5).setBackground(slot,-1,-1).addItemStack(recipe.result());}
        public void draw(OrbitalRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){g.drawString(Minecraft.getInstance().font,Component.literal("→"),123,9,0x555555,false);g.drawString(Minecraft.getInstance().font,ModuleContent.text("recipe_info",r.tier()+1,(r.ticks()+(1<<r.tier())-1)/(1<<r.tier()),dev.everyonemek.gravity.client.ReactorScreen.fe(r.energy())),3,34,0x444444,false);}
    }
}
