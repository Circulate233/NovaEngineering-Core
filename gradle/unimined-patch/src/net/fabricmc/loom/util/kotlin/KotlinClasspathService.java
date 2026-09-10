package net.fabricmc.loom.util.kotlin;

import org.gradle.api.Project;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

/**
 * 用来顶掉 unimined 内置的同名类（本补丁 jar 只含这一个 class）。
 *
 * <p>背景：unimined 的 {@code ModRemapProvider#constructRemapper} 会调用
 * {@code KotlinClasspathService.getOrCreateIfRequired(project)}，一旦返回非 null，
 * 就给 <b>mod 反混淆</b>用的重映射器挂上 loom 的 Kotlin 元数据改写扩展。而 fugue、
 * CraftTweaker2-API 这类 mod 内含 1.4 之前的 Kotlin 元数据，kotlin-metadata-jvm
 * 拒绝改写（该限制在 2.3.20 / 2.4.0 等所有版本中都存在），导致整个反混淆阶段抛出
 * {@code Failed to remap ... to mcp}，且无法通过调整构建脚本顺序绕过
 * （反混淆发生在 build.gradle 执行完之后）。
 *
 * <p>这里让 {@code getOrCreateIfRequired} 恒返回 null，从而不再启用该扩展。
 * 本项目的 Kotlin 元数据改写需求由 {@code mixinRemap} 自行处理，不依赖此扩展。
 *
 * <p>该 class 被编译进 {@code gradle/unimined-patch/repo} 下的本地 maven 仓库，
 * 并在 settings.gradle 中通过 {@code pluginManagement.resolutionStrategy.eachPlugin}
 * 以独立模块的形式放到插件类路径最前面，从而覆盖原实现。
 */
public class KotlinClasspathService implements KotlinClasspath {

    final Set<URL> classpath;
    final String version;

    public KotlinClasspathService(Set<URL> classpath, String version) {
        this.classpath = classpath;
        this.version = version;
    }

    /** 恒返回 null：不启用 Kotlin 元数据改写扩展。 */
    public static KotlinClasspathService getOrCreateIfRequired(Project project) {
        return null;
    }

    public static synchronized KotlinClasspathService getOrCreate(Project project, String kotlinVersion) {
        return new KotlinClasspathService(Collections.emptySet(), kotlinVersion);
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public Set<URL> classpath() {
        return classpath;
    }
}
