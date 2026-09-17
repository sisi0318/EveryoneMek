package dev.everyonemek.factory;
import mekanism.api.text.ILangEntry;
public enum Grade implements ILangEntry {
    BASIC("basic",9,16000), ADVANCED("advanced",18,64000), ELITE("elite",36,256000), ULTIMATE("ultimate",54,1024000);
    public final String id;public final int slots,capacity;
    Grade(String id,int slots,int capacity){this.id=id;this.slots=slots;this.capacity=capacity;}
    public int size(){return FactoryConfig.SIZE[ordinal()].get();}
    public int parallel(){return FactoryConfig.PARALLEL[ordinal()].get();}
    @Override public String getTranslationKey(){return "block.mekfactory."+id+"_controller";}
}
