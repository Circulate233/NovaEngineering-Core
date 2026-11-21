package github.kasuminova.novaeng.testutil;

import com.cleanroommc.common.CleanroomEnvironment;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes Forge's real SRG-to-MCP remapper for JUnit, which does not run the game launcher.
 */
public final class ForgeReflectionEnvironment {
    private static boolean initialized;

    private ForgeReflectionEnvironment() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        final Path mappings = Path.of(System.getProperty("novaeng.test.srgToNamed"));
        if (!Files.isRegularFile(mappings)) throw new IllegalStateException("Missing generated mappings: " + mappings);
        final String property = "net.minecraftforge.gradle.GradleStart.srg.srg-mcp";
        final String previous = System.getProperty(property);
        System.setProperty(property, mappings.toString());
        try {
            CleanroomEnvironment.setSide(Side.CLIENT);
            final LaunchClassLoader resources = new LaunchClassLoader(new URL[0]) {
                @Override
                public byte[] getClassBytes(final String name) throws IOException {
                    try (final var input = ForgeReflectionEnvironment.class.getClassLoader()
                                                                           .getResourceAsStream(name.replace('.', '/') + ".class")) {
                        return input == null ? null : input.readAllBytes();
                    }
                }
            };
            FMLDeobfuscatingRemapper.INSTANCE.setup(new File("."), resources, "", false);
            initialized = true;
        } finally {
            if (previous == null) System.clearProperty(property);
            else System.setProperty(property, previous);
        }
    }
}
