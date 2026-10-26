package github.kasuminova.novaeng.common.entity;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Marker entity for items created by a Botania mana pool.
 *
 * <p>The class intentionally does not change {@link EntityItem}'s behavior;
 * its type is used to keep pool outputs from being accepted as new inputs.</p>
 */
public class EntityManaPoolOutputItem extends EntityItem {

    public EntityManaPoolOutputItem(final World world) {
        super(world);
    }

    public EntityManaPoolOutputItem(final World world, final double x, final double y, final double z) {
        super(world, x, y, z);
    }

    public EntityManaPoolOutputItem(final World world, final double x, final double y, final double z,
                                    final ItemStack stack) {
        super(world, x, y, z, stack);
    }

}
