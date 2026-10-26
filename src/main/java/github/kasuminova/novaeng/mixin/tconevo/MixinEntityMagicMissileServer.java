package github.kasuminova.novaeng.mixin.tconevo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.phanta.tconevo.entity.EntityMagicMissile;

@Mixin(value = EntityMagicMissile.class, remap = false, priority = 1100)
public abstract class MixinEntityMagicMissileServer {

    /**
     * @author Circulation
     * @reason Remove client-only particle bytecode from the dedicated-server entity class.
     */
    @Overwrite(remap = false)
    private void playFlightEffect() {
    }

    /**
     * @author Circulation
     * @reason Remove client-only particle bytecode from the dedicated-server entity class.
     */
    @Overwrite(remap = false)
    private void playHitEffect() {

    }
}
