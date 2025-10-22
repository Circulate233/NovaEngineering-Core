package github.kasuminova.novaeng.common.hypernet.old.research;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"FeatureEnvy", "unused"})
public record SimpleResearchData(String researchName, float techLevel, double requiredPoints,
                                 double minComputationPointPerTick, List<String> descriptions,
                                 List<String> unlockedDescriptions, List<String> dependencies) implements Serializable {

    public static SimpleResearchData of(ResearchCognitionData data) {
        return new SimpleResearchData(
                data.getTranslatedName().replaceAll("§.", ""),
                data.getTechLevel(),
                data.getRequiredPoints(),
                data.getMinComputationPointPerTick(),
                data.getDescriptions().stream()
                        .map(desc -> desc.replaceAll("§.", ""))
                        .collect(Collectors.toList()),
                data.getUnlockedDescriptions().stream()
                        .map(desc -> desc.replaceAll("§.", ""))
                        .collect(Collectors.toList()),
                data.getDependencies().stream()
                        .map(dep -> dep.getTranslatedName().replaceAll("§.", ""))
                        .collect(Collectors.toList())
        );
    }
}
