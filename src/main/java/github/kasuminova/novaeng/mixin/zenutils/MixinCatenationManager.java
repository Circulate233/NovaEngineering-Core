package github.kasuminova.novaeng.mixin.zenutils;

import com.google.common.collect.Multimap;
import crafttweaker.mc1120.world.MCWorld;
import github.kasuminova.novaeng.common.performance.SparseTickTasks;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import youyihj.zenutils.api.util.catenation.Catenation;
import youyihj.zenutils.impl.util.catenation.CatenationManager;

import java.util.Collection;

@Mixin(value = CatenationManager.class, remap = false)
public abstract class MixinCatenationManager {

    @Shadow
    @Final
    private static Multimap<World, Catenation> catenations;

    @Shadow
    @Final
    private static Multimap<World, Catenation> cantenationsToAdd;

    /**
     * @author circulation
     * @reason Avoid building empty multimap views and iterators for worlds without catenations.
     */
    @Overwrite
    @SubscribeEvent
    public static void onWorldTick(final TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final Collection<Catenation> tasks = SparseTickTasks.existing(catenations, event.world);
            if (tasks != null) {
                tasks.removeIf(task -> task.tick(new MCWorld(event.world)));
            }
        } else if (event.phase == TickEvent.Phase.END) {
            SparseTickTasks.mergePending(catenations, cantenationsToAdd);
        }
    }
}
