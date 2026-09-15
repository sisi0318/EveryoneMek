package dev.everyonemek.overloadcore.mixin;
import dev.everyonemek.overloadcore.CoreConfig;
import dev.everyonemek.overloadcore.client.CoreClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
@Mixin(targets = "mekanism.client.sound.SoundHandler$TileTickableSound", remap = false)
public abstract class MachineSoundMixin {
    @ModifyReturnValue(method = "getVolume", at = @At("RETURN"))
    private float overload$volume(float volume) {
        var sound=(SoundInstance)(Object)this;
        return CoreConfig.HAZARDS.get() && volume > 0 && CoreClient.affected(sound.getX(), sound.getY(), sound.getZ()) ? volume * CoreConfig.SOUND_GAIN.get().floatValue() : volume;
    }
}
