package dev.everyonemek.overloadcore.client;

import java.util.ArrayList;

/** Original unit meshes, shared by the renderer and offline GPU checks. Built once, then uploaded once. */
public final class BlackHoleGeometry {
    public static final float DISC_INNER=1.12F, DISC_OUTER=2.68F, LENS_REACH=3F;
    public record Mesh(float[] vertices) {public int quads(){return vertices.length/20;}}
    @FunctionalInterface public interface Sink {void vertex(float x,float y,float z,float u,float v);}
    private static final int[][] CORNERS={{0,0},{1,0},{1,1},{0,1}};
    private static final Mesh[] SPHERES={sphere(12,24),sphere(24,48)};
    private static final Mesh[] DISCS={disc(48),disc(96)};
    public static Mesh sphere(boolean detailed){return SPHERES[detailed?1:0];}
    public static Mesh disc(boolean detailed){return DISCS[detailed?1:0];}
    public static void emit(Mesh mesh,Sink out){var v=mesh.vertices;for(int i=0;i<v.length;i+=5)out.vertex(v[i],v[i+1],v[i+2],v[i+3],v[i+4]);}
    private static Mesh sphere(int rows,int columns){
        var b=new Builder();
        for(int y=0;y<rows;y++)for(int x=0;x<columns;x++)for(var q:CORNERS){
            double latitude=Math.PI*((y+q[1])/(double)rows-.5),longitude=2*Math.PI*(x+q[0])/columns;
            b.add(Math.cos(latitude)*Math.cos(longitude),Math.sin(latitude),Math.cos(latitude)*Math.sin(longitude),0,0);
        }
        return b.finish();
    }
    private static Mesh disc(int sectors){
        // A closed, lenticular cross-section: no overlapping internal glow sheets.
        float[][] profile={{DISC_INNER,0},{1.60F,.095F},{DISC_OUTER,0},{1.60F,-.095F}};
        var b=new Builder();
        for(int i=0;i<sectors;i++)for(int s=0;s<4;s++)for(var q:CORNERS){
            var p=profile[(s+q[1])%4];double angle=(i+q[0])*Math.PI*2/sectors;
            b.add(p[0]*Math.cos(angle),p[1],p[0]*Math.sin(angle),(i+q[0])/(double)sectors,(p[0]-DISC_INNER)/(DISC_OUTER-DISC_INNER));
        }
        return b.finish();
    }
    private static final class Builder {
        private final ArrayList<Float> data=new ArrayList<>();
        void add(double x,double y,double z,double u,double v){for(double n:new double[]{x,y,z,u,v})data.add((float)n);}
        Mesh finish(){float[] result=new float[data.size()];for(int i=0;i<result.length;i++)result[i]=data.get(i);return new Mesh(result);}
    }
    private BlackHoleGeometry(){}
}
