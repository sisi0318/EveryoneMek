package dev.everyonemek.botania;

import io.netty.buffer.Unpooled;
import mekanism.api.Action;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.common.block.BotaniaBlocks;
import static dev.everyonemek.botania.BotanicalGameTests.*;
import static dev.everyonemek.botania.ManaMachineGameTests.*;

/** Test-only extension registers during mod setup, exactly like an external integration. */
@EventBusSubscriber(modid = BotanicalMekanism.ID)
@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class GreenhouseRuleGameTests {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("botanicalmekanism_test", "long_named_registered_water_rule");
    private static final GreenhouseFlowerRule RULE = new GreenhouseFlowerRule() {
        @Override public int fluidAmount() { return 250; }
        @Override public boolean acceptsFluid(FluidStack stack) { return !stack.isEmpty() && stack.is(Fluids.WATER); }
        @Override public GreenhouseNative.Result resolve(ItemStack flower, ItemStack item, FluidStack fluid, Level level) {
            return acceptsFluid(fluid) ? new GreenhouseNative.Result(777, 5, 0, flower.copyWithCount(1), 0) : null;
        }
        @Override public ItemStack prepare(ItemStack flower, Level level) { CustomData.update(DataComponents.CUSTOM_DATA, flower, tag -> tag.putBoolean("prepared", true)); return flower; }
        @Override public ItemStack onBlocked(ItemStack flower, Level level) { CustomData.update(DataComponents.CUSTOM_DATA, flower, tag -> tag.putBoolean("blocked", true)); return flower; }
        @Override public Component statusInfo(ItemStack flower, Level level) { return Component.literal("Extension state"); }
        @Override public Component recipeNote() { return Component.literal("Extension recipe"); }
        @Override public boolean variableOutput() { return true; }
    };
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) { event.enqueueWork(() -> GreenhouseRules.register(ID, RULE)); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void externalRuleLoadsRecipeSyncsAndRunsThroughSharedFluidAndStateHooks(GameTestHelper h) {
        var recipe = GreenhouseWork.recipes(h.getLevel()).stream().filter(r -> r.id().getPath().equals("greenhouse_extension_test")).findFirst().orElseThrow().value();
        check(recipe.rule() == RULE && GreenhouseRules.get("endoflame") == GreenhouseRules.get("botanicalmekanism:endoflame"), "Namespaced rules or old aliases failed");
        boolean duplicateRejected = false;
        try { GreenhouseRules.register(ID, RULE); } catch (IllegalArgumentException expected) { duplicateRejected = true; }
        check(duplicateRejected && GreenhouseRules.get(ID.toString()) == RULE, "Duplicate registration replaced a rule");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var codec = new GreenhouseRecipe.Serializer().streamCodec(); codec.encode(buffer, recipe);
            var synced = codec.decode(buffer);
            check(synced.formula().equals(ID.toString()) && synced.rule() == RULE && synced.rule().variableOutput()
                  && !synced.rule().recipeNote().getString().isEmpty(), "Recipe sync or generic JEI metadata lost a custom rule");
        } finally { buffer.release(); }
        var tile = machine(h, new BlockPos(20, 3, 20), ManaMachineKind.GREENHOUSE); power(tile);
        tile.extras.getFirst().setStack(new ItemStack(BotaniaBlocks.HYDROANGEAS)); mana(tile, ManaMachine.MANA_CAPACITY);
        var fluid = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tile.getBlockPos(), Direction.NORTH);
        check(fluid != null && fluid.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE) == 250, "Custom rule did not reach the real fluid input");
        long energy = tile.energy().getEnergy();
        h.startSequence().thenIdle(8).thenExecute(() -> {
            var state = tile.extras.getFirst().getStack().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            check(state.getBoolean("prepared") && state.getBoolean("blocked") && tile.energy().getEnergy() == energy
                  && tile.greenhouseFluid().getFluidAmount() == 250 && GreenhouseWork.flowerInfo(tile).getString().equals("Extension state"), "Rule lifecycle, UI information or full-storage handling failed");
            var manaPort = h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), tile.getBlockPos(), Direction.NORTH);
            check(manaPort.extractChemical(Long.MAX_VALUE, Action.EXECUTE).getAmount() == ManaMachine.MANA_CAPACITY, "Could not clear the test mana buffer");
        }).thenWaitUntil(() -> check(tile.greenhouseFluid().isEmpty(), "Custom fluid rule did not finish"))
              .thenExecute(() -> {
                  stop(tile); check(tile.mana().getStored() == 777 && tile.extras.getFirst().getCount() == 1
                        && tile.energy().getEnergy() == energy - tile.energy().getEnergyPerTick() * 5, "Shared executor lost resources for a registered extension");
              }).thenSucceed();
    }
}
