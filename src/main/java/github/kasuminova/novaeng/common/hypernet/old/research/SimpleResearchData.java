package github.kasuminova.novaeng.common.hypernet.old.research;


import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"FeatureEnvy", "unused"})
public record SimpleResearchData(String researchName, float techLevel, double requiredPoints,
                                 double minComputationPointPerTick, List<String> descriptions,
                                 List<String> unlockedDescriptions, List<String> dependencies) implements Serializable {

    public static SimpleResearchData of(ResearchCognitionData data) {
        final List<String> descriptions = new ArrayList<>(data.getDescriptions().size());
        for (String description : data.getDescriptions()) descriptions.add(description.replaceAll("§.", ""));
        final List<String> unlockedDescriptions = new ArrayList<>(data.getUnlockedDescriptions().size());
        for (String description : data.getUnlockedDescriptions()) unlockedDescriptions.add(description.replaceAll("§.", ""));
        final List<String> dependencies = new ArrayList<>(data.getDependencies().size());
        for (ResearchCognitionData dependency : data.getDependencies())
            dependencies.add(dependency.getTranslatedName().replaceAll("§.", ""));
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
