package github.kasuminova.novaeng.mixin.geckolib3;

import com.sun.management.ThreadMXBean;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import software.bernie.geckolib3.geo.raw.pojo.Bone;
import software.bernie.geckolib3.geo.raw.pojo.Converter;
import software.bernie.geckolib3.geo.raw.tree.RawBoneGroup;
import software.bernie.geckolib3.geo.raw.tree.RawGeometryTree;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * Standalone differential test against the installed GeckoLib, with optional local model corpus.
 */
public final class RawGeometryTreeLookupTest {
    private static volatile RawBoneGroup sink;
    private static int checks;

    static void main(final String[] args) throws Exception {
        final Random random = new Random(0x47454f);
        final List<RawGeometryTree> trees = new ObjectArrayList<>();
        for (int sample = 0; sample < 100; sample++) {
            final RawGeometryTree tree = new RawGeometryTree();
            final List<RawBoneGroup> groups = new ObjectArrayList<>();
            for (int i = 0; i < 100; i++) {
                final Bone bone = new Bone();
                bone.setName(i % 13 == 0 ? null : "bone" + random.nextInt(20));
                final RawBoneGroup group = new RawBoneGroup(bone);
                // Different map keys intentionally exercise duplicate and null selfBone names.
                if (i == 0 || random.nextInt(5) == 0) tree.topLevelBones.put("key" + i, group);
                else groups.get(random.nextInt(groups.size())).children.put("key" + i, group);
                groups.add(group);
            }
            for (int i = 0; i < 21; i++) check(tree, "bone" + i);
            check(tree, null);
            trees.add(tree);
        }
        final RawGeometryTree empty = new RawGeometryTree();
        check(empty, null);
        check(empty, "missing");
        final RawBoneGroup added = new RawBoneGroup(new Bone());
        added.selfBone.setName("added");
        empty.topLevelBones.put("added", added);
        check(empty, "added");
        added.selfBone.setName("renamed");
        check(empty, "added");
        check(empty, "renamed");
        empty.topLevelBones.clear();
        check(empty, "renamed");

        int models = 0;
        try (var paths = Files.walk(Path.of(args[0]))) {
            final List<Path> jsonPaths = paths.filter(p -> p.toString().endsWith(".json")).toList();
            for (int index = 0; index < jsonPaths.size(); index++) {
                final Path path = jsonPaths.get(index);
                final var raw = Converter.fromJsonString(Files.readString(path));
                final var tree = RawGeometryTree.parseHierarchy(raw, null);
                for (final Bone bone : raw.getMinecraftGeometry()[0].getBones()) {
                    check(tree, bone.getName());
                    check(tree, bone.getParent());
                }
                check(tree, "nova$missing");
                trees.add(tree);
                models++;
            }
        }
        // Warm both paths; compare allocated bytes on the same parsed models and lookup sequence.
        for (int i = 0; i < 4; i++) {
            measure(trees, false);
            measure(trees, true);
        }
        final long original = measure(trees, false);
        final long optimized = measure(trees, true);
        if (optimized >= original) throw new AssertionError("No allocation reduction");
        System.out.println("Passed " + checks + " identity comparisons; " + models + " local models. Lookup bytes: "
            + original + " original, " + optimized + " optimized.");
    }

    private static void check(final RawGeometryTree tree, final String name) {
        if (RawGeometryTree.getGroupFromHierarchy(tree, name)
            != MixinRawGeometryTreeLookup.getGroupFromHierarchy(tree, name)) {
            throw new AssertionError("Lookup differs for " + name);
        }
        checks++;
    }

    private static long measure(final List<RawGeometryTree> trees, final boolean optimized) {
        final var bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long id = Thread.currentThread().threadId();
        final long before = bean.getThreadAllocatedBytes(id);
        for (int index = 0; index < trees.size(); index++) {
            final RawGeometryTree tree = trees.get(index);
            for (int i = 0; i < 20; i++) {
                sink = optimized ? MixinRawGeometryTreeLookup.getGroupFromHierarchy(tree, "bone1")
                    : RawGeometryTree.getGroupFromHierarchy(tree, "bone1");
            }
        }
        return bean.getThreadAllocatedBytes(id) - before;
    }
}
