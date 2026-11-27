package github.kasuminova.novaeng.common.hypernet.old.research;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.Serializable;
import java.util.List;
import java.util.RandomAccess;

@SuppressWarnings({"FeatureEnvy", "unused"})
public record SimpleResearchData(String researchName, float techLevel, double requiredPoints,
                                 double minComputationPointPerTick, List<String> descriptions,
                                 List<String> unlockedDescriptions, List<String> dependencies) implements Serializable {

    public static SimpleResearchData of(ResearchCognitionData data) {
        final List<String> descriptions = new ObjectArrayList<>(data.getDescriptions().size());
        final List<String> sourceDescriptions = data.getDescriptions();
        if (sourceDescriptions instanceof RandomAccess) {
            for (int index = 0; index < sourceDescriptions.size(); index++)
                descriptions.add(sourceDescriptions.get(index).replaceAll("§.", ""));
        } else {
            for (String description : sourceDescriptions) descriptions.add(description.replaceAll("§.", ""));
        }
        final List<String> unlockedDescriptions = new ObjectArrayList<>(data.getUnlockedDescriptions().size());
        final List<String> sourceUnlockedDescriptions = data.getUnlockedDescriptions();
        if (sourceUnlockedDescriptions instanceof RandomAccess) {
            for (int index = 0; index < sourceUnlockedDescriptions.size(); index++)
                unlockedDescriptions.add(sourceUnlockedDescriptions.get(index).replaceAll("§.", ""));
        } else {
            for (String description : sourceUnlockedDescriptions) unlockedDescriptions.add(description.replaceAll("§.", ""));
        }
        final List<String> dependencies = new ObjectArrayList<>(data.getDependencies().size());
        final List<ResearchCognitionData> sourceDependencies = data.getDependencies();
        if (sourceDependencies instanceof RandomAccess) {
            for (int index = 0; index < sourceDependencies.size(); index++)
                dependencies.add(sourceDependencies.get(index).getTranslatedName().replaceAll("§.", ""));
        } else {
            for (ResearchCognitionData dependency : sourceDependencies)
                dependencies.add(dependency.getTranslatedName().replaceAll("§.", ""));
        }
        return new SimpleResearchData(
            data.getTranslatedName().replaceAll("§.", ""),
            data.getTechLevel(),
            data.getRequiredPoints(),
            data.getMinComputationPointPerTick(),
            descriptions,
            unlockedDescriptions,
            dependencies
        );
    }
}
