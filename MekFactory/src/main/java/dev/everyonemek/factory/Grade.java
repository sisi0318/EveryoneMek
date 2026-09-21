package dev.everyonemek.factory;
import mekanism.api.text.ILangEntry;
public enum Grade implements ILangEntry {
    BASIC("basic",9,512,64000), ADVANCED("advanced",18,2048,256000), ELITE("elite",36,8192,1024000), ULTIMATE("ultimate",54,32768,4096000);
    public final String id;public final int slots,itemCapacity,capacity;
    Grade(String id,int slots,int itemCapacity,int capacity){this.id=id;this.slots=slots;this.itemCapacity=itemCapacity;this.capacity=capacity;}
    public int size(){return FactoryConfig.SIZE[ordinal()].get();}
    public int parallel(){return FactoryConfig.PARALLEL[ordinal()].get();}
    @Override public String getTranslationKey(){return "block.mekfactory."+id+"_controller";}
}
