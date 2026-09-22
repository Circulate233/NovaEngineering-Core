package github.kasuminova.novaeng.common.util;

import com.cleanroommc.discovery.CleanroomModDiscoverer;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import net.minecraft.launchwrapper.Launch;

import java.io.IOException;

public final class MixinDecisions {

    public static final boolean ae2Loaded = isPresent("appliedenergistics2");
    public static final boolean alfheimLoaded = isPresent("alfheim");
    public static final boolean actiniumLoaded = isPresent("actinium");
    public static final boolean arLoaded = isPresent("advancedrocketry");
    public static final boolean avaritiaLoaded = isPresent("avaritia");
    public static final boolean actuallyAdditionsLoaded = isPresent("actuallyadditions");
    public static final boolean astralSorceryLoaded = isPresent("astralsorcery");
    public static final boolean athenaeumLoaded = isPresent("athenaeum");
    public static final boolean betterP2pLoaded = isPresent("betterp2p");
    public static final boolean baseLoaded = isPresent("base");
    public static final boolean biomesOPlentyLoaded = isPresent("biomesoplenty");
    public static final boolean bibliocraftLoaded = isPresent("bibliocraft");
    public static final boolean botaniaLoaded = isPresent("botania");
    public static final boolean cofhLoaded = isPresent("cofhcore");
    public static final boolean craftTweakerLoaded = isPresent("crafttweaker");
    public static final boolean deepMobLearningLoaded = isPresent("deepmoblearning");
    public static final boolean draconicEvolutionLoaded = isPresent("draconicevolution");
    public static final boolean electroblobsLoaded = isPresent("ebwizardry");
    public static final boolean enderioLoaded = isPresent("enderio");
    public static final boolean extrabotanyLoaded = isPresent("extrabotany");
    public static final boolean ic2Loaded = isPresent("ic2");
    public static final boolean immersiveEngineeringLoaded = isPresent("immersiveengineering");
    public static final boolean ingameImeLoaded = isPresent("ingameime");
    public static final boolean jeiLoaded = isPresent("jei");
    public static final boolean jetifLoaded = isPresent("jetif");
    public static final boolean legendaryTooltipsLoaded = isPresent("legendarytooltips");
    public static final boolean libvulpesLoaded = isPresent("libvulpes");
    public static final boolean lootOverhaulLoaded = isPresent("lootoverhaul");
    public static final boolean libNineLoaded = isPresent("libnine");
    public static final boolean metsLoaded = isPresent("mets");
    public static final boolean mekanismLoaded = isPresent("mekanism");
    public static final boolean modularRoutersLoaded = isPresent("modularrouters");
    public static final boolean nae2Loaded = isPresent("nae2");
    public static final boolean nuclearcraftLoaded = isPresent("nuclearcraft");
    public static final boolean packagedAutoLoaded = isPresent("packagedauto");
    public static final boolean psiLoaded = isPresent("psi");
    public static final boolean rftoolsLoaded = isPresent("rftools");
    public static final boolean techgunsLoaded = isPresent("techguns");
    public static final boolean threngLoaded = isPresent("threng");

    private MixinDecisions() {
    }

    public static boolean shouldApply(final String mixinName) {
        final int split = mixinName.indexOf('.');
        if (split < 0) {
            return true;
        }
        if (!isOptimizationEnabled(mixinName)) {
            return false;
        }

        return switch (mixinName.substring(0, split)) {
            case "dme" -> deepMobLearningLoaded && hasClassBytes("mustapelto.deepmoblearning.common.metadata.MetadataManager");
            case "botania_r" -> botaniaLoaded && NovaEngCoreConfig.SERVER.bot;
            case "ae2" -> ae2Loaded;
            case "alfheim" -> alfheimLoaded;
            case "actinium" -> actiniumLoaded;
            case "ar" -> arLoaded;
            case "avaritia" -> avaritiaLoaded;
            case "actuallyadditions" -> actuallyAdditionsLoaded;
            case "astralsorcery" -> astralSorceryLoaded;
            case "athenaeum" -> athenaeumLoaded;
            case "betterp2p" -> betterP2pLoaded;
            case "base" -> baseLoaded && NovaEngCoreConfig.CLIENT.optimizeResourceExistence;
            case "biomesoplenty" -> biomesOPlentyLoaded && NovaEngCoreConfig.CLIENT.optimizeBopFog;
            case "bibliocraft" -> bibliocraftLoaded;
            case "botania" -> botaniaLoaded;
            case "cofh" -> cofhLoaded;
            case "crafttweaker" -> craftTweakerLoaded;
            case "draconicevolution" -> draconicEvolutionLoaded;
            case "electroblobs" -> electroblobsLoaded;
            case "enderio" -> enderioLoaded;
            case "extrabotany" -> extrabotanyLoaded;
            case "ic2" -> ic2Loaded;
            case "immersiveengineering" -> immersiveEngineeringLoaded;
            case "ingameime" -> ingameImeLoaded;
            case "jei" -> jeiLoaded;
            case "jetif" -> jetifLoaded;
            case "legendarytooltips" -> legendaryTooltipsLoaded;
            case "libvulpes" -> libvulpesLoaded;
            case "lootoverhaul" -> lootOverhaulLoaded;
            case "libnine" -> libNineLoaded;
            case "mets" -> metsLoaded;
            case "mekanism" -> mekanismLoaded;
            case "modularrouters" -> modularRoutersLoaded;
            case "nae2" -> nae2Loaded;
            case "nco" -> nuclearcraftLoaded;
            case "packagedauto" -> packagedAutoLoaded;
            case "psi" -> psiLoaded;
            case "rftools" -> rftoolsLoaded;
            case "techguns" -> techgunsLoaded;
            case "threng" -> threngLoaded;
            default -> true;
        };
    }

