package dev.everyonemek.overloadcore.client;

/** Small local-space meshes shared by the renderer and hidden OpenGL verification. +Z is travel. */
public final class GearEffectGeometry {
    @FunctionalInterface public interface Sink {
        void vertex(float x,float y,float z,float u,float v,int material,float power,float alpha);
    }
    private static void v(Sink s,float x,float y,float z,float u,float v,int mat,float power,float a){s.vertex(x,y,z,u,v,mat,power,a);}
    private static void strip(Sink s,float x0,float y0,float z0,float x1,float y1,float z1,float dx,float dy,float dz,int mat,float power,float a){
        v(s,x0-dx,y0-dy,z0-dz,0,0,mat,power,a);v(s,x1-dx,y1-dy,z1-dz,1,0,mat,power,a);
        v(s,x1+dx,y1+dy,z1+dz,1,1,mat,power,a);v(s,x0+dx,y0+dy,z0+dz,0,1,mat,power,a);
    }
    private static void flash(Sink s,float z,float radius,float alpha){
        // Three planes, so impacts remain visible when viewed from the side.
        strip(s,-radius,0,z,radius,0,z,0,radius,0,6,1,alpha);
        strip(s,0,-radius,z,0,radius,z,0,0,radius,6,1,alpha);
        strip(s,0,0,z-radius,0,0,z+radius,radius,0,0,6,1,alpha);
    }
    public static float flightTime(float length){return Math.max(1.6F,length/24F);}
    public static void muzzle(Sink s,float age){if(age>=0&&age<1.8F)flash(s,0,.11F+.055F*age,.8F*(1-age/1.8F));}
    private static void slug(Sink s,float head){
        float rear=Math.max(0,head-.48F),shoulder=Math.max(rear,head-.13F),r=.075F;
        // Eight metallic facets and a pointed nose, not a pair of luminous beam planes.
        for(int i=0;i<8;i++){
            float a=(float)(i*Math.PI/4),b=(float)((i+1)*Math.PI/4);
            float ax=(float)Math.cos(a)*r,ay=(float)Math.sin(a)*r,bx=(float)Math.cos(b)*r,by=(float)Math.sin(b)*r;
            float shade=.35F+.55F*Math.max(0,(float)Math.cos(a-1));
            v(s,ax,ay,rear,0,0,8,shade,1);v(s,bx,by,rear,0,1,8,shade,1);
            v(s,bx,by,shoulder,1,1,8,shade,1);v(s,ax,ay,shoulder,1,0,8,shade,1);
            v(s,ax,ay,shoulder,0,0,8,shade,1);v(s,bx,by,shoulder,0,1,8,shade,1);
            v(s,0,0,head,1,.5F,8,shade,1);v(s,0,0,head,1,.5F,8,shade,1);
            v(s,bx,by,rear,0,0,8,.4F,1);v(s,ax,ay,rear,0,1,8,.4F,1);
            v(s,0,0,rear,1,.5F,8,.4F,1);v(s,0,0,rear,1,.5F,8,.4F,1);
        }
    }
    public static void shot(Sink s,int kind,float length,float age,boolean impact){
        shot(s,kind,length,age,impact,true);
    }
    public static void shot(Sink s,int kind,float length,float age,boolean impact,boolean showMuzzle){
        if(length<.01F||age<0||age>=15)return;
        if(kind==0){
            float flight=flightTime(length);
            if(age<flight){
                float head=length*Math.min(1,(age+.08F)/flight);
                slug(s,head);
                for(int i=1;i<=3;i++){
                    float z=head-.48F-i*.16F;if(z<0)break;
                    float x=(i%2==0?1:-1)*i*.018F,y=(float)Math.sin(i*3.1F)*.035F;
                    strip(s,x-.045F,y,z,x+.045F,y,z,0,.045F,0,6,1,.50F/i);
                }
            }
            if(showMuzzle)muzzle(s,age);
            float elapsed=age-flight;
            if(impact&&elapsed>=0&&elapsed<4)flash(s,length,.12F+.075F*elapsed,(1-elapsed/4)*.85F);
        }else if(kind==1){
            if(age>=7)return;
            // A crescent in the transverse XY plane; the old XZ fan was edge-on to its shooter.
            float travel=length*Math.min(1,(age+.7F)/5.5F),radius=.65F+.17F*travel;
            for(int i=0;i<24;i++){
                float a=-1.13F+i*2.26F/24,b=-1.13F+(i+1)*2.26F/24;
                float ax=(float)Math.sin(a),ay=(float)Math.cos(a),bx=(float)Math.sin(b),by=(float)Math.cos(b);
                float innerA=radius*(1-.40F*(float)Math.sin(i*Math.PI/24)),innerB=radius*(1-.40F*(float)Math.sin((i+1)*Math.PI/24));
                float alpha=.85F*(1-age/7),back=Math.max(0,travel-.07F);
                crescent(s,ax*innerA,ay*innerA-radius*.60F,back,i/24F,0,alpha*.65F);
                crescent(s,bx*innerB,by*innerB-radius*.60F,back,(i+1)/24F,0,alpha*.65F);
                crescent(s,bx*radius,by*radius-radius*.60F,back,(i+1)/24F,1,alpha*.65F);
                crescent(s,ax*radius,ay*radius-radius*.60F,back,i/24F,1,alpha*.65F);
                crescent(s,ax*radius,ay*radius-radius*.60F,travel,i/24F,1,alpha*.65F);
                crescent(s,bx*radius,by*radius-radius*.60F,travel,(i+1)/24F,1,alpha*.65F);
                crescent(s,bx*innerB,by*innerB-radius*.60F,travel,(i+1)/24F,0,alpha*.65F);
                crescent(s,ax*innerA,ay*innerA-radius*.60F,travel,i/24F,0,alpha*.65F);
                // Outer and inner edges give the slash thickness from observer-side views.
                for(int edge=0;edge<2;edge++){
                    float ra=edge==0?radius:innerA,rb=edge==0?radius:innerB;
                    crescent(s,ax*ra,ay*ra-radius*.60F,back,i/24F,.25F,alpha*.45F);
                    crescent(s,bx*rb,by*rb-radius*.60F,back,(i+1)/24F,.25F,alpha*.45F);
                    crescent(s,bx*rb,by*rb-radius*.60F,travel,(i+1)/24F,.75F,alpha*.45F);
                    crescent(s,ax*ra,ay*ra-radius*.60F,travel,i/24F,.75F,alpha*.45F);
                }
            }
        }else if(kind==2&&age<5){
            for(int i=0;i<8;i++){
                float x0=i==0?0:(float)Math.sin(i*7.4)*.10F,x1=i==7?0:(float)Math.sin((i+1)*7.4)*.10F;
                strip(s,x0,0,length*i/8,x1,0,length*(i+1)/8,.045F,0,0,4,1,.9F*(1-age/5));
                strip(s,x0,0,length*i/8,x1,0,length*(i+1)/8,0,.045F,0,4,1,.9F*(1-age/5));
            }
        }
    }
    private static void crescent(Sink s,float x,float y,float z,float u,float v,float a){
        // Tilt the slash 22 degrees without changing the forward travel axis.
        s.vertex(x*.927F-y*.375F,x*.375F+y*.927F,z,u,v,3,1,a);
    }
    public static void charge(Sink s,boolean rail,float amount){
        if(amount<=0)return;
        if(rail){
            for(int ring=0;ring<2;ring++)for(int i=0;i<24;i++){
                float a=(float)(i*Math.PI/12),b=(float)((i+1)*Math.PI/12);
                float inner=.10F+.045F*amount+ring*.015F,outer=inner+.032F,z=-.79F-ring*.14F;
                float ax=(float)Math.cos(a),ay=(float)Math.sin(a),bx=(float)Math.cos(b),by=(float)Math.sin(b);
                v(s,.5F+ax*inner,7/16F+ay*inner,z,i/24F,0,5,amount,amount*.8F);
                v(s,.5F+bx*inner,7/16F+by*inner,z,(i+1)/24F,0,5,amount,amount*.8F);
                v(s,.5F+bx*outer,7/16F+by*outer,z,(i+1)/24F,1,5,amount,amount*.8F);
                v(s,.5F+ax*outer,7/16F+ay*outer,z,i/24F,1,5,amount,amount*.8F);
            }
        }else for(float z:new float[]{.43F,.57F}){
            strip(s,.585F,.51F,z,.535F,1.70F,z,.08F*amount,0,0,7,amount,amount*.72F);
        }
    }
    private GearEffectGeometry(){}
}
