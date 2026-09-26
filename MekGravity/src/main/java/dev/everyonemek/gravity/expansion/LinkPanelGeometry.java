package dev.everyonemek.gravity.expansion;
/** Small pure geometry shared by rendering and hit-testing. Coordinates are canvas-local. */
public final class LinkPanelGeometry {
    public static final int WIDTH=118,HEIGHT=46,COLUMN=158,ROW=60,SEGMENTS=24;
    public record Point(double x,double y){}
    public static Point[] layout(int[] columns){int[] rows=new int[3];Point[] result=new Point[columns.length];for(int i=0;i<columns.length;i++){int c=Math.clamp(columns[i],0,2);result[i]=new Point(c*COLUMN,rows[c]++*ROW);}return result;}
    public static Point curve(Point a,Point b,double t){double bend=Math.max(35,Math.abs(b.x-a.x)*.5),u=1-t;return new Point(u*u*u*a.x+3*u*u*t*(a.x+bend)+3*u*t*t*(b.x-bend)+t*t*t*b.x,u*u*u*a.y+3*u*u*t*a.y+3*u*t*t*b.y+t*t*t*b.y);}
    public static double segmentDistance(Point p,Point a,Point b){double dx=b.x-a.x,dy=b.y-a.y,n=dx*dx+dy*dy,t=n==0?0:Math.clamp(((p.x-a.x)*dx+(p.y-a.y)*dy)/n,0,1);double x=p.x-a.x-t*dx,y=p.y-a.y-t*dy;return x*x+y*y;}
    public static boolean contains(Point origin,Point p){return p.x>=origin.x&&p.x<=origin.x+WIDTH&&p.y>=origin.y&&p.y<=origin.y+HEIGHT;}
    private LinkPanelGeometry(){}
}
