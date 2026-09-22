package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.creativetab.CreativeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes a creative tab's label.
 *
 * <p>Mixin's refmap translates the field name, so callers no longer need Forge's deprecated reflection helper
 * or a hand-written list of the MCP and SRG spellings.</p>
 */
@Mixin(CreativeTabs.class)
public interface AccessorCreativeTabs {

    @Accessor("tabLabel")
    String nova$getTabLabel();

}
