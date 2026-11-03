package github.kasuminova.novaeng.mixin.actinium;

import dhj.embeddedt.embeddium.impl.util.collections.quadtree.QuadTree;
import dhj.embeddedt.embeddium.impl.util.collections.quadtree.Rect2i;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuadTree.class, remap = false)
public abstract class MixinQuadTreeConstruction<T> extends Rect2i {

    @Shadow
    private QuadTree<T> child0;
    @Shadow
    private QuadTree<T> child1;
    @Shadow
    private QuadTree<T> child2;
    @Shadow
    private QuadTree<T> child3;

    @Unique
    private int nova$minSize;
    @Unique
    private int nova$childDepth;

    private MixinQuadTreeConstruction(final Rect2i rect) {
        super(rect);
    }

    @Invoker("<init>")
    private static <T> QuadTree<T> nova$createNode(final Rect2i rect, final int minSize, final int depth) {
        throw new AssertionError();
    }

    @ModifyConstant(method = "<init>(Ldhj/embeddedt/embeddium/impl/util/collections/quadtree/Rect2i;II)V",
        constant = @Constant(intValue = 12), require = 1)
    private int nova$deferChildren(final int original, final Rect2i rect, final int minSize, final int depth) {
        // A custom rectangle can override its accessors; keep their original calls.
        return rect.getClass() == Rect2i.class ? 0 : original;
    }

    @Inject(method = "<init>(Ldhj/embeddedt/embeddium/impl/util/collections/quadtree/Rect2i;II)V",
        at = @At("RETURN"), require = 1)
    private void nova$rememberSplit(final Rect2i rect, final int minSize, final int depth, final CallbackInfo ci) {
        nova$minSize = minSize;
        // The original constructor has already incremented its depth argument.
        nova$childDepth = rect.getClass() == Rect2i.class ? depth : 12;
    }

    @Redirect(method = "insert", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
        target = "Ldhj/embeddedt/embeddium/impl/util/collections/quadtree/QuadTree;child0:Ldhj/embeddedt/embeddium/impl/util/collections/quadtree/QuadTree;"), require = 1)
    private QuadTree<T> nova$prepareChildren(final QuadTree<T> tree) {
        final int depth = nova$childDepth;
        if (depth >= 12) {
            return child0;
        }
        // The original bake prunes unused children. Leave them unallocated until
        // the first insertion, then retain its split geometry and child ordering.
        nova$childDepth = 12;
        if (width() > nova$minSize && width() % 2 == 0) {
            final int childWidth = width() / 2;
            final int childHeight = height() / 2;
            if (width() == height() / 2) {
                child0 = nova$createNode(new Rect2i(x(), y(), width(), childHeight), nova$minSize, depth);
                child3 = nova$createNode(new Rect2i(x(), y() + childHeight, width(), childHeight), nova$minSize, depth);
            } else if (height() == width() / 2) {
                child0 = nova$createNode(new Rect2i(x(), y(), childWidth, height()), nova$minSize, depth);
                child1 = nova$createNode(new Rect2i(x() + childWidth, y(), childWidth, height()), nova$minSize, depth);
            } else {
                child0 = nova$createNode(new Rect2i(x(), y(), childWidth, childHeight), nova$minSize, depth);
                child1 = nova$createNode(new Rect2i(x() + childWidth, y(), childWidth, childHeight), nova$minSize, depth);
                child2 = nova$createNode(new Rect2i(x() + childWidth, y() + childHeight, childWidth, childHeight), nova$minSize, depth);
                child3 = nova$createNode(new Rect2i(x(), y() + childHeight, childWidth, childHeight), nova$minSize, depth);
            }
        }
        return child0;
    }
}
