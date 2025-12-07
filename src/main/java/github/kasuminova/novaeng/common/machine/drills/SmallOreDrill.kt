package github.kasuminova.novaeng.common.machine.drills

object SmallOreDrill : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.a"
    }

    override fun getMachineName(): String {
        return "small_ore_drill"
    }

    override fun getType(): Type {
        return Type.SINGLE
    }

    override fun getBaseEnergy(): Long {
        return 24576
    }
}
