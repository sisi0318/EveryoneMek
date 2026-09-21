package dev.everyonemek.gravity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Face borders use the four coplanar neighbors and four diagonals; connections are never saved in NBT. */
public final class GlassConnections {
    public static int bit(int x,int y,int z){return 1<<((x+1)*9+(y+1)*3+z+1);}

    public static int sample(BlockGetter level,BlockPos pos,BlockState state){
        int neighbors=0;var cursor=new BlockPos.MutableBlockPos();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            if(x==0&&y==0&&z==0)continue;
            cursor.setWithOffset(pos,x,y,z);
            if(level.getBlockState(cursor).is(state.getBlock()))neighbors|=bit(x,y,z);
        }
        return neighbors;
    }

    private static boolean connected(int neighbors,Direction.Axis faceAxis,int a,int b){
        // These two in-plane axes must match tools/generate_resources.py:glass_parts.
        int x=faceAxis==Direction.Axis.X?0:a;
        int y=faceAxis==Direction.Axis.Y?0:b;
        int z=faceAxis==Direction.Axis.X?a:faceAxis==Direction.Axis.Y?b:0;
        return (neighbors&bit(x,y,z))!=0;
    }

    public static int visibleParts(int neighbors,Direction face){
        if((neighbors&bit(face.getStepX(),face.getStepY(),face.getStepZ()))!=0)return 0;
        return planarParts(neighbors,face);
    }

    public static int planarParts(int neighbors,Direction face){
        var axis=face.getAxis();int visible=0;
        boolean aMin=connected(neighbors,axis,-1,0),aMax=connected(neighbors,axis,1,0);
        boolean bMin=connected(neighbors,axis,0,-1),bMax=connected(neighbors,axis,0,1);
        if(!aMin)visible|=1;if(!aMax)visible|=2;if(!bMin)visible|=4;if(!bMax)visible|=8;
        // Corners bridge the outer contour and remain at concave notches when the diagonal is absent.
        for(int a=0;a<2;a++)for(int b=0;b<2;b++)
            if(!(a==0?aMin:aMax)||!(b==0?bMin:bMax)||!connected(neighbors,axis,a==0?-1:1,b==0?-1:1))visible|=1<<(4+a*2+b);
        return visible;
    }

    private GlassConnections(){}
}
