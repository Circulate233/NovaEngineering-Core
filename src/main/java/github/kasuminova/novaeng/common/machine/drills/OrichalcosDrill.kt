package github.kasuminova.novaeng.common.machine.drills

object OrichalcosDrill : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.c"
    }

    override fun getMachineName(): String {
        return "orichalcos_drill"
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
