package github.kasuminova.novaeng.common.assembly;

import github.kasuminova.novaeng.novaeng_core.Tags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Captures only drops spawned by the synchronous harvest call.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class AssemblyDrops implements AutoCloseable {
    private static final ThreadLocal<AssemblyDrops> ACTIVE = new ThreadLocal<>();
    private AssemblyDrops previous;
    private final EntityPlayerMP player;
    private final AssemblyMaterials materials;
    private final ObjectArrayList<EntityItem> drops = new ObjectArrayList<>();

    AssemblyDrops(final EntityPlayerMP player, final AssemblyMaterials materials) {
        this.player = player;
        this.materials = materials;
    }

    AssemblyDrops begin() {
        previous = ACTIVE.get();
        ACTIVE.set(this);
        return this;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void capture(final EntityJoinWorldEvent event) {
        final var capture = ACTIVE.get();
        if (capture != null && event.getWorld() == capture.player.world && event.getEntity() instanceof EntityItem item) {
            capture.drops.add(item);
            event.setCanceled(true);
        }
    }

    @Override
    public void close() {
        ACTIVE.remove();
        try {
            for (int i = 0; i < drops.size(); i++) {
                final var entity = drops.get(i);
                materials.retain(materials.recover(entity.getItem()));
                entity.setDead();
            }
        } finally {
            drops.clear();
            if (previous != null) ACTIVE.set(previous);
            previous = null;
        }
    }
}
