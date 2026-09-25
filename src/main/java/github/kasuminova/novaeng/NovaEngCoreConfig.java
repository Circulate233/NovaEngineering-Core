package github.kasuminova.novaeng;

import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

@SuppressWarnings("CanBeFinal")
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID)
public class NovaEngCoreConfig {

    @Config.Name("Client")
    public static Client CLIENT = new Client();

    @Config.Name("Server")
    public static Server SERVER = new Server();

    @Config.Name("MachineAssemblyTool")
    public static MachineAssemblyTool MACHINE_ASSEMBLY_TOOL = new MachineAssemblyTool();

    static {
        ConfigManager.register(NovaEngCoreConfig.class);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Objects.equals(event.getModID(), Tags.MOD_ID)) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }

    @SuppressWarnings("CanBeFinal")
    public static class Client {
        @Config.RequiresMcRestart
        @Config.Name("EnableNovaEngTitle")
        public boolean enableNovaEngTitle = true;

        @Config.Name("EnableFullbright")
        public boolean enableFullbright = true;

        @Config.Name("OptimizeBopFog")
        public boolean optimizeBopFog = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAstralSorceryTexturePreload")
        @Config.Comment("Load Astral Sorcery textures on first use while preserving resource reload invalidation.")
        public boolean optimizeAstralSorceryTexturePreload = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAstralSorceryConfigSave")
        @Config.Comment("Skip unchanged Astral Sorcery configuration writes.")
        public boolean optimizeAstralSorceryConfigSave = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeBopRemoteTrails")
        @Config.Comment("Skip Biomes O' Plenty's synchronous remote trail download.")
        public boolean optimizeBopRemoteTrails = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeNuclearcraftNcpfExport")
        @Config.Comment("Skip NuclearCraft's unused generated NCPF export.")
        public boolean optimizeNuclearcraftNcpfExport = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEnderIoXmlFactory")
        @Config.Comment("Reuse Ender IO's single-threaded XML input factory while keeping readers per document.")
        public boolean optimizeEnderIoXmlFactory = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeBotaniaTinyPotato")
        @Config.Comment("Defer Tiny Potato cosmetic stack construction until its first render.")
        public boolean optimizeBotaniaTinyPotato = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeBiblioCraftPaintingCache")
        @Config.Comment("Reuse BiblioCraft painting jar scans within one resource reload.")
        public boolean optimizeBiblioCraftPaintingCache = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCloudColorUpload")
        @Config.Comment("Skip unchanged Forge cloud color texture uploads with exact GL lifetime tracking.")
        public boolean optimizeCloudColorUpload = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeIc2PropertyTables")
        @Config.Comment("Skip vanilla block-state transition tables unused by IC2's indexed states.")
        public boolean optimizeIc2PropertyTables = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeModelJsonMetadata")
        @Config.Comment("Share static model JSON parsing between LibNine and AE2 UVL probes.")
        public boolean optimizeModelJsonMetadata = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeResourceExistence")
        @Config.Comment("Memoise directory resource pack file probes for the duration of one model reload.")
        public boolean optimizeResourceExistence = true;

        @Config.RequiresMcRestart
        @Config.Name("DiagObjModelProbe")
        @Config.Comment("Diagnostic only. Count OBJModel construction and wrapping sources per .obj path, dumped at the end of each model reload. Changes no behaviour.")
        public boolean diagObjModelProbe = false;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeThermalTransposerRecipes")
        @Config.Comment("Skip Thermal Expansion's JEI transposer fill and extract recipe generation.")
        public boolean optimizeThermalTransposerRecipes = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCreativeTabLookup")
        @Config.Comment("Memoise ContentTweaker's creative tab label lookup. Applies on both sides.")
        public boolean optimizeCreativeTabLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJourneymapThemeCopy")
        @Config.Comment("Skip JourneyMap's theme resource unpack while its mod jar is unchanged.")
        public boolean optimizeJourneymapThemeCopy = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEventBusRegistration")
        @Config.Comment("Answer Forge's event registration lookups without throwing on every miss. Applies on both sides.")
        public boolean optimizeEventBusRegistration = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeZenScriptCompilation")
        @Config.Comment("Use a fastutil type table and cached method metadata while compiling scripts. Applies on both sides.")
        public boolean optimizeZenScriptCompilation = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEntityGatherer")
        @Config.Comment("Index client chunks that can contain entities for Actinium gathering.")
        public boolean optimizeEntityGatherer = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeTesrLazyBatch")
        @Config.Comment("Open Actinium TESR batch state only when the current pass renders a tile entity.")
        public boolean optimizeTesrLazyBatch = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeGenericAttributes")
        @Config.Comment("Suppress redundant COLOR and SECONDARY_UV generic attribute uploads.")
        public boolean optimizeGenericAttributes = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeVaoBindings")
        @Config.Comment("Suppress proven redundant VAO binds with raw-path invalidation.")
        public boolean optimizeVaoBindings = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeReusableVBOUploader")
        @Config.Comment("Upload only finished MMCE vertex bytes when a reusable client-array buffer has excess capacity.")
        public boolean optimizeReusableVBOUploader = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeClientBiomeLookup")
        @Config.Comment("Resolve client biome lookups with one chunk query instead of a residency check plus another lookup.")
        public boolean optimizeClientBiomeLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeClientChunkLookup")
        @Config.Comment("Resolve client chunks with one primitive-map lookup and without a null wrapper allocation.")
        public boolean optimizeClientChunkLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeNae2BeamCube")
        @Config.Comment("Build NAE2 beam cube faces without repeating set membership checks or color unpacking.")
        public boolean optimizeNae2BeamCube = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeClientChunkPresence")
        @Config.Comment("Test client chunk presence with one loaded-chunk map query.")
        public boolean optimizeClientChunkPresence = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeTextureBind")
        @Config.Comment("Use fastutil maps in TextureManager and skip consecutive identical binds.")
        public boolean optimizeTextureBind = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeItemLightingBuffers")
        @Config.Comment("Reuse constant item-lighting float buffers instead of rewriting one shared buffer.")
        public boolean optimizeItemLightingBuffers = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeTesrRendererLookup")
        @Config.Comment("Use a reference-equality map for TESR class-to-renderer lookup.")
        public boolean optimizeTesrRendererLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeBufferBuilderEndian")
        @Config.Comment("Reuse process endianness for packed BufferBuilder color writes.")
        public boolean optimizeBufferBuilderEndian = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEntityDataMaps")
        @Config.Comment("Use primitive and reference fastutil maps for entity data manager lookups.")
        public boolean optimizeEntityDataMaps = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeItemModelMaps")
        @Config.Comment("Use primitive and reference fastutil maps for item model mesher lookups.")
        public boolean optimizeItemModelMaps = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeTrsrCenterCache")
        @Config.Comment("Cache TRSR corner-to-center conversions on the source transform instance.")
        public boolean optimizeTrsrCenterCache = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeClientFastutilMaps")
        @Config.Comment("Replace remaining client HashMaps for entity renderers, texture atlases, and item tool classes with fastutil maps.")
        public boolean optimizeClientFastutilMaps = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeInventoryTickWork")
        @Config.Comment("Defer the inventory_changed trigger to one pass per tick (a criterion satisfied only for part of one tick is no longer granted) and skip the per-tick map walk while the player holds none.")
        public boolean optimizeInventoryTickWork = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeIntHashMap")
        @Config.Comment("Replace vanilla IntHashMap chaining with a primitive fastutil map. Applies on both sides.")
        public boolean optimizeIntHashMap = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeItemOverrideList")
        @Config.Comment("Skip empty item-override matching and reuse the immutable override list.")
        public boolean optimizeItemOverrideList = true;

    }

    @SuppressWarnings("CanBeFinal")
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
