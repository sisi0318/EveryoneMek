package dev.everyonemek.overloadcore.client;

/** Small shader meshes: no world searches, particles, or full-screen passes. */
public final class TacticalGeometry {
    private static final float[] TARGET=buildTarget();
    public static void panel(GearEffectGeometry.Sink s,float x,float y,float z,float w,float h,int material,float power,float alpha){
        s.vertex(x-w,y-h,z,0,0,material,power,alpha);s.vertex(x+w,y-h,z,1,0,material,power,alpha);
        s.vertex(x+w,y+h,z,1,1,material,power,alpha);s.vertex(x-w,y+h,z,0,1,material,power,alpha);
    }
    public static void guard(GearEffectGeometry.Sink s,boolean perfect){panel(s,0,0,0,.95F,.95F,10,perfect?1:0,.65F);}
    public static void mark(GearEffectGeometry.Sink s,int stacks){panel(s,0,0,0,.23F,.13F,11,Math.clamp(stacks,1,3)/3F,.85F);}
    public static void target(GearEffectGeometry.Sink s){
        for(int i=0;i<TARGET.length;i+=5)s.vertex(TARGET[i],TARGET[i+1],TARGET[i+2],TARGET[i+3],TARGET[i+4],12,1,.55F);
    }
    private static float[] buildTarget(){var vertices=new java.util.ArrayList<Float>();
        GearEffectGeometry.Sink s=(x,y,z,u,v,m,p,a)->{vertices.add(x);vertices.add(y);vertices.add(z);vertices.add(u);vertices.add(v);};
        // A clearly artificial frame instead of an opaque creature skin.
        box(s,-.20F,.85F,-.12F,.20F,1.42F,.12F);
        box(s,-.18F,1.46F,-.18F,.18F,1.80F,.18F);
        box(s,-.31F,.87F,-.09F,-.22F,1.40F,.09F);box(s,.22F,.87F,-.09F,.31F,1.40F,.09F);
        box(s,-.17F,.10F,-.10F,-.025F,.83F,.10F);box(s,.025F,.10F,-.10F,.17F,.83F,.10F);
        float[] result=new float[vertices.size()];for(int i=0;i<result.length;i++)result[i]=vertices.get(i);return result;
    }
    private static void box(GearEffectGeometry.Sink s,float x,float y,float z,float X,float Y,float Z){
        float[][] p={{x,y,z},{X,y,z},{X,Y,z},{x,Y,z},{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
        int[][] faces={{0,1,2,3},{4,7,6,5},{0,4,5,1},{3,2,6,7},{0,3,7,4},{1,5,6,2}};
        for(var face:faces)for(int i=0;i<4;i++){var v=p[face[i]];s.vertex(v[0],v[1],v[2],i==1||i==2?1:0,i>=2?1:0,12,1,.55F);}
    }
    private TacticalGeometry(){}
}
