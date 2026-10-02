package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the item an {@link ItemPredicate} is restricted to, so predicates can be grouped by the only item they can
 * ever match before anything is evaluated.
 */
@Mixin(ItemPredicate.class)
public interface AccessorItemPredicate {

    @Accessor("item")
    Item nova$getItem();
}
