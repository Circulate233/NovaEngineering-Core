package github.kasuminova.novaeng.common.machine.drills

object DifferentWorld : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.b"
    }

    override fun getMachineName(): String {
        return "different_world"
    }

    override fun getType(): Type {
        return Type.RANGE
    }

    override fun getRecipeTimeMultiple(): Float {
        return 1.5f
    }

    override fun getAdvancedRecipeTimeMultiple(): Float {
        return 1.5f
    }

    override fun getBaseEnergy(): Long {
        return 32768
    }
}
