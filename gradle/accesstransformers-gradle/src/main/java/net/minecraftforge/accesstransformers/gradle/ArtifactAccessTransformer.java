/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.accesstransformers.gradle;

import net.minecraftforge.accesstransformers.gradle.shadow.net.minecraftforge.util.hash.HashStore;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.process.ExecOperations;
import org.gradle.process.ProcessExecutionException;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ArtifactAccessTransformer implements TransformAction<ArtifactAccessTransformer.Parameters> {
    private static final Logger LOGGER = Logging.getLogger(ArtifactAccessTransformer.class);
    private static final Pattern ARG_INJAR = Pattern.compile("{inJar}", Pattern.LITERAL);
    private static final Pattern ARG_ATFILE = Pattern.compile("{atFile}", Pattern.LITERAL);
    private static final Pattern ARG_OUTJAR = Pattern.compile("{outJar}", Pattern.LITERAL);
    private static final Pattern ARG_LOGFILE = Pattern.compile("{logFile}", Pattern.LITERAL);
    private final AccessTransformersProblems problems;

    public ArtifactAccessTransformer() {
        this.problems = this.getObjects().newInstance(AccessTransformersProblems.class);
    }

    public static void validateConfig(Project project, Object dependency, RegularFileProperty config) {
        validateConfig(project.getObjects(), project.getProviders(), dependency, config);
    }

    public static void validateConfig(ObjectFactory objects, ProviderFactory providers, Object dependency, RegularFileProperty config) {
        validateConfig(objects.newInstance(AccessTransformersProblems.class), providers, dependency, config);
    }

    static void validateConfig(AccessTransformersProblems problems, ProviderFactory providers, Object dependency, RegularFileProperty atFileProperty) {
        String dependencyToString = dependency instanceof Dependency d ? Util.toString(d) : dependency.toString();
        RegularFile atFileSource;
        try {
            atFileSource = atFileProperty.get();
        } catch (IllegalStateException e) {
            throw problems.accessTransformerConfigNotDefined(new RuntimeException("Failed to resolve config file property", e), dependencyToString);
        }

        File atFile = atFileSource.getAsFile();
        String atFilePath = atFile.getPath();
        if (!atFile.exists()) {
            throw problems.accessTransformerConfigMissing(new RuntimeException(new FileNotFoundException("AccessTransformer config does not exist at " + atFilePath)), dependencyToString, atFilePath);
        }

        String atFileContents;
        try {
            atFileContents = providers.fileContents(atFileSource).getAsText().get();
        } catch (Throwable e) {
            throw problems.accessTransformerConfigUnreadable(new RuntimeException(new IOException("Failed to read config file at " + atFilePath, e)), dependencyToString, atFilePath);
        }
        if (atFileContents.isBlank()) {
            throw problems.accessTransformerConfigEmpty(new IllegalStateException("Config file must not be blank at " + atFilePath), dependencyToString, atFilePath);
        }
    }

    private static String replace(String value, Map<Pattern, String> substitutions) {
        for (var entry : substitutions.entrySet()) {
            value = entry.getKey().matcher(value).replaceAll(Matcher.quoteReplacement(entry.getValue()));
        }
        return value;
    }

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ExecOperations getExecOperations();

    protected abstract @InputArtifact Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(@NonNull TransformOutputs outputs) {
        try {
            this.exec(outputs);
        } catch (Exception e) {
            LOGGER.error("Failed to apply AccessTransformers to {}", this.getInputArtifact().get().getAsFile().getName(), e);
            throw new RuntimeException(e);
        }
    }

    private void exec(TransformOutputs outputs) throws Exception {
        Parameters parameters = this.getParameters();
        File inJar = this.getInputArtifact().get().getAsFile();
        File atFile = parameters.getConfig().getAsFile().getOrNull();
        if (atFile == null) {
            outputs.file(inJar);
            return;
        }

        String outJarName = inJar.getName().replace(".jar", "-at.jar");
        File outJar = this.file(outJarName);
        File logFile = this.file(outJar.getName() + ".log");
        var cachesDir = parameters.getCachesDir();
        var cache = new HashStore(cachesDir.get().getAsFile())
            .load(cachesDir.file(outJarName + ".cache").get().getAsFile())
            .add("atFile", atFile)
            .add("inJar", inJar)
            .add("outJar", outJar);

        if (outJar.exists() && cache.isSame()) {
            LOGGER.info("Access transformer output up-to-date, skipping transformation for {}", inJar.getName());
        } else {
            LOGGER.info("Access transformer started. Input jar: {}", inJar.getAbsolutePath());
            var result = this.getExecOperations().javaexec(exec -> {
                LogLevel logLevel = parameters.getLogLevel().getOrElse(LogLevel.INFO);
                var stream = Util.toLog(s -> LOGGER.log(logLevel, s));
                exec.setStandardOutput(stream);
                exec.setErrorOutput(stream);
                exec.setExecutable(parameters.getJavaLauncher().get());
                exec.setClasspath(parameters.getClasspath());
                Util.setOptional(exec.getMainClass(), parameters.getMainClass());

                var substitutions = Map.of(
                    ARG_INJAR, inJar.getAbsolutePath(),
                    ARG_ATFILE, atFile.getAbsolutePath(),
                    ARG_OUTJAR, outJar.getAbsolutePath(),
                    ARG_LOGFILE, logFile.getAbsolutePath()
                );
                exec.setArgs(parameters.getArgs().get().stream()
                                       .map(arg -> replace(arg, substitutions))
                                       .collect(Collectors.toList()));
            });

            try {
                result.rethrowFailure().assertNormalExitValue();
            } catch (ProcessExecutionException e) {
                throw this.problems.accessTransformerFailed(e, inJar, atFile, logFile);
            }

            LOGGER.info("Access transformer completed. Output jar: {}", outJar.getAbsolutePath());
            cache.add("outJar", outJar).save();
        }

        var output = outputs.file(outJarName).toPath();
        try {
            Files.copy(outJar.toPath(), output, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw this.problems.accessTransformerCannotWriteOutput(e, output);
        }
    }

    private File file(String path) {
        return this.getParameters().getCachesDir().file(path).map(this.problems.ensureFileLocation()).get().getAsFile();
    }

    public interface Parameters extends TransformParameters {
        static Action<? super Parameters> defaults(Project project) {
            return defaults(project, parameters -> {
            });
        }

        static Action<? super Parameters> defaults(Project project, Action<? super Parameters> action) {
            AccessTransformersPlugin plugin = project.getPlugins().getPlugin(AccessTransformersPlugin.class);
            return parameters -> {
                parameters.getClasspath().from(plugin.getTool(Tools.ACCESSTRANSFORMERS));
                parameters.getMainClass().value(Tools.ACCESSTRANSFORMERS.getMainClass());
                parameters.getJavaLauncher().value(Util.launcherFor(project, Tools.ACCESSTRANSFORMERS.getJavaVersion()).map(Util.LAUNCHER_EXECUTABLE));
                parameters.getArgs().value(Constants.ACCESSTRANSFORMERS_DEFAULT_ARGS);
                parameters.getCachesDir().value(plugin.localCaches());
                action.execute(parameters);
            };
        }

        @InputFile
        RegularFileProperty getConfig();

        @Console
        Property<LogLevel> getLogLevel();

        @InputFiles
        @Classpath
        ConfigurableFileCollection getClasspath();

        @Input
        @Optional
        Property<String> getMainClass();

        @Input
        Property<String> getJavaLauncher();

        @Input
        ListProperty<String> getArgs();

        // This directory is written by the transform itself and must not affect its input fingerprint.
        @Internal
        DirectoryProperty getCachesDir();
    }
}
