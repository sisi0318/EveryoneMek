package dev.everyonemek.oritech.collider;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmartTrackTest {
    private static int p(int x,int y){return Track.cell(x,y);}
    @Test void draggingAnElbowCreatesTwoLegalFortyFiveDegreeBends(){
        var result=SmartTrack.plan(Map.of(),new int[]{p(2,2),p(3,2),p(4,2),p(4,3),p(4,4)},Track.RING,0,10);
        assertTrue(result.valid(),result.toString());assertEquals(4,result.required());assertFalse(result.nodes().containsKey(p(4,2)),"The sharp corner cell must be bevelled away");
        assertEquals(1,result.nodes().get(p(3,2)).exit(0));assertEquals(2,result.nodes().get(p(4,3)).exit(1));
        assertEquals(2,result.nodes().values().stream().filter(n->n.bend()!=0).count());
    }
    @Test void extendingAnOpenEndRetunesThatEndButPreservesItsExistingConnection(){
        var existing=new TreeMap<Integer,Track.Node>();existing.put(p(1,2),new Track.Node(Track.A,0,0));existing.put(p(2,2),new Track.Node(Track.RING,0,0));
        var copy=new TreeMap<>(existing);var result=SmartTrack.plan(existing,new int[]{p(3,3)},Track.RING,0,10);
        assertTrue(result.valid(),result.toString());assertEquals(1,result.required());assertEquals(1,result.nodes().get(p(2,2)).exit(0));
        assertEquals(copy,existing,"Preview mutated the live beamline");assertFalse(result.nodes().containsKey(p(1,2)),"Emitter was silently rotated");
    }
    @Test void occupiedCornersAndAmbiguousJunctionsAreRejectedWithoutChangingAnything(){
        var occupied=Map.of(p(4,2),new Track.Node(Track.MOTOR,0,0));var result=SmartTrack.plan(occupied,new int[]{p(2,2),p(3,2),p(4,2),p(4,3),p(4,4)},Track.RING,0,10);
        assertEquals(SmartTrack.Failure.OCCUPIED_CORNER,result.failure());assertTrue(result.nodes().isEmpty());
        var cross=new TreeMap<Integer,Track.Node>();cross.put(p(3,4),new Track.Node(Track.RING,0,0));cross.put(p(5,4),new Track.Node(Track.RING,0,0));cross.put(p(4,3),new Track.Node(Track.RING,2,0));cross.put(p(4,5),new Track.Node(Track.RING,2,0));
        assertEquals(SmartTrack.Failure.AMBIGUOUS,SmartTrack.plan(cross,new int[]{p(4,4)},Track.RING,0,10).failure());
        var connected=new TreeMap<Integer,Track.Node>();connected.put(p(4,4),new Track.Node(Track.RING,2,0));connected.put(p(4,2),new Track.Node(Track.MOTOR,0,0));connected.put(p(4,6),new Track.Node(Track.MOTOR,0,0));
        var nearby=SmartTrack.plan(connected,new int[]{p(5,5)},Track.RING,0,10);assertTrue(nearby.valid());assertFalse(nearby.nodes().containsKey(p(4,4)),"A connected guide was mistaken for an open end because motor artwork faced sideways");
    }
    @Test void aDrawnClosedLoopConnectsFixedOpposingEmittersAndKeepsExistingMotor(){
        var nodes=new TreeMap<Integer,Track.Node>();nodes.put(p(4,2),new Track.Node(Track.A,0,0));nodes.put(p(7,2),new Track.Node(Track.B,4,0));nodes.put(p(6,9),new Track.Node(Track.MOTOR,4,0));
        var stroke=new ArrayList<Integer>();for(int x=4;x<=9;x++)stroke.add(p(x,2));for(int y=3;y<=9;y++)stroke.add(p(9,y));for(int x=8;x>=2;x--)stroke.add(p(x,9));for(int y=8;y>=2;y--)stroke.add(p(2,y));for(int x=3;x<=4;x++)stroke.add(p(x,2));
        var result=SmartTrack.plan(nodes,stroke.stream().mapToInt(Integer::intValue).toArray(),Track.RING,0,10);assertTrue(result.valid(),result.toString());
        var built=new TreeMap<>(nodes);built.putAll(result.nodes());assertTrue(Track.plan(built,new Track.Rules(10,2.5,10)).valid());
        assertEquals(nodes.get(p(4,2)),built.get(p(4,2)));assertEquals(nodes.get(p(7,2)),built.get(p(7,2)));assertEquals(Track.MOTOR,built.get(p(6,9)).kind());
    }
    @Test void mixedStrokeUsesMotorsForStraightsAndGuidesForBothBevels(){
        var result=SmartTrack.mixed(Map.of(),new int[]{p(2,2),p(3,2),p(4,2),p(4,3),p(4,4)},0,10);
        assertTrue(result.valid(),result.toString());assertEquals(Track.MOTOR,result.nodes().get(p(2,2)).kind());assertEquals(Track.MOTOR,result.nodes().get(p(4,4)).kind());
        assertEquals(Track.RING,result.nodes().get(p(3,2)).kind());assertEquals(Track.RING,result.nodes().get(p(4,3)).kind());
        assertEquals(2,result.needed(Map.of(),Track.MOTOR));assertEquals(2,result.needed(Map.of(),Track.RING));
    }
    @Test void extendingMotorEndpointUsesOneNewGuideAndReusesTheOldMotor(){
        var existing=Map.of(p(1,2),new Track.Node(Track.A,0,0),p(2,2),new Track.Node(Track.MOTOR,0,0));
        var result=SmartTrack.mixed(existing,new int[]{p(3,3)},0,10);assertTrue(result.valid(),result.toString());
        assertEquals(Track.RING,result.nodes().get(p(2,2)).kind());assertEquals(1,result.nodes().get(p(2,2)).exit(0));assertEquals(Track.MOTOR,result.nodes().get(p(3,3)).kind());
        assertEquals(0,result.needed(existing,Track.MOTOR));assertEquals(1,result.needed(existing,Track.RING));assertFalse(result.nodes().containsKey(p(1,2)));
        var sharp=SmartTrack.mixed(existing,new int[]{p(2,2),p(2,3),p(2,4)},0,10);assertEquals(SmartTrack.Failure.SHARP_TURN,sharp.failure(),"An unsupported 90-degree continuation must not disconnect the existing incoming beamline");
    }
}
