package github.kasuminova.novaeng.mixin.geckolib3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib3.geo.raw.tree.RawBoneGroup;
import software.bernie.geckolib3.geo.raw.tree.RawGeometryTree;

import java.util.Objects;

@Mixin(value = RawGeometryTree.class, remap = false)
public abstract class MixinRawGeometryTreeLookup {
    /**
     * @author NovaEngineering
     * @reason Resolve a parent without allocating a flattened map for every bone insertion.
     */
    @Overwrite
    public static RawBoneGroup getGroupFromHierarchy(final RawGeometryTree hierarchy, final String bone) {
        RawBoneGroup found = null;
        for (final RawBoneGroup group : hierarchy.topLevelBones.values()) {
            found = nova$findLast(group, bone, found);
        }
        return found;
    }

    @Unique
    private static RawBoneGroup nova$findLast(final RawBoneGroup group, final String name, RawBoneGroup found) {
        // Upstream puts every node into a HashMap in this exact order. The last equal name wins,
        // even across roots, so neither an early return nor a direct children-map lookup is equivalent.
        if (Objects.equals(group.selfBone.getName(), name)) found = group;
        for (final RawBoneGroup child : group.children.values()) {
            found = nova$findLast(child, name, found);
        }
        return found;
    }
}
