package dev.everyonemek.gravity.expansion;
public enum ModuleKind {
    CAPTOR("flare_captor",true),FORGE("gravity_forge",true),TUNER("core_tuner",false),NODE("gravity_node",true),OBSERVATORY("stellar_observatory",false);
    public final String id;public final boolean cargo;
    ModuleKind(String id,boolean cargo){this.id=id;this.cargo=cargo;}
    public boolean processor(){return this==CAPTOR||this==FORGE;}
}
