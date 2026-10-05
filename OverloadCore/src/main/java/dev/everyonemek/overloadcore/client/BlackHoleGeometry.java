package dev.everyonemek.overloadcore.client;

/** Optical image faces the viewer. The shader reconstructs curved surface depth within this bound. */
public final class BlackHoleGeometry {
    @FunctionalInterface public interface Sink {void vertex(float x,float y,float z,float u,float v,float r,float g,float b,float a);}
    private static final int[][] CORNERS={{0,0},{1,0},{1,1},{0,1}};
    private static final java.util.List<float[]> FALLBACK=buildFallback();
    public static float quantizedRadius(float radius){return Math.max(1,Math.round(radius*255/8))/255F*8;}
    public static void billboard(Sink sink,float radius,float phase){
        float size=3*radius;
        sink.vertex(-size,-size,0,0,0,radius/8,phase,0,1);sink.vertex(size,-size,0,1,0,radius/8,phase,0,1);
        sink.vertex(size,size,0,1,1,radius/8,phase,0,1);sink.vertex(-size,size,0,0,1,radius/8,phase,0,1);
    }
    public static void fallback(Sink sink,float radius){
        for(var v:FALLBACK)sink.vertex(v[0]*radius,v[1]*radius,v[2]*radius,0,0,v[3],v[4],v[5],v[6]);
    }
    private static java.util.List<float[]> buildFallback(){
        var vertices=new java.util.ArrayList<float[]>();
        Sink sink=(x,y,z,u,v,r,g,b,a)->vertices.add(new float[]{x,y,z,r,g,b,a});float radius=1;
        // Real opaque curved core; fewer than 350 quads, no textures or shader dependency.
        for(int j=0;j<12;j++)for(int i=0;i<24;i++){
            for(int[] q:CORNERS){
                double lat=-Math.PI/2+(j+q[1])*Math.PI/12,lon=(i+q[0])*Math.PI*2/24;
                sink.vertex((float)(Math.cos(lat)*Math.cos(lon))*radius,(float)Math.sin(lat)*radius,(float)(Math.cos(lat)*Math.sin(lon))*radius,0,0,0,0,0,1);
            }
        }
        for(int i=0;i<48;i++)for(int[] q:CORNERS){
            double a=(i+q[0])*Math.PI*2/48;float ring=(q[1]==0?1.12F:2.5F)*radius;
            sink.vertex((float)Math.cos(a)*ring,(float)Math.sin(a)*ring*.28F-.10F*radius,(float)Math.sin(a)*ring*.82F,0,0,1,.82F,.48F,q[1]==0?.95F:.38F);
        }
        return java.util.List.copyOf(vertices);
    }
    private BlackHoleGeometry(){}
}
