package dev.everyonemek.ars;

// Harvest calculation adapted from Ars Nouveau's DrygmyTile (Ars Nouveau contributors), LGPL-3.0.
// Changes: inventory targets, retained jar snapshots, upgradeable processing and persistent output batches.

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.common.entity.EntityDrygmy;
import com.hollingsworth.arsnouveau.common.items.MobJarItem;
import com.hollingsworth.arsnouveau.common.lib.EntityTags;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

/** Inventory-based harvesting with the same bonus, loot selection and XP conversion as Ars DrygmyTile. */
final class DrygmyHarvest {
    static final int JAR_SLOTS = 8;
    private static final int MAX_ROLLS = 16_384;
    record Samples(CompoundTag context, List<LivingEntity> entities, int bonus, int types) { }

    static boolean accepts(ItemStack stack) {
        var data = stack.get(DataComponentRegistry.MOB_JAR);
        return stack.getItem() instanceof MobJarItem && data != null && data.entityTag().filter(tag -> !tag.isEmpty()).isPresent();
    }

    static BasicInventorySlot slot(IContentsListener listener, int x, int y) {
        return new BasicInventorySlot(1, (stack, automation) -> automation != AutomationType.EXTERNAL,
              (stack, automation) -> true, DrygmyHarvest::accepts, listener, x, y) { };
    }

    static boolean hasJars(SourceMachine m) { return m.jarSlots != null && m.jarSlots.stream().anyMatch(slot -> !slot.isEmpty()); }
    static boolean hasPending(CompoundTag tag) { return tag != null && tag.getBoolean("paid"); }

    static AdvancedRecipes.Work plan(SourceMachine m) {
        if (hasPending(m.drygmyHarvest)) return deliver(m, m.drygmyHarvest, true);
        // Drain the old collection slots before harvesting; upgrading never erases their contents.
        var buffered = new ArrayList<ItemStack>();
        int[] consume = new int[m.inputs.size()];
        for (int i = 1; i < m.inputs.size(); i++) {
            ItemStack stack = m.inputs.get(i).getStack();
            if (!stack.isEmpty() && WorldControllers.acceptsCollected(m, stack)) {
                buffered.add(stack.copy()); consume[i] = stack.getCount();
            }
        }
        if (!buffered.isEmpty()) return AdvancedRecipes.fixed(m, "arsmekanism:collect_buffer", consume, buffered, 0, 0, 0, 0);

        ServerLevel level = (ServerLevel) m.getLevel();
        CompoundTag context = context(m);
        if (m.drygmySamples == null || !context.equals(m.drygmySamples.context())) m.drygmySamples = samples(m, context);
        var sample = m.drygmySamples;
        m.observe(sample.bonus(), sample.entities().size(), sample.types());
        if (sample.entities().isEmpty() || sample.entities().stream().allMatch(entity -> entity.getType().is(EntityTags.DRYGMY_BLACKLIST))) {
            m.cacheDrygmyHarvest(null); m.status(SourceMachine.NO_JAR_CREATURE); return null;
        }
        CompoundTag saved = m.drygmyHarvest;
        if (saved == null || saved.getInt("version") != 1 || !context.equals(saved.getCompound("context"))
              || !saved.contains("products", Tag.TAG_LIST)) {
            m.cacheDrygmyHarvest(null);
            if (m.sourceTank().getStored() < Config.DRYGMY_MANA_COST.get()) { m.status(SourceMachine.NEED_SOURCE); return null; }
            if (m.energy().getEnergy() < m.energy().getEnergyPerTick()) { m.status(SourceMachine.NEED_ENERGY); return null; }
            long count = Math.max(0L, (long) Config.DRYGMY_BASE_ITEM.get() + sample.bonus());
            if (count > MAX_ROLLS) { m.status(SourceMachine.OUTPUT_FULL); return null; }
            var products = roll(level, sample.entities(), (int) count);
            // Some mobs mutate their base XP when queried (notably baby zombies). Never reuse a
            // sampled entity after rolling; the next harvest loads fresh copies from the unchanged jars.
            m.drygmySamples = null;
            products.removeIf(stack -> !WorldControllers.acceptsCollected(m, stack));
            saved = new CompoundTag(); saved.putInt("version", 1); saved.put("context", context);
            saved.putInt("bonus", sample.bonus()); saved.putInt("creatures", sample.entities().size()); saved.putInt("types", sample.types());
            encodeProducts(saved, compact(products), level.registryAccess());
            m.cacheDrygmyHarvest(saved);
        }
        return deliver(m, saved, false);
    }

