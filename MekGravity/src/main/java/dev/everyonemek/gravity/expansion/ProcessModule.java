package dev.everyonemek.gravity.expansion;
import java.util.*;
import dev.everyonemek.gravity.corona.CoronalInventorySlot;
import mekanism.api.*;
import mekanism.common.capabilities.holder.slot.*;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.*;
import mekanism.common.tile.component.config.*;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Item IO configuration for the captor, forge and tuner, using Mek's real holder and side packets. */
public final class ProcessModule extends OrbitalModule implements ISideConfiguration {
    private final TileComponentConfig config;private final TileComponentEjector ejector;
    public ProcessModule(BlockPos pos,BlockState state){super(pos,state);config=new TileComponentConfig(this,EnumSet.of(TransmissionType.ITEM));var info=config.getConfig(TransmissionType.ITEM);
        info.addSlotInfo(DataType.INPUT,TileComponentConfig.createInfo(TransmissionType.ITEM,true,false,inputs));
        if(!outputs.isEmpty()){var both=new ArrayList<Object>(inputs);both.addAll(outputs);info.addSlotInfo(DataType.OUTPUT,TileComponentConfig.createInfo(TransmissionType.ITEM,false,true,outputs));info.addSlotInfo(DataType.INPUT_OUTPUT,TileComponentConfig.createInfo(TransmissionType.ITEM,true,true,both));}
        for(var side:RelativeSide.values())info.setDataType(!outputs.isEmpty()&&side==RelativeSide.FRONT?DataType.OUTPUT:DataType.INPUT,side);info.setCanEject(!outputs.isEmpty());info.setEjecting(true);ejector=new TileComponentEjector(this).setCanEject(t->false);
    }
    @Override public TileComponentConfig getConfig(){return config;}
    @Override public TileComponentEjector getEjector(){return ejector;}
    @Override protected IInventorySlotHolder getInitialInventory(IContentsListener listener){inputs=new ArrayList<>();outputs=new ArrayList<>();var h=InventorySlotHelper.forSideWithConfig(this);
        if(kind().processor()){for(int i=0;i<9;i++){var slot=new CoronalInventorySlot(false,listener,12+i%3*18,78+i/3*18);inputs.add(slot);h.addSlot(slot);}for(int i=0;i<9;i++){var slot=new CoronalInventorySlot(true,listener,154+i%3*18,78+i/3*18);outputs.add(slot);h.addSlot(slot);}}
        else{var slot=InputInventorySlot.at(s->s.is(ModuleContent.FLARE.get()),listener,13,120);inputs.add(slot);h.addSlot(slot);}return h.build();
    }
    @Override protected void eject(){var info=config.getConfig(TransmissionType.ITEM);if(!autoEject||!info.isEjecting()||outputs.isEmpty())return;int budget=4096;
        for(var side:RelativeSide.values()){var mode=info.getDataType(side);if(mode!=DataType.OUTPUT&&mode!=DataType.INPUT_OUTPUT)continue;var direction=side.getDirection(getDirection());var at=worldPosition.relative(direction);if(!level.hasChunkAt(at))continue;var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,at,direction.getOpposite());if(handler==null)continue;
            for(var slot:outputs){if(budget<=0)return;if(slot.isEmpty())continue;var offer=slot.getStack().copyWithCount(Math.min(budget,slot.getCount()));int accepted=offer.getCount()-ItemHandlerHelper.insertItemStacked(handler,offer,true).getCount();if(accepted==0)continue;var extracted=slot.extractItem(accepted,Action.EXECUTE,AutomationType.INTERNAL);var left=ItemHandlerHelper.insertItemStacked(handler,extracted,false);budget-=extracted.getCount()-left.getCount();if(!left.isEmpty())slot.insertItem(left,Action.EXECUTE,AutomationType.INTERNAL);}
        }
    }
}
