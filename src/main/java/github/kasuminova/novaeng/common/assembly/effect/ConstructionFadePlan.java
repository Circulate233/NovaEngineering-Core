package github.kasuminova.novaeng.common.assembly.effect;

final class ConstructionFadePlan {
    private static final int WHOLE_STRUCTURE_LIMIT = 5_000;
    private static final int TARGET_CELLS = 3_000;

    private final int[] groups;
    private final int size;

    private ConstructionFadePlan(int[] groups, int size) {
        this.groups = groups;
        this.size = size;
    }

    static ConstructionFadePlan create(int[] counts) {
        long total = 0;
        int remainingGroups = 0;
        for (int count : counts) {
            total += count;
            if (count > 0) remainingGroups++;
        }

        int[] groups = new int[counts.length];
        if (total <= WHOLE_STRUCTURE_LIMIT || remainingGroups == 1) return new ConstructionFadePlan(groups, 1);

        int size = Math.clamp(Math.round((double) total / TARGET_CELLS), 2, remainingGroups);
        int stage = 0;
        long cells = 0;
        long stageStart = 0;
        for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            if (count > 0) {
                if (stage + 1 < size && cells > stageStart) {
                    // Cut at the closest physical boundary, leaving a nonempty group for every later stage.
                    long ideal = total * (stage + 1);
                    long before = Math.abs(ideal - cells * size);
                    long after = Math.abs(ideal - (cells + count) * size);
                    if (remainingGroups == size - stage - 1 || before <= after) {
                        stage++;
                        stageStart = cells;
                    }
                }
                cells += count;
                remainingGroups--;
            }
            groups[i] = stage;
        }
        return new ConstructionFadePlan(groups, size);
    }

    int group(int physicalGroup) {
        return groups[physicalGroup];
    }

    int size() {
        return size;
    }
}
