package dev.everyonemek.oritech;

import java.util.Locale;

/** Presentation of finalized addon factors, not the possibly-negative raw addon settings. */
public final class AddonReadout {
    public static String multiplier(float factor,boolean inverse){
        if(!Float.isFinite(factor)||factor<=0)return "—";
        double value=inverse?1.0/factor:factor;
        if(value>=.01&&value<1000)return String.format(Locale.ROOT,"%.2f",value);
        return String.format(Locale.ROOT,"%.2g",value).replace("e+0","e").replace("e-0","e-").replace("e+","e");
    }
    private AddonReadout(){}
}
