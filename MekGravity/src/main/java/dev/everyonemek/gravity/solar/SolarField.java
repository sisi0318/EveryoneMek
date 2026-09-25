package dev.everyonemek.gravity.solar;

/** Client visual geometry only. Shared with the offline preview; no world queries or entities. */
public final class SolarField {
    public static final int MAX_QUADS=800;
    private static final double TAU=Math.PI*2;
    private static final int VIOLET=0xAE9AFA,ICE=0xBAE8FF,GOLD=0xFFC569,WHITE=0xFFF1CD;
    public record Point(double x,double y,double z) {
        Point scale(double n){return new Point(x*n,y*n,z*n);}
        Point rotate(double yaw,double tilt){
            double X=x*Math.cos(tilt)-y*Math.sin(tilt),Y=x*Math.sin(tilt)+y*Math.cos(tilt);
            return new Point(X*Math.cos(yaw)-z*Math.sin(yaw),Y,X*Math.sin(yaw)+z*Math.cos(yaw));
        }
    }
    @FunctionalInterface public interface Ribbons {
        void add(Point a,Point b,double width,int rgb,int alpha,boolean halo);
    }
    private static Point orbit(double angle,double radius,double yaw,double tilt){return new Point(Math.cos(angle)*radius,0,Math.sin(angle)*radius).rotate(yaw,tilt);}
    private static Point meridian(double t,double radius,double azimuth){
        double angle=-Math.PI/2+t*Math.PI,turn=azimuth+.42*Math.sin(angle);
        return new Point(Math.cos(angle)*Math.cos(turn)*radius,Math.sin(angle)*radius,Math.cos(angle)*Math.sin(turn)*radius);
    }
    private static Point stream(double t,int axis,int sign,double phase){
        double radial=1.42+2.08*t,bend=Math.sin(t*Math.PI)*.13;
        return axis==0?new Point(sign*radial,bend*Math.sin(phase),bend*Math.cos(phase)):new Point(bend*Math.cos(phase),bend*Math.sin(phase),sign*radial);
    }
    private static void line(Ribbons out,Point a,Point b,double width,int rgb,double alpha,boolean halo){out.add(a,b,width,rgb,(int)Math.clamp(alpha,0,255),halo);}
    public static void emit(double phase,float strength,Ribbons out){
        emit(phase,strength,.2+.8*strength,out);
    }
    public static void emit(double phase,float strength,double size,Ribbons out){emit(phase,strength,size,false,out);}
    public static void emit(double phase,float strength,double size,boolean reduced,Ribbons out){
        if(strength<=.002F)return;
        double pulse=.86+.14*Math.sin(phase*.075);
        // An open cage outside the photosphere. Each band precesses independently;
        // the bright moving sector makes motion visible even on a sunny background.
        for(int ring=0;ring<(reduced?1:3);ring++){
            double radius=(1.65+ring*.16)*size,yaw=Math.toRadians((phase*(ring==1?-1.6:1.2)+ring*120)%360),tilt=Math.toRadians(22+ring*27);
            for(int i=0;i<48;i+=(reduced?2:1))line(out,orbit(i*TAU/48,radius,yaw,tilt),orbit((i+(reduced?2:1))*TAU/48,radius,yaw,tilt),.015*size,ring==1?ICE:VIOLET,80*strength*pulse,true);
            double head=phase*(ring==1?-.08:.065)+ring*2.1;
            for(int i=0;i<12;i+=(reduced?2:1))line(out,orbit(head+i*.055,radius,yaw,tilt),orbit(head+(i+(reduced?2:1))*.055,radius,yaw,tilt),.032*size,ring==1?ICE:0xE4D6FF,(45+i*15)*strength,true);
        }
        // Curved polar field lines and travelling charges, with gaps instead of an opaque bubble.
        for(int arc=0;arc<(reduced?0:6);arc++){
            double yaw=arc*TAU/6+phase*.008,radius=1.84*size;
            for(int i=0;i<18;i++)line(out,meridian(i/18D,radius,yaw),meridian((i+1)/18D,radius,yaw),.009*size,VIOLET,43*strength,false);
            double t=(phase/85+arc/6D)%1;
            line(out,meridian(t,radius,yaw),meridian(Math.min(1,t+.035),radius,yaw),.027*size,ICE,205*strength,true);
        }
        // The two real focus noses sit 2.25 blocks above/below the seed center.
        for(int sign:new int[]{-1,1}){
            double surface=1.23*size;
            line(out,new Point(0,sign*surface,0),new Point(0,sign*2.25,0),.025,GOLD,125*strength,true);
            for(int i=0;i<(reduced?2:3);i++){
                double t=(phase/28+i/3D)%1,y=sign*(2.20-(2.20-surface)*t);
                line(out,new Point(0,y,0),new Point(0,y-sign*.085,0),.048,WHITE,235*strength,true);
            }
        }
        // Four warm prominences remain on the surface, visually distinct from the cold field.
        for(int plume=0;plume<(reduced?2:4);plume++){
            double yaw=Math.toRadians((plume*90+phase*.9)%360),tilt=Math.toRadians(20+plume*31);
            for(int i=0;i<16;i+=(reduced?2:1)){
                double a=i/16D,b=(i+(reduced?2:1))/16D,ra=(1.23+.30*Math.sin(a*Math.PI)*(.8+.2*Math.sin(phase*.045+plume)))*size,rb=(1.23+.30*Math.sin(b*Math.PI)*(.8+.2*Math.sin(phase*.045+plume)))*size;
                line(out,new Point(Math.cos(-.4+a*.8)*ra,Math.sin(-.4+a*.8)*ra,0).rotate(yaw,tilt),new Point(Math.cos(-.4+b*.8)*rb,Math.sin(-.4+b*.8)*rb,0).rotate(yaw,tilt),.022*size,GOLD,180*strength,false);
            }
        }
        // Harvest streams end before the collector surfaces and pass through their viewing apertures.
        for(int axis:new int[]{0,2})for(int sign:new int[]{-1,1}){
            double turn=phase*.045+axis+sign;
            for(int i=0;i<12;i+=(reduced?3:1))line(out,stream(i/12D,axis,sign,turn),stream((i+(reduced?3:1))/12D,axis,sign,turn),.01,GOLD,40*strength,false);
            for(int i=0;i<(reduced?2:4);i++){
                double t=(phase/48+i*.25)%1;
                line(out,stream(t,axis,sign,turn),stream(Math.min(1,t+.045),axis,sign,turn),.033,WHITE,195*strength,true);
            }
        }
    }
    private SolarField(){}
}
