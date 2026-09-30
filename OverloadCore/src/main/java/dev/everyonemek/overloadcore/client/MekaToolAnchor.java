package dev.everyonemek.overloadcore.client;

import org.joml.Matrix4f;

/** Nozzle center of Mek 10.7.19.85's native right/left OBJ; the barrel points down local Y. */
public final class MekaToolAnchor {
    public static Matrix4f nozzle(boolean left){
        return new Matrix4f().translate(left?-.453443F:.421557F,.155F,.03125F).rotateX((float)Math.PI/2);
    }
    private MekaToolAnchor(){}
}
