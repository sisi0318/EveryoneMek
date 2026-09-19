package dev.everyonemek.factory;

/** Move legacy shared goods once, debiting only what an actual hatch accepted. Excess remains recoverable. */
public final class LegacyMigration {
    public static void transfer(Controller c) {
        if (c.getLevel().getGameTime() % 5 != 0) return;
        if (c.inputs.hasContents()) move(c.inputs, c.warehouseBank(false));
        if (c.outputs.hasContents()) move(c.outputs, c.warehouseBank(true));
    }
    private static void move(Buffers source, ResourceBank target) {
        for (int i = 0; i < source.itemSlots(); i++) {
            var stack = source.item(i); if (stack.isEmpty()) continue;
            var left = stack.copy();
            for (int pass = 0; pass < 2; pass++) for (int j = 0; j < target.itemSlots() && !left.isEmpty(); j++) {
                if (pass == 0 ? target.item(j).isEmpty() : !target.item(j).isEmpty()) continue;
                left = target.insert(j, left, false);
            }
            if (left.getCount() != stack.getCount()) source.item(i, left);
        }
        for (int i = 0; i < source.fluidTanks(); i++) {
            var stack = source.fluid(i); if (stack.isEmpty()) continue; int left = stack.getAmount();
            for (int pass = 0; pass < 2; pass++) for (int j = 0; j < target.fluidTanks() && left > 0; j++) {
                if (pass == 0 ? target.fluid(j).isEmpty() : !target.fluid(j).isEmpty()) continue;
                left -= target.insertFluid(j, stack.copyWithAmount(left), false);
            }
            if (left != stack.getAmount()) source.fluid(i, stack.copyWithAmount(left));
        }
        for (int i = 0; i < source.chemicalTanks(); i++) {
            var stack = source.chemical(i); if (stack.isEmpty()) continue; long left = stack.getAmount();
            for (int pass = 0; pass < 2; pass++) for (int j = 0; j < target.chemicalTanks() && left > 0; j++) {
                if (pass == 0 ? target.chemical(j).isEmpty() : !target.chemical(j).isEmpty()) continue;
                left -= target.insertChem(j, stack.copyWithAmount(left), false);
            }
            if (left != stack.getAmount()) source.chemical(i, stack.copyWithAmount(left));
        }
    }
    private LegacyMigration() {}
}