    private static boolean isOptimizationEnabled(final String mixinName) {
        if (mixinName.startsWith("diagnostic.")) {
            return NovaEngCoreConfig.CLIENT.diagObjModelProbe;
        }
        return switch (mixinName) {
            case "astralsorcery.MixinTexturePreloader",
                 "astralsorcery.MixinBindableResourceLazyAllocation" -> NovaEngCoreConfig.CLIENT.optimizeAstralSorceryTexturePreload;
            case "astralsorcery.MixinAstralSorceryConfig" -> NovaEngCoreConfig.CLIENT.optimizeAstralSorceryConfigSave;
            case "enderio.MixinRecipeFactoryXmlInputFactory" -> NovaEngCoreConfig.CLIENT.optimizeEnderIoXmlFactory;
            case "botania.MixinRenderTileTinyPotatoLazyCosmetics" -> NovaEngCoreConfig.CLIENT.optimizeBotaniaTinyPotato;
            case "bibliocraft.MixinPaintingUtil",
                 "bibliocraft.MixinModelManagerPaintingGeneration" -> NovaEngCoreConfig.CLIENT.optimizeBiblioCraftPaintingCache;
            case "biomesoplenty.MixinTrailManager" -> NovaEngCoreConfig.CLIENT.optimizeBopRemoteTrails;
            case "nco.MixinNCPFWriter" -> NovaEngCoreConfig.CLIENT.optimizeNuclearcraftNcpfExport;
            case "minecraft.MixinAbstractTexture",
                 "minecraft.forge.MixinCloudRenderer" -> NovaEngCoreConfig.CLIENT.optimizeCloudColorUpload;
            case "ic2.MixinBlockStateContainer",
                 "ic2.MixinIc2BlockStateInstance" -> NovaEngCoreConfig.CLIENT.optimizeIc2PropertyTables;
            case "cofh.MixinTransposerRecipeCategoryFill",
                 "cofh.MixinTransposerRecipeCategoryExtract" -> NovaEngCoreConfig.CLIENT.optimizeThermalTransposerRecipes;
            case "contenttweaker.MixinCreativeTabsResourceList",
                 "minecraft.AccessorCreativeTabs" -> NovaEngCoreConfig.CLIENT.optimizeCreativeTabLookup;
            case "journeymap.MixinFileHandler" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapThemeCopy;
            case "minecraft.forge.MixinEventBus" -> NovaEngCoreConfig.CLIENT.optimizeEventBusRegistration;
            case "zenscript.MixinTypeRegistry",
                 "zenscript.MixinJavaMethod" -> NovaEngCoreConfig.CLIENT.optimizeZenScriptCompilation;
            case "minecraft.MixinFallbackResourceManager",
                 "libnine.MixinL9Models",
                 "ae2.MixinUVLModelLoader" -> NovaEngCoreConfig.CLIENT.optimizeModelJsonMetadata;
            case "minecraft.MixinStitcher",
                 "minecraft.MixinStitcherSlot",
                 "minecraft.AccessorStitcherSlot" -> NovaEngCoreConfig.CLIENT.optimizeStitcherFrontier;
            case "actinium.MixinChunk",
                 "actinium.MixinChunkProviderClient",
                 "actinium.MixinEntityGatherer",
                 "actinium.MixinWorldClient" -> NovaEngCoreConfig.CLIENT.optimizeEntityGatherer;
            case "actinium.MixinActiniumWorldRendererLazyBatch" -> NovaEngCoreConfig.CLIENT.optimizeTesrLazyBatch;
            case "actinium.MixinGLStateManagerGenericAttributes",
                 "actinium.MixinShaderManagerGenericAttributes" -> NovaEngCoreConfig.CLIENT.optimizeGenericAttributes;
            case "actinium.MixinGLStateManagerVertexArray",
                 "actinium.MixinImmediateCommandListVertexArray",
                 "actinium.MixinPassThroughGLStateManagerVertexArray" -> NovaEngCoreConfig.CLIENT.optimizeVaoBindings;
            default -> true;
        };
    }

    private static boolean isPresent(final String modId) {
        return CleanroomModDiscoverer.instance().isModPresent(modId);
    }

    private static boolean hasClassBytes(final String className) {
        try {
            return Launch.classLoader.getClassBytes(className) != null;
        } catch (final IOException e) {
            return false;
        }
    }
}
