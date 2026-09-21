package dev.everyonemek.gravity;
public enum Grade {
    BASIC,ADVANCED,ELITE,ULTIMATE;
    public String id(){return name().toLowerCase(java.util.Locale.ROOT);}
    public long power(){return ReactorConfig.POWER[ordinal()].get();}
}
