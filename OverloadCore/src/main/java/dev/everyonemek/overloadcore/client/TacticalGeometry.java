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
        target(s,0,1);
    }
    public static void target(GearEffectGeometry.Sink s,float hit,float appear){
        for(int i=0;i<TARGET.length;i+=7)s.vertex(TARGET[i],TARGET[i+1],TARGET[i+2],TARGET[i+3],TARGET[i+4],(int)TARGET[i+5],hit,TARGET[i+6]*appear);
    }
    private static float[] buildTarget(){var vertices=new java.util.ArrayList<Float>();
        GearEffectGeometry.Sink s=(x,y,z,u,v,m,p,a)->{vertices.add(x);vertices.add(y);vertices.add(z);vertices.add(u);vertices.add(v);vertices.add((float)m);vertices.add(a);};
        // Segmented silhouette, chest reticles and a projection source. Cached once, no frame allocations.
        box(s,-.20F,.85F,-.12F,.20F,1.42F,.12F);
        box(s,-.18F,1.46F,-.18F,.18F,1.80F,.18F);
        box(s,-.18F,.77F,-.11F,.18F,.83F,.11F);
        for(int side=-1;side<=1;side+=2){float x=side*.267F;
            box(s,x-.043F,1.15F,-.09F,x+.043F,1.39F,.09F);
            box(s,x-.043F,.88F,-.09F,x+.043F,1.12F,.09F);
        }
        reticle(s,-.126F);
        box(s,-.17F,.10F,-.10F,-.025F,.83F,.10F);box(s,.025F,.10F,-.10F,.17F,.83F,.10F);
        for(int i=0;i<16;i++){
            float a=(float)((i+.08)*Math.PI/8),b=(float)((i+.92)*Math.PI/8);
            for(int j=0;j<4;j++){float angle=j==1||j==2?b:a,r=j>=2?.355F:.325F;
                s.vertex((float)Math.cos(angle)*r,-.45F,(float)Math.sin(angle)*r,j==1||j==2?1:0,j>=2?1:0,13,0,.7F);
            }
        }
        for(int side=-1;side<=1;side+=2)for(int edge=-1;edge<=1;edge+=2){
            s.vertex(side*.17F-.008F,-.43F,edge*.12F,0,0,13,0,.35F);s.vertex(side*.17F+.008F,-.43F,edge*.12F,1,0,13,0,.35F);
            s.vertex(side*.10F+.008F,.10F,edge*.08F,1,1,13,0,.10F);s.vertex(side*.10F-.008F,.10F,edge*.08F,0,1,13,0,.10F);
        }
        float[] result=new float[vertices.size()];for(int i=0;i<result.length;i++)result[i]=vertices.get(i);return result;
    }
    private static void reticle(GearEffectGeometry.Sink s,float z){
        for(int i=0;i<12;i++){
            float a=(float)(i*Math.PI/6),b=(float)((i+1)*Math.PI/6);
            for(int j=0;j<4;j++){float angle=j==1||j==2?b:a,r=j>=2?.13F:.116F;
                s.vertex((float)Math.cos(angle)*r,1.15F+(float)Math.sin(angle)*r,z,j==1||j==2?1:0,j>=2?1:0,13,0,.9F);
            }
        }
        panel(s,0,1.15F,z,.022F,.022F,13,0,.95F);
    }
    private static void box(GearEffectGeometry.Sink s,float x,float y,float z,float X,float Y,float Z){
        float[][] p={{x,y,z},{X,y,z},{X,Y,z},{x,Y,z},{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
        int[][] faces={{0,1,2,3},{4,7,6,5},{0,4,5,1},{3,2,6,7},{0,3,7,4},{1,5,6,2}};
        for(var face:faces)for(int i=0;i<4;i++){var v=p[face[i]];s.vertex(v[0],v[1],v[2],i==1||i==2?1:0,i>=2?1:0,12,0,.64F);}
    }
    private TacticalGeometry(){}
}
