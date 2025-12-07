package github.kasuminova.novaeng.common.machine.drills

object VoidMiner : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.b"
    }

    override fun getMachineName(): String {
        return "void_miner"
    }

    override fun getType(): Type {
        return Type.SINGLE
    }

    override fun getBaseEnergy(): Long {
        return 24576
    }

    override fun getRecipeTimeMultiple(): Float {
        return 1.5f
    }

    override fun isDimensional(): Boolean {
        return true
    }
}
