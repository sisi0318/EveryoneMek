package dev.everyonemek.overloadcore.gear;

import java.util.function.Consumer;
import com.mojang.serialization.Codec;
import dev.everyonemek.overloadcore.CoreContent;
import dev.everyonemek.overloadcore.OverloadCore;
import mekanism.api.gear.*;
import mekanism.api.radial.*;
import mekanism.api.radial.mode.*;
import mekanism.api.text.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Uses Mek's immutable module config, native tweaker, scroll selection and radial menu. */
public record CombatModule(Form form) implements ICustomModule<CombatModule> {
    public static final ResourceLocation FORM=ResourceLocation.fromNamespaceAndPath(OverloadCore.ID,"combat_mode");
    public static final net.neoforged.neoforge.common.util.Lazy<RadialData<Form>> RADIAL=
        net.neoforged.neoforge.common.util.Lazy.of(()->IRadialDataHelper.INSTANCE.dataForEnum(FORM,Form.MELEE));
    public CombatModule(IModule<CombatModule> module){this(module.<Form>getConfigOrThrow(FORM).get());}
    @Override public void addRadialModes(IModule<CombatModule> module,ItemStack stack,Consumer<NestedRadialMode> adder){
        adder.accept(new NestedRadialMode(RADIAL.get(),CoreContent.text("combat.form"),Form.MELEE.icon(),EnumColor.BRIGHT_GREEN));
    }
    @Override @SuppressWarnings("unchecked") public <M extends IRadialMode> M getMode(IModule<CombatModule> module,ItemStack stack,RadialData<M> data){
        return data==RADIAL.get()?(M)form:null;
    }
    @Override public <M extends IRadialMode> boolean setMode(IModule<CombatModule> module,Player player,IModuleContainer container,ItemStack stack,RadialData<M> data,M mode){
        if(data!=RADIAL.get()||!(mode instanceof Form next))return false;
        change(module,player,container,stack,next);return true;
    }
    @Override public Component getModeScrollComponent(IModule<CombatModule> module,ItemStack stack){return form.getTextComponent();}
    @Override public void changeMode(IModule<CombatModule> module,Player player,IModuleContainer container,ItemStack stack,int shift,boolean message){
        Form next=Form.values()[Math.floorMod(form.ordinal()+shift,2)];change(module,player,container,stack,next);
        if(message)module.displayModeChange(player,CoreContent.text("combat.form"),next);
    }
    private void change(IModule<CombatModule> module,Player player,IModuleContainer container,ItemStack stack,Form next){
        if(next!=form){MekaCombat.cancel(player);container.replaceModuleConfig(player.registryAccess(),stack,module.getDataHolder(),module.<Form>getConfigOrThrow(FORM).with(next));}
    }
    @Override public void addHUDStrings(IModule<CombatModule> module,IModuleContainer container,ItemStack stack,Player player,Consumer<Component> adder){
        adder.accept(form.getTextComponent());if(form==Form.RANGED)adder.accept(CoreContent.text("weapon.magazine",GearCombat.ammo(stack),GearCombat.magazine(stack)));
    }
    public enum Form implements IRadialMode,IHasTextComponent,StringRepresentable {
        MELEE("melee","blasting_low",EnumColor.BRIGHT_GREEN),RANGED("ranged","blasting_high",EnumColor.AQUA);
        public static final Codec<Form> CODEC=StringRepresentable.fromEnum(Form::values);
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf,Form> STREAM_CODEC=
            net.minecraft.network.codec.ByteBufCodecs.idMapper(i->values()[Math.floorMod(i,2)],Form::ordinal);
        private final String name,icon;private final EnumColor color;
        Form(String name,String icon,EnumColor color){this.name=name;this.icon=icon;this.color=color;}
        @Override public String getSerializedName(){return name;}
        @Override public Component getTextComponent(){return CoreContent.text("combat."+name);}
        @Override public Component sliceName(){return getTextComponent();}
        @Override public ResourceLocation icon(){return ResourceLocation.fromNamespaceAndPath("mekanism","gui/radial/"+icon+".png");}
        @Override public EnumColor color(){return color;}
    }
}
