package dev.everyonemek.gravity.expansion;
import dev.everyonemek.gravity.corona.CoronalField;
/** Fixed geometry; kinds 2 and 4 use a four-vertex projected control display. */
public final class ModuleField {
    public static void emit(int kind,boolean reduced,CoronalField.Vertex vertex){
        if(kind==2||kind==4){vertex.accept(-.36F,-.23F,0,-1,-1);vertex.accept(.36F,-.23F,0,1,-1);vertex.accept(.36F,.23F,0,1,1);vertex.accept(-.36F,.23F,0,-1,1);}
        else CoronalField.emit(reduced,vertex);
    }
    private ModuleField(){}
}
