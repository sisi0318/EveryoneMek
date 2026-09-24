package dev.everyonemek.gravity.solar;

/** Immutable-at-call-site cached unit normals and positions; built once, never per frame. */
public final class SolarSphereMesh {
    public static final float RADIUS=1.25F;
    private static final float[] NEAR=build(12),MEDIUM=build(8),FAR=build(4);
    public static float[] vertices(double distance){return distance<16?NEAR:distance<40?MEDIUM:FAR;}
    private static float[] point(int face,double u,double v){
        double x=0,y=0,z=0;
        switch(face){case 0->{x=1;y=1-2*v;z=1-2*u;}case 1->{x=-1;y=1-2*v;z=2*u-1;}case 2->{x=2*u-1;y=1;z=2*v-1;}case 3->{x=2*u-1;y=-1;z=1-2*v;}case 4->{x=2*u-1;y=1-2*v;z=1;}case 5->{x=1-2*u;y=1-2*v;z=-1;}}
        return new float[]{(float)(x*Math.sqrt(1-y*y/2-z*z/2+y*y*z*z/3)),(float)(y*Math.sqrt(1-z*z/2-x*x/2+z*z*x*x/3)),(float)(z*Math.sqrt(1-x*x/2-y*y/2+x*x*y*y/3))};
    }
    private static float[] build(int steps){var result=new float[6*steps*steps*4*3];int at=0;
        for(int face=0;face<6;face++)for(int y=0;y<steps;y++)for(int x=0;x<steps;x++){
            var points=new float[][]{point(face,x/(double)steps,y/(double)steps),point(face,(x+1D)/steps,y/(double)steps),point(face,(x+1D)/steps,(y+1D)/steps),point(face,x/(double)steps,(y+1D)/steps)};
            var a=points[0];var b=points[1];var c=points[2];float dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],ex=c[0]-a[0],ey=c[1]-a[1],ez=c[2]-a[2];
            boolean reverse=(dy*ez-dz*ey)*a[0]+(dz*ex-dx*ez)*a[1]+(dx*ey-dy*ex)*a[2]<0;
            for(int i=0;i<4;i++)for(float value:points[reverse?3-i:i])result[at++]=value;
        }return result;
    }
    private SolarSphereMesh(){}
}
