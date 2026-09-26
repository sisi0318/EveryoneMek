package dev.everyonemek.gravity.corona;
/** Fixed geometry budget; no world queries or particle entities. Also used by the GPU verifier. */
public final class CoronalField {
    @FunctionalInterface public interface Vertex{void accept(float x,float y,float z,float angle,float across);}
    public static void emit(boolean reduced,Vertex v){
        int segments=reduced?24:64;
        for(int ring=0;ring<2;ring++)for(int i=0;i<segments;i++)for(int corner=0;corner<4;corner++){
            double angle=(i+(corner==1||corner==2?1:0))*Math.PI*2/segments;float across=corner<2?0:1;
            double radius=(ring==0?.27:.34)+(across-.5)*.055,x=Math.cos(angle)*radius,y=Math.sin(angle)*radius;
            v.accept((float)x,(float)(y*.93),(float)(y*(ring==0?.35:-.35)),(float)(angle+ring*Math.PI/3),across);
        }
    }
    private CoronalField(){}
}
