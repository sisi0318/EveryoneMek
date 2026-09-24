package dev.everyonemek.gravity.solar;
import java.util.*;
import net.minecraft.core.Direction;
/** Generated from reviewed art/solar-design/layout.json by tools/generate_solar_resources.py. */
public final class SolarLayout {
 public record Slot(int x,int y,int z,SolarBlock.Kind kind,Direction face,boolean output){}
 public static final List<Slot> SLOTS=List.of(
new Slot(0,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(0,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(0,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(0,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(0,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,0,1,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,0,7,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,0,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,1,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,7,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(2,0,8,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,0,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,1,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,7,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(3,0,8,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,0,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,1,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,7,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(4,0,8,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,0,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,1,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,7,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(5,0,8,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,0,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,1,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,7,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(6,0,8,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(7,0,1,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(7,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(7,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(7,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(7,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(7,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(7,0,7,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(8,0,2,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(8,0,3,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(8,0,4,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(8,0,5,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(8,0,6,SolarBlock.Kind.BASE,Direction.NORTH,false),
new Slot(1,1,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,2,1,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(1,3,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,4,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,5,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,6,1,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(1,7,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,1,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,2,7,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(1,3,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,4,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,5,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(1,6,7,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(1,7,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,1,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,2,1,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(7,3,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,4,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,5,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,6,1,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(7,7,1,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,1,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,2,7,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(7,3,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,4,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,5,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(7,6,7,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(7,7,7,SolarBlock.Kind.SUPPORT,Direction.NORTH,false),
new Slot(0,2,2,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,2,3,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,2,4,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,2,5,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,2,6,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(2,2,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(2,2,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(3,2,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(3,2,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(4,2,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(4,2,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(5,2,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(5,2,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(6,2,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(6,2,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,2,2,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,2,3,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,2,4,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,2,5,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,2,6,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,6,2,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,6,3,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,6,4,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,6,5,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,6,6,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(2,6,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(2,6,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(3,6,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(3,6,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(4,6,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(4,6,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(5,6,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(5,6,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(6,6,0,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(6,6,8,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,6,2,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,6,3,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,6,4,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,6,5,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(8,6,6,SolarBlock.Kind.RING,Direction.NORTH,false),
new Slot(0,8,2,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(0,8,3,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(0,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(0,8,5,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(0,8,6,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(1,8,1,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(1,8,7,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(2,8,0,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(2,8,8,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(3,8,0,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(3,8,8,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,0,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,8,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(5,8,0,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(5,8,8,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(6,8,0,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(6,8,8,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(7,8,1,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(7,8,7,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(8,8,2,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(8,8,3,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(8,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(8,8,5,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(8,8,6,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(1,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(2,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(3,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(5,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(6,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(7,8,4,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,1,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,2,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,3,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,5,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,6,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,8,7,SolarBlock.Kind.CROWN,Direction.NORTH,false),
new Slot(4,1,4,SolarBlock.Kind.FOCUS,Direction.UP,false),
new Slot(4,7,4,SolarBlock.Kind.FOCUS,Direction.DOWN,false),
new Slot(4,4,4,SolarBlock.Kind.SEED,Direction.NORTH,false),
new Slot(0,3,3,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,3,3,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(3,3,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(3,3,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,3,4,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,3,4,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(4,3,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(4,3,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,3,5,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,3,5,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(5,3,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(5,3,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,4,3,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,4,3,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(3,4,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(3,4,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,4,4,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,4,4,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(4,4,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(4,4,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,4,5,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,4,5,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(5,4,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(5,4,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,5,3,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,5,3,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(3,5,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(3,5,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,5,4,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,5,4,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(4,5,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(4,5,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(0,5,5,SolarBlock.Kind.COLLECTOR,Direction.EAST,false),
new Slot(8,5,5,SolarBlock.Kind.COLLECTOR,Direction.WEST,false),
new Slot(5,5,0,SolarBlock.Kind.COLLECTOR,Direction.SOUTH,false),
new Slot(5,5,8,SolarBlock.Kind.COLLECTOR,Direction.NORTH,false),
new Slot(4,1,0,null,Direction.NORTH,false),
new Slot(2,1,0,SolarBlock.Kind.FUEL,Direction.NORTH,false),
new Slot(6,1,0,SolarBlock.Kind.ENERGY,Direction.NORTH,false),
new Slot(3,1,8,SolarBlock.Kind.ENERGY,Direction.SOUTH,true),
new Slot(5,1,8,SolarBlock.Kind.ENERGY,Direction.SOUTH,true),
new Slot(8,1,3,SolarBlock.Kind.ENERGY,Direction.EAST,true),
new Slot(8,1,5,SolarBlock.Kind.ENERGY,Direction.EAST,true));
 private static final Map<Integer,Slot> INDEX=new HashMap<>();
 static{for(var slot:SLOTS)INDEX.put(slot.x()*81+slot.y()*9+slot.z(),slot);}
 public static Slot get(int x,int y,int z){return INDEX.get(x*81+y*9+z);}
 public static boolean footprint(int x,int z){return Math.abs(x-4)+Math.abs(z-4)<=6;}
 private static int[] ringState(int x,int z){return switch(x*9+z){
  case 2 -> new int[]{0,0,0};
  case 3 -> new int[]{1,1,0};
  case 4 -> new int[]{2,8,0};
  case 5 -> new int[]{3,3,0};
  case 6 -> new int[]{4,4,0};
  case 10 -> new int[]{5,5,0};
  case 13 -> new int[]{7,7,0};
  case 16 -> new int[]{5,5,3};
  case 18 -> new int[]{4,4,1};
  case 22 -> new int[]{7,7,0};
  case 26 -> new int[]{0,0,3};
  case 27 -> new int[]{3,3,1};
  case 31 -> new int[]{7,7,0};
  case 35 -> new int[]{1,1,3};
  case 36 -> new int[]{2,8,1};
  case 37 -> new int[]{7,7,1};
  case 38 -> new int[]{7,7,1};
  case 39 -> new int[]{7,7,1};
  case 40 -> new int[]{6,6,0};
  case 41 -> new int[]{7,7,1};
  case 42 -> new int[]{7,7,1};
  case 43 -> new int[]{7,7,1};
  case 44 -> new int[]{2,8,3};
  case 45 -> new int[]{1,1,1};
  case 49 -> new int[]{7,7,0};
  case 53 -> new int[]{3,3,3};
  case 54 -> new int[]{0,0,1};
  case 58 -> new int[]{7,7,0};
  case 62 -> new int[]{4,4,3};
  case 64 -> new int[]{5,5,1};
  case 67 -> new int[]{7,7,0};
  case 70 -> new int[]{5,5,2};
  case 74 -> new int[]{4,4,2};
  case 75 -> new int[]{3,3,2};
  case 76 -> new int[]{2,8,2};
  case 77 -> new int[]{1,1,2};
  case 78 -> new int[]{0,0,2};
  default -> new int[]{2,7,0};};}
 public static int ringSegment(int x,int z,boolean crown){return ringState(x,z)[crown?1:0];}
 public static Direction ringFacing(int x,int z,boolean crown){return switch(ringState(x,z)[2]){case 1->Direction.EAST;case 2->Direction.SOUTH;case 3->Direction.WEST;default->Direction.NORTH;};}
 private SolarLayout(){}
}
