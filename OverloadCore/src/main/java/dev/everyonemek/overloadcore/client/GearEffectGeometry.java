package dev.everyonemek.overloadcore.client;

/** Small local-space meshes shared by the renderer and hidden OpenGL verification. +Z is travel. */
public final class GearEffectGeometry {
    @FunctionalInterface public interface Sink {
        void vertex(float x,float y,float z,float u,float v,int material,float power,float alpha);
    }
    /** Opaque pulse, alpha-blended fields, then small additive sparks. Shared with the GPU check. */
    public static int renderPass(int material){return material==8?0:material==4||material==6?2:1;}
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
    private static void ring(Sink s,float z,float radius,float width,float alpha,int segments){
        for(int i=0;i<segments;i++){
            float a=(float)((i+.12)*Math.PI*2/segments),b=(float)((i+.88)*Math.PI*2/segments);
            float ax=(float)Math.cos(a),ay=(float)Math.sin(a),bx=(float)Math.cos(b),by=(float)Math.sin(b);
            v(s,ax*radius,ay*radius,z,i/(float)segments,0,5,1,alpha);v(s,bx*radius,by*radius,z,(i+1)/(float)segments,0,5,1,alpha);
            v(s,bx*(radius+width),by*(radius+width),z,(i+1)/(float)segments,1,5,1,alpha);v(s,ax*(radius+width),ay*(radius+width),z,i/(float)segments,1,5,1,alpha);
        }
    }
    public static void muzzle(Sink s,float age){
        if(age<0||age>=1.8F)return;
        float fade=1-age/1.8F;ring(s,.025F,.07F+age*.09F,.018F,fade*.6F,12);
        flash(s,0,.085F,fade*.3F);
    }
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
                ring(s,Math.max(0,head-.16F),.078F,.018F,.65F,12);
                for(int i=1;i<=3;i++){
                    float z=head-.48F-i*.16F;if(z<0)break;
                    float x=(i%2==0?1:-1)*i*.018F,y=(float)Math.sin(i*3.1F)*.035F;
                    strip(s,x-.045F,y,z,x+.045F,y,z,0,.045F,0,6,1,.50F/i);
                }
            }
            if(showMuzzle)muzzle(s,age);
            float elapsed=age-flight;
            if(impact&&elapsed>=0&&elapsed<4){
                float fade=1-elapsed/4;flash(s,length,.10F+.04F*elapsed,fade*.55F);
                ring(s,Math.max(0,length-.02F),.08F+elapsed*.10F,.025F,fade*.50F,16);
            }
        }else if(kind==1){
            if(age>=7)return;
            float start=Math.min(length,.9F),travel=start+(length-start)*Math.min(1,age/5.5F),radius=.42F+.13F*travel;
            float fade=(1-age/7),roll=.35F-.50F*Math.min(1,age/5),cs=(float)Math.cos(roll),sn=(float)Math.sin(roll);
            // A narrow cutting edge and one offset wake. No overlapping front/back luminous panel.
            for(int layer=1;layer>=0;layer--){
                float r=radius*(layer==0?1:.93F),z=Math.max(0,travel-layer*.18F);
                float thickness=layer==0?.13F:.05F,alpha=fade*(layer==0?.78F:.24F);
                for(int i=0;i<24;i++){
                    float u=i/24F,U=(i+1)/24F,a=-1.2F+u*2.4F,b=-1.2F+U*2.4F;
                    float ax=(float)Math.sin(a),ay=(float)Math.cos(a),bx=(float)Math.sin(b),by=(float)Math.cos(b);
                    float ra=r*(1-thickness*(float)Math.sin(u*Math.PI)),rb=r*(1-thickness*(float)Math.sin(U*Math.PI));
                    arc(s,ax*ra,ay*ra-r*.65F,z,u,0,alpha,cs,sn);arc(s,bx*rb,by*rb-r*.65F,z,U,0,alpha,cs,sn);
                    arc(s,bx*r,by*r-r*.65F,z,U,1,alpha,cs,sn);arc(s,ax*r,ay*r-r*.65F,z,u,1,alpha,cs,sn);
                    if(layer==0){
                        arc(s,ax*r,ay*r-r*.65F,z,u,.25F,alpha*.35F,cs,sn);arc(s,bx*r,by*r-r*.65F,z,U,.25F,alpha*.35F,cs,sn);
                        arc(s,bx*r,by*r-r*.65F,Math.max(0,z-.045F),U,.75F,alpha*.35F,cs,sn);arc(s,ax*r,ay*r-r*.65F,Math.max(0,z-.045F),u,.75F,alpha*.35F,cs,sn);
                    }
                }
            }
            for(int i=0;i<4;i++){
                float a=-.85F+i*.53F,x=radius*(float)Math.sin(a),y=radius*((float)Math.cos(a)-.65F);
                float px=x*cs-y*sn,py=x*sn+y*cs;
                strip(s,px-.04F,py,Math.max(0,travel-.10F),px+.035F,py+.045F,travel,.012F,.012F,0,4,1,fade*.4F);
            }
        }else if(kind==2&&age<5){
            for(int i=0;i<8;i++){
                float x0=i==0?0:(float)Math.sin(i*7.4)*.10F,x1=i==7?0:(float)Math.sin((i+1)*7.4)*.10F;
                strip(s,x0,0,length*i/8,x1,0,length*(i+1)/8,.045F,0,0,4,1,.9F*(1-age/5));
                strip(s,x0,0,length*i/8,x1,0,length*(i+1)/8,0,.045F,0,4,1,.9F*(1-age/5));
            }
        }
    }
    private static void arc(Sink s,float x,float y,float z,float u,float v,float a,float cs,float sn){
        s.vertex(x*cs-y*sn,x*sn+y*cs,z,u,v,3,1,a);
    }
    public static void toolForm(Sink s,boolean rail,float power,float charge){
        if(power<=0)return;
        if(rail){
            float radius=.125F-.035F*charge,alpha=.30F+.40F*charge;
            ring(s,.025F,radius,.022F,alpha,16);ring(s,.14F,radius*.8F,.015F,alpha*.75F,12);
        }else{
            // Curved contact edge bridging the head's prongs, rather than projecting long flat laser fins.
            float radius=.17F,alpha=.60F+.25F*charge;
            for(int i=0;i<16;i++){
                float u=i/16F,U=(i+1)/16F,a=-1.25F+u*2.5F,b=-1.25F+U*2.5F;
                float ax=(float)Math.sin(a),ay=(float)Math.cos(a),bx=(float)Math.sin(b),by=(float)Math.cos(b);
                float z=.08F+.14F*charge,inner=radius-.045F;
                v(s,ax*inner,ay*inner*.8F-.03F,z+ay*.045F,u,0,9,power,alpha);
                v(s,bx*inner,by*inner*.8F-.03F,z+by*.045F,U,0,9,power,alpha);
                v(s,bx*radius,by*radius*.8F-.03F,z+by*.045F,U,1,9,power,alpha);
                v(s,ax*radius,ay*radius*.8F-.03F,z+ay*.045F,u,1,9,power,alpha);
            }
        }
    }
    private GearEffectGeometry(){}
}
