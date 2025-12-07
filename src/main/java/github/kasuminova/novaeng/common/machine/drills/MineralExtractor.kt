package github.kasuminova.novaeng.common.machine.drills

object MineralExtractor : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.a"
    }

    override fun getMachineName(): String {
        return "mineral_extractor"
    }

    override fun getType(): Type {
        return Type.RANGE
    }

    override fun getAdvancedRecipeTimeMultiple(): Float {
        return 1.5f
    }

    override fun getBaseEnergy(): Long {
        return 32768
    }
}
