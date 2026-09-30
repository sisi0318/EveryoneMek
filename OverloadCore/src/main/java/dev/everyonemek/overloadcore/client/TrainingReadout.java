package dev.everyonemek.overloadcore.client;

/** Shared compact values for the native screen and in-world hit feedback. */
public final class TrainingReadout {
    public static String number(double value){return value>=1e12?String.format(java.util.Locale.ROOT,"%.2fT",value/1e12):value>=1e9?String.format(java.util.Locale.ROOT,"%.2fG",value/1e9):value>=1e6?String.format(java.util.Locale.ROOT,"%.2fM",value/1e6):value>=1000?String.format(java.util.Locale.ROOT,"%.1fk",value/1000):String.format(java.util.Locale.ROOT,"%.1f",value);}
    public static String count(int value){return value<1000?Integer.toString(value):number(value);}
    private TrainingReadout(){}
}
