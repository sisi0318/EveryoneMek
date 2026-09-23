package dev.everyonemek.gravity;

/** Client-owned motion state; analytic smoothing makes the same elapsed time independent of FPS. */
public final class CoreMotion {
    private double lastTick=Double.NaN;
    private double strength;
    private double phase;

    public void update(double tick,double target){
        target=Math.clamp(target,0,1);
        if(Double.isNaN(lastTick)){lastTick=tick;return;}
        double elapsed=tick-lastTick;lastTick=tick;
        if(elapsed<=0)return;
        // Re-entering render range never fast-forwards a long period that was not observed.
        elapsed=Math.min(elapsed,5);
        double decay=Math.exp(-elapsed/8D);
        double integrated=target*elapsed+(strength-target)*8D*(1-decay);
        phase+=integrated;
        strength=target+(strength-target)*decay;
        if(target==0&&strength<.0001)strength=0;
    }
    public float strength(){return (float)strength;}
    public double phase(){return phase;}
}