    private static AdvancedRecipes.Work deliver(SourceMachine m, CompoundTag saved, boolean paid) {
        m.observe(saved.getInt("bonus"), saved.getInt("creatures"), saved.getInt("types"));
        List<ItemStack> products = readProducts(saved, m.getLevel().registryAccess());
        if (products == null) { m.cacheDrygmyHarvest(null); m.status(SourceMachine.NO_JAR_CREATURE); return null; }
        // A diverse harvest may contain more item types than the six output slots. Queue the remainder
        // after paying once, then drain fixed batches before allowing another harvest or external mode.
        int batchSize = Math.min(m.outputs().size(), products.size());
        List<ItemStack> batch = products.subList(0, batchSize);
        var work = AdvancedRecipes.work(m, paid ? "arsmekanism:drygmy_output" : "arsmekanism:drygmy_harvest",
              new int[m.inputs.size()], batch, () -> batch, paid ? 0 : Config.DRYGMY_MANA_COST.get(), 0, 0, 0, saved, () -> {
                  if (batchSize == products.size()) m.cacheDrygmyHarvest(null);
                  else {
                      var remaining = saved.copy(); remaining.putBoolean("paid", true);
                      encodeProducts(remaining, products.subList(batchSize, products.size()), m.getLevel().registryAccess());
                      m.cacheDrygmyHarvest(remaining);
                  }
              });
        int ticks = paid ? 1 : MachineConfig.DRYGMY_HARVEST_TICKS.get();
        work.signature().putInt("ticks", ticks);
        return new AdvancedRecipes.Work(work.id(), ticks, work.consume(), work.maximumOutputs(), work.outputs(),
              work.sourceCost(), 0, 0, 0, work.signature(), work.commit());
    }

    private static Samples samples(SourceMachine m, CompoundTag context) {
        var entities = new ArrayList<LivingEntity>();
        var types = new HashSet<ResourceLocation>();
        for (var slot : m.jarSlots) {
            if (!accepts(slot.getStack())) continue;
            // No world registration, entity ticking, jar mutation or jar behavior dispatch.
            ItemStack copy = slot.getStack().copy();
            var data = copy.get(DataComponentRegistry.MOB_JAR);
            copy.set(DataComponentRegistry.MOB_JAR, new com.hollingsworth.arsnouveau.common.items.data.MobJarData(
                  data.entityTag().map(CompoundTag::copy), data.extraDataTag().map(CompoundTag::copy)));
            var entity = MobJarItem.fromItem(copy, m.getLevel());
            if (!(entity instanceof LivingEntity living) || living instanceof EntityDrygmy || living instanceof Player) continue;
            living.setBoundingBox(new AABB(0, 0, 0, 0, 0, 0)); living.setPos(m.getBlockPos().getCenter());
            entities.add(living); types.add(EntityType.getKey(living.getType()));
        }
        int bonus = types.size() * Config.DRYGMY_UNIQUE_BONUS.get() + Math.min(Config.DRYGMY_QUANTITY_CAP.get(), entities.size());
        return new Samples(context, List.copyOf(entities), bonus, types.size());
    }

    private static List<ItemStack> compact(List<ItemStack> products) {
        var result = new ArrayList<ItemStack>();
        for (ItemStack product : products) {
            int remaining = product.getCount(), limit = Math.min(64, product.getMaxStackSize());
            for (ItemStack stack : result) if (remaining > 0 && ItemStack.isSameItemSameComponents(stack, product)) {
                int moved = Math.min(remaining, limit - stack.getCount()); stack.grow(moved); remaining -= moved;
            }
            while (remaining > 0) { int moved = Math.min(remaining, limit); result.add(product.copyWithCount(moved)); remaining -= moved; }
        }
        return List.copyOf(result);
    }

