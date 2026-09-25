package dev.everyonemek.gravity;
/** Read-only inventory summary. Saturation is explicit because stellar stacks can exceed long energy. */
public final class FuelStock {
    public int portions;
    public long energy;
    public FuelStock add(int count,long perPortion){
        if(count<=0||perPortion<=0)return this;portions+=count;
        energy=count>(Long.MAX_VALUE-energy)/perPortion?Long.MAX_VALUE:energy+count*perPortion;return this;
    }
}
