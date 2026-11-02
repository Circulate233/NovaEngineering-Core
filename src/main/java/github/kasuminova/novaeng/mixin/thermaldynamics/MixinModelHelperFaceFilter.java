package github.kasuminova.novaeng.mixin.thermaldynamics;

import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.Vertex5;
import codechicken.lib.vec.uv.UV;
import cofh.thermaldynamics.render.ModelHelper;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.LinkedList;
import java.util.function.Predicate;

@Mixin(value = ModelHelper.class, remap = false)
public abstract class MixinModelHelperFaceFilter {

    @Redirect(method = "simplifyModel", at = @At(value = "INVOKE",
        target = "Ljava/util/LinkedList;removeIf(Ljava/util/function/Predicate;)Z"), require = 1)
    private static boolean nova$skipUnmergeableFaces(final LinkedList<ModelHelper.Face> faces,
                                                     final Predicate<? super ModelHelper.Face> filter,
                                                     final LinkedList<Vertex5> in,
                                                     @Local(name = "f") final ModelHelper.Face face,
                                                     @Share("nova$customVertices") final LocalBooleanRef customVertices) {
        if (!customVertices.get() && in.getClass() == LinkedList.class && nova$plainVertices(face)) {
            // In this TD version, attemptToCombineUnflipped compares this face with
            // itself before looking at the other face. After isPolygon rejects equal
            // adjacent positions, only equal opposite vertices can pass that gate.
            // Reversing either face cannot create such a pair. Preserve the existing
            // algorithm for those pairs instead of changing its merge semantics.
            // Check both directions: equalsT's floating-point interval can be asymmetric.
            if (!face.equalVert(face.vec(0), face.vec(2)) && !face.equalVert(face.vec(2), face.vec(0))
                && !face.equalVert(face.vec(1), face.vec(3)) && !face.equalVert(face.vec(3), face.vec(1))) {
                return false;
            }
        } else {
            // A custom vertex or input iterator can mutate earlier faces via callbacks.
            // Retain every original predicate call from this point onward.
            customVertices.set(true);
        }
        return faces.removeIf(filter);
    }

    @Unique
    private static boolean nova$plainVertices(final ModelHelper.Face face) {
        if (face.getClass() != ModelHelper.Face.class || face.verts == null || face.verts.length != 4) {
            return false;
        }
        for (final Vertex5 vertex : face.verts) {
            if (vertex == null || vertex.getClass() != Vertex5.class
                || vertex.vec == null || vertex.vec.getClass() != Vector3.class
                || vertex.uv == null || vertex.uv.getClass() != UV.class) {
                return false;
            }
        }
        return true;
    }
}
