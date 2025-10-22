package github.kasuminova.novaeng.common.config

import com.cleanroommc.configanytime.ConfigAnytime
import github.kasuminova.novaeng.NovaEngineeringCore
import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class NovaEngCoreConfig {

    /*
        必须在最后加载。
    */
    companion object {
        @JvmField
        @Config.Name("Client")
        val CLIENT = Client()
        @JvmField
        @Config.Name("Server")
        val SERVER = Server()
        @JvmField
        @Config.Name("MachineAssemblyTool")
        val MACHINE_ASSEMBLY_TOOL = MachineAssemblyTool()
        @JvmField
        @Config.RequiresMcRestart
        @Config.Name("javaCheck")
        var javaCheck = true

        init {
            ConfigAnytime.register(Companion::class.java)
        }
    }

    @SubscribeEvent
    fun onConfigChanged(event: ConfigChangedEvent.OnConfigChangedEvent) {
        if (event.modID == NovaEngineeringCore.MOD_ID) {
            ConfigManager.sync(NovaEngineeringCore.MOD_ID, Config.Type.INSTANCE)
        }
    }

    class Client {
        @JvmField
        @Config.RequiresMcRestart
        @Config.Name("EnableNovaEngTitle")
        var enableNovaEngTitle = true

        @JvmField
        @Config.RequiresMcRestart
        @Config.Name("爆炸")
        var piece = false
    }

    class Server {
        @JvmField
        @Config.RequiresMcRestart
        @Config.Name("ForceChunkHandler")
        var forceChunkHandler = true

        @JvmField
        @Config.RequiresMcRestart
        @Config.Name("SpecialMachine")
        var specialMachine = true

        @JvmField
        @Config.RequiresMcRestart
        @Config.Name("bot")
        var bot = true
    }

    class MachineAssemblyTool {
        @JvmField
        @Config.Name("BuildQuantity")
        @Config.RangeInt(min = 1, max = 1000)
        var buildQuantity = 60

        @JvmField
        @Config.Name("BuildSpeed")
        @Config.RangeInt(min = 1, max = 1000)
        var buildSpeed = 20
    }
}