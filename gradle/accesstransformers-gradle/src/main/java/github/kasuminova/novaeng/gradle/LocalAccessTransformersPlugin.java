package github.kasuminova.novaeng.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Internal;
import org.jspecify.annotations.NonNull;

public final class LocalAccessTransformersPlugin implements Plugin<Project> {
    @Override
    @SuppressWarnings("unchecked")
    public void apply(@NonNull Project project) {
        try {
            ClassLoader loader = getClass().getClassLoader();
            Class<?> parameters = Class.forName(
                "net.minecraftforge.accesstransformers.gradle.ArtifactAccessTransformer$Parameters",
                true,
                loader
            );
            if (!parameters.getMethod("getCachesDir").isAnnotationPresent(Internal.class)) {
                throw new GradleException("The local AccessTransformers cache input override was not loaded.");
            }

            Class<? extends Plugin<Project>> delegate = (Class<? extends Plugin<Project>>) Class.forName(
                "net.minecraftforge.accesstransformers.gradle.AccessTransformersPlugin",
                true,
                loader
            );
            project.getPluginManager().apply(delegate);
        } catch (ReflectiveOperationException e) {
            throw new GradleException("Could not load the Forge AccessTransformers plugin implementation.", e);
        }
    }
}