    private static void encodeProducts(CompoundTag tag, List<ItemStack> products, HolderLookup.Provider provider) {
        var encoded = new ListTag(); long total = 0;
        for (ItemStack stack : products) { encoded.add(stack.save(provider)); total += stack.getCount(); }
        tag.put("products", encoded); tag.putInt("pending_count", (int) Math.min(Integer.MAX_VALUE, total));
    }

    private static CompoundTag context(SourceMachine m) {
        var tag = new CompoundTag();
        var jars = new ListTag();
        for (var slot : m.jarSlots) jars.add(slot.isEmpty() ? new CompoundTag() : slot.getStack().save(m.getLevel().registryAccess()).copy());
        tag.put("jars", jars);
        tag.put("filter", m.inputs.getFirst().isEmpty() ? new CompoundTag() : m.inputs.getFirst().getStack().copyWithCount(1).save(m.getLevel().registryAccess()));
        tag.putInt("filter_mode", m.mode());
        tag.putInt("base", Config.DRYGMY_BASE_ITEM.get());
        tag.putInt("unique_bonus", Config.DRYGMY_UNIQUE_BONUS.get());
        tag.putInt("quantity_cap", Config.DRYGMY_QUANTITY_CAP.get());
        tag.putInt("source", Config.DRYGMY_MANA_COST.get());
        return tag;
    }

    private static List<ItemStack> readProducts(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag encoded = tag.getList("products", Tag.TAG_COMPOUND);
        if (encoded.size() > MAX_ROLLS + 4_096) return null;
        var products = new ArrayList<ItemStack>();
        for (int i = 0; i < encoded.size(); i++) {
            var stack = ItemStack.parse(provider, encoded.getCompound(i));
            if (stack.isEmpty()) return null;
            products.add(stack.get());
        }
        return List.copyOf(products);
    }

    private static ArrayList<ItemStack> roll(ServerLevel level, List<LivingEntity> entities, int numberItems) {
        var pool = new ArrayList<ItemStack>();
        var player = ANFakePlayer.getPlayer(level);
        var damage = level.damageSources().playerAttack(player);
        long experience = 0;
        for (LivingEntity entity : entities) {
            if (entity.getType().is(EntityTags.DRYGMY_BLACKLIST) || entity.getLootTable() == null) continue;
            var loot = level.getServer().reloadableRegistries().getLootTable(entity.getLootTable());
            var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, entity)
                  .withParameter(LootContextParams.ORIGIN, entity.position()).withParameter(LootContextParams.DAMAGE_SOURCE, damage)
                  .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
                  .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, damage.getDirectEntity())
                  .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
            pool.addAll(loot.getRandomItems(params.create(LootContextParamSets.ENTITY)));
            experience += Math.max(0, entity.getExperienceReward(level, player));
        }
        pool.removeIf(ItemStack::isEmpty);
        var products = new ArrayList<ItemStack>();
        int picked = 0;
        if (!pool.isEmpty()) for (int i = 0; i < numberItems; i++) {
            ItemStack stack = pool.get(level.random.nextInt(pool.size())).copy();
            products.add(stack); picked += stack.getCount();
            if (picked >= numberItems) break;
        }
        long xp = experience / 4;
        if (xp > 3) {
            int greater = (int) Math.min(Integer.MAX_VALUE, xp / 12);
            int lesser = (int) ((xp % 12 + 2) / 3);
            if (greater > 0) products.add(new ItemStack(ItemsRegistry.GREATER_EXPERIENCE_GEM, greater));
            if (lesser > 0) products.add(new ItemStack(ItemsRegistry.EXPERIENCE_GEM, lesser));
        }
        return products;
    }

    private DrygmyHarvest() { }
}
