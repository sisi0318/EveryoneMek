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
    public static void shot(Sink s,int kind,float length,float age,boolean impact){
        if(length<.01F||age<0||age>=15)return;
        if(kind==0){
            float flight=length/24F;
            if(age<flight){
                float head=Math.min(length,(age+.04F)*24),tail=Math.max(0,head-1.6F);
                strip(s,0,0,tail,0,0,head,.055F,0,0,2,1,1);
                strip(s,0,0,tail,0,0,head,0,.055F,0,2,1,1);
            }
            if(age<1.2F)flash(s,0,.13F+.08F*age,1-age/1.2F);
            float elapsed=age-flight;
            if(impact&&elapsed>=0&&elapsed<4)flash(s,length,.12F+.075F*elapsed,(1-elapsed/4)*.85F);
        }else if(kind==1){
            if(age>=7)return;
            float radius=Math.min(length,.55F+length*age/7),inner=Math.max(0,radius-.24F);
            for(int i=0;i<24;i++){
                float a=-1.13F+i*2.26F/24,b=-1.13F+(i+1)*2.26F/24;
                float ax=(float)Math.sin(a),az=(float)Math.cos(a),bx=(float)Math.sin(b),bz=(float)Math.cos(b);
                float alpha=.85F*(1-age/7);
                v(s,ax*inner,ax*inner*.22F,az*inner,i/24F,0,3,1,alpha);
                v(s,bx*inner,bx*inner*.22F,bz*inner,(i+1)/24F,0,3,1,alpha);
                v(s,bx*radius,bx*radius*.22F,bz*radius,(i+1)/24F,1,3,1,alpha);
                v(s,ax*radius,ax*radius*.22F,az*radius,i/24F,1,3,1,alpha);
            }
        }else if(kind==2&&age<5){
            for(int i=0;i<8;i++){
                float x0=i==0?0:(float)Math.sin(i*7.4)*.10F,x1=i==7?0:(float)Math.sin((i+1)*7.4)*.10F;
                strip(s,x0,0,length*i/8,x1,0,length*(i+1)/8,.045F,0,0,4,1,.9F*(1-age/5));
                strip(s,x0,0,length*i/8,x1,0,length*(i+1)/8,0,.045F,0,4,1,.9F*(1-age/5));
            }
        }
    }
    public static void charge(Sink s,boolean rail,float amount){
        if(amount<=0)return;
        if(rail){
            for(int ring=0;ring<2;ring++)for(int i=0;i<24;i++){
                float a=(float)(i*Math.PI/12),b=(float)((i+1)*Math.PI/12);
                float inner=.10F+.045F*amount+ring*.015F,outer=inner+.022F,z=-.645F+ring*.20F;
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
