package github.kasuminova.novaeng.common.machine.drills

object ManaOreDrill : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.c"
    }

    override fun getMachineName(): String {
        return "mana_ore_drill"
    }

    override fun getType(): Type {
        return Type.SINGLE
    }

    override fun getBaseEnergy(): Long {
        return 24576
    }
}
