package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerAssemblyMixinTest {
    @Test
    void dismantlingAppliesToTheRealControllerWithItsInheritedUpdateMethod() throws Exception {
        final String controller = "hellfirepvp/modularmachinery/common/tiles/base/TileMultiblockMachineController";
        final byte[] bytes = IsolatedMixinTransformer.transform(new String[]{controller}, "MixinControllerAssembly")[0];
        final var transformed = new ClassNode();
        new ClassReader(bytes).accept(transformed, 0);
        assertTrue(transformed.interfaces.contains("github/kasuminova/novaeng/common/assembly/AssemblyController"));
        assertTrue(transformed.methods.stream().anyMatch(method -> method.name.equals("nova$beginDismantling") && method.desc.equals("()Z")));
    }
}
