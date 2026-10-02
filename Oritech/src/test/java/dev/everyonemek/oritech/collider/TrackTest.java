package dev.everyonemek.oritech.collider;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrackTest {
    private static Map<Integer,Track.Node> loop(int low,int high){var cells=new TreeMap<Integer,Track.Node>();
        for(int n=low+2;n<=high-2;n++){cells.put(Track.cell(n,low),new Track.Node(Track.MOTOR,0,0));cells.put(Track.cell(high,n),new Track.Node(Track.MOTOR,2,0));cells.put(Track.cell(n,high),new Track.Node(Track.MOTOR,4,0));cells.put(Track.cell(low,n),new Track.Node(Track.MOTOR,6,0));}
        int[][] corners={{high-2,low,0,2},{high-1,low+1,1,0},{high,low+2,6,1},{high,high-2,2,2},{high-1,high-1,3,0},{high-2,high,0,1},
            {low+2,high,4,2},{low+1,high-1,5,0},{low,high-2,2,1},{low,low+2,6,2},{low+1,low+1,7,0},{low+2,low,4,1}};
        for(var c:corners)cells.put(Track.cell(c[0],c[1]),new Track.Node(Track.RING,c[2],c[3]));
        cells.put(Track.cell(low+4,low),new Track.Node(Track.A,0,0));cells.put(Track.cell(high-4,low),new Track.Node(Track.B,4,0));return cells;
    }
    @Test void actualRouteAcceleratesChargesEveryStepAndSchedulesCollision(){var rules=new Track.Rules(10,2.5,10);var plan=Track.plan(loop(3,60),rules);assertTrue(plan.valid(),plan.toString());assertTrue(Track.preflight(plan,rules,15000).valid());var beam=new Track.Beam();long spent=0;boolean collided=false;
        for(int i=0;i<3000;i++){var step=Track.advance(plan,beam,rules,Long.MAX_VALUE,15000);assertEquals(Track.Fault.NONE,step.fault());spent+=step.spent();if(step.collision()){collided=true;break;}}
        assertTrue(collided,"A must physically arrive at B after reaching threshold");assertTrue(beam.speed+1>=15000);
        assertEquals(10*beam.speed*(beam.speed-1)/2,spent,"Every unit of acceleration pays its increasing native energy cost");
        assertEquals(Track.B,plan.segments().get(beam.segment).target().kind());
        var adjacentOnly=new Track.Rules(1,2.5,10);assertTrue(Track.preflight(Track.plan(loop(3,60),adjacentOnly),adjacentOnly,15000).valid(),"The legal one-cell gate configuration must not throw or reject adjacent guides");
    }
    @Test void insufficientEnergyPreservesMotorProgressAndNeverGivesFreeSpeed(){var plan=Track.plan(loop(3,60),new Track.Rules(10,2.5,10));var beam=new Track.Beam();
        Track.Step stalled=null;for(int i=0;i<100;i++){stalled=Track.advance(plan,beam,new Track.Rules(10,2.5,10),0,500);if(stalled.needsPower())break;}
        assertTrue(stalled.needsPower());assertEquals(1,beam.speed);int segment=beam.segment;double offset=beam.offset;
        var retry=Track.advance(plan,beam,new Track.Rules(10,2.5,10),9,500);assertTrue(retry.needsPower());assertEquals(segment,beam.segment);assertEquals(offset,beam.offset);assertEquals(0,retry.spent());
        var paid=Track.advance(plan,beam,new Track.Rules(10,2.5,10),10,500);assertEquals(10,paid.spent());assertEquals(2,beam.speed);
    }
    @Test void brokenPathsWrongEmitterDirectionAndTightBendsCannotProduce(){var cells=loop(3,60);var rules=new Track.Rules(10,2.5,10);int b=Track.cell(56,3);cells.put(b,new Track.Node(Track.B,0,0));assertEquals(Track.Fault.WRONG_FACING,Track.plan(cells,rules).fault());
        var tiny=Track.plan(loop(3,16),rules);assertTrue(tiny.valid());var beam=new Track.Beam();Track.Fault fault=Track.Fault.NONE;boolean collision=false;
        for(int i=0;i<1000&&fault==Track.Fault.NONE;i++){var step=Track.advance(tiny,beam,rules,Long.MAX_VALUE,15000);fault=step.fault();collision|=step.collision();}
        assertEquals(Track.Fault.BEND_TOO_TIGHT,fault);assertFalse(collision);
        assertEquals(Track.Fault.BEND_TOO_TIGHT,Track.preflight(tiny,rules,15000).fault());
        cells=loop(3,60);cells.remove(Track.cell(58,3));assertFalse(Track.plan(cells,rules).valid());
    }
}
