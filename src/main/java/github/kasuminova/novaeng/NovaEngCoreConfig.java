package github.kasuminova.novaeng;

import com.cleanroommc.configanytime.ConfigAnytime;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID)
public class NovaEngCoreConfig {

    @Config.Name("Client")
    public static Client CLIENT = new Client();

    @Config.Name("Server")
    public static Server SERVER = new Server();

    @Config.Name("MachineAssemblyTool")
    public static MachineAssemblyTool MACHINE_ASSEMBLY_TOOL = new MachineAssemblyTool();

    @Config.RequiresMcRestart
    @Config.Name("javaCheck")
    public static boolean javaCheck = true;

    static {
        ConfigAnytime.register(NovaEngCoreConfig.class);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Objects.equals(event.getModID(), Tags.MOD_ID)) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }

    public static class Client {
        @Config.RequiresMcRestart
        @Config.Name("EnableNovaEngTitle")
        public boolean enableNovaEngTitle = true;

        @Config.RequiresMcRestart
        @Config.Name("爆炸")
        public boolean piece = false;

        @Config.Name("ExtremeCraftingUIModification")
        public boolean ExtremeCraftingUIModification = true;

        @Config.Name("TooltipTintStrength")
        @Config.Comment("把传奇提示的物品配色叠加到 Obscure Tooltips 提示框上的强度：0 为关闭，1 为正片叠底到配色本身。")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double tooltipTintStrength = 1.0D;

        @Config.Name("TooltipFrameOverhang")
        @Config.Comment("传奇提示的装饰对齐 Obscure 外框时使用的外扩像素；-1 为按提示框样式自动探测。")
        @Config.RangeInt(min = -1, max = 64)
        public int tooltipFrameOverhang = -1;
    }

    public static class Server {
        @Config.RequiresMcRestart
        @Config.Name("ForceChunkHandler")
        public boolean forceChunkHandler = true;

        @Config.RequiresMcRestart
        @Config.Name("SpecialMachine")
        public boolean specialMachine = true;

        @Config.RequiresMcRestart
        @Config.Name("bot")
        public boolean bot = true;
    }

    public static class MachineAssemblyTool {
        @Config.Name("BuildQuantity")
        @Config.RangeInt(min = 1)
        public int buildQuantity = 100;

        @Config.Name("BuildSpeed")
        @Config.RangeInt(min = 1)
        public int buildSpeed = 20;
    }
}
