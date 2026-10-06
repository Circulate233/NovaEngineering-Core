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
    public static final boolean codeChickenLibLoaded = isPresent("codechickenlib");
    public static final boolean craftTweakerLoaded = isPresent("crafttweaker");
    public static final boolean customLoadingScreenLoaded = isPresent("customloadingscreen");
    public static final boolean deepMobLearningLoaded = isPresent("deepmoblearning");
    public static final boolean draconicAdditionsLoaded = isPresent("draconicadditions");
    public static final boolean draconicEvolutionLoaded = isPresent("draconicevolution");
    public static final boolean electroblobsLoaded = isPresent("ebwizardry");
    public static final boolean enderioLoaded = isPresent("enderio");
    public static final boolean extrabotanyLoaded = isPresent("extrabotany");
    public static final boolean ic2Loaded = isPresent("ic2");
    public static final boolean immersiveEngineeringLoaded = isPresent("immersiveengineering");
    public static final boolean ingameImeLoaded = isPresent("ingameime");
    public static final boolean jeiLoaded = isPresent("jei");
    public static final boolean jetifLoaded = isPresent("jetif");
    public static final boolean journeymapLoaded = isPresent("journeymap");
    public static final boolean legendaryTooltipsLoaded = isPresent("legendarytooltips");
    public static final boolean librarianLibLoaded = isPresent("librarianlib");
    public static final boolean libvulpesLoaded = isPresent("libvulpes");
    public static final boolean lootOverhaulLoaded = isPresent("lootoverhaul");
    public static final boolean libNineLoaded = isPresent("libnine");
    public static final boolean metsLoaded = isPresent("mets");
    public static final boolean mekanismLoaded = isPresent("mekanism");
    public static final boolean ctmLoaded = isPresent("ctm");
    public static final boolean mmceLoaded = isPresent("modularmachinery");
    public static final boolean modularRoutersLoaded = isPresent("modularrouters");
    public static final boolean nae2Loaded = isPresent("nae2");
    public static final boolean nuclearcraftLoaded = isPresent("nuclearcraft");
    public static final boolean packagedAutoLoaded = isPresent("packagedauto");
    public static final boolean packagedAstralLoaded = isPresent("packagedastral");
    public static final boolean psiLoaded = isPresent("psi");
    public static final boolean rftoolsLoaded = isPresent("rftools");
    public static final boolean techgunsLoaded = isPresent("techguns");
    public static final boolean threngLoaded = isPresent("threng");
    public static final boolean circulationNetworksLoaded = isPresent("circulation_networks");
    public static final boolean universalTweaksLoaded = isPresent("universaltweaks");

    private MixinDecisions() {
    }

    public static boolean deCoreBindingEnabled() {
        return mmceLoaded && circulationNetworksLoaded && draconicEvolutionLoaded
            && NovaEngCoreConfig.SERVER.optimizeDECoreHatchScan;
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
            case "draconicadditions" -> draconicAdditionsLoaded;
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
            case "ctm" -> ctmLoaded;
            case "mmce" -> mmceLoaded;
            case "modularrouters" -> modularRoutersLoaded;
            case "nae2" -> nae2Loaded;
            case "nco" -> nuclearcraftLoaded;
            case "packagedauto" -> packagedAutoLoaded;
            case "psi" -> psiLoaded;
            case "rftools" -> rftoolsLoaded;
            case "techguns" -> techgunsLoaded;
            case "threng" -> threngLoaded;
            case "universaltweaks" -> universalTweaksLoaded;
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
            case "astralsorcery.MixinTileAccelerationBlacklist" ->
                NovaEngCoreConfig.SERVER.optimizeHorologiumAccelerationBlacklist;
            case "enderio.MixinRecipeFactoryXmlInputFactory" -> NovaEngCoreConfig.CLIENT.optimizeEnderIoXmlFactory;
            case "botania.MixinRenderTileTinyPotatoLazyCosmetics" -> NovaEngCoreConfig.CLIENT.optimizeBotaniaTinyPotato;
            case "bibliocraft.MixinPaintingUtil" -> NovaEngCoreConfig.CLIENT.optimizeBiblioCraftPaintingCache;
            case "biomesoplenty.MixinTrailManager" -> NovaEngCoreConfig.CLIENT.optimizeBopRemoteTrails;
            case "nco.MixinNCPFWriter" -> NovaEngCoreConfig.CLIENT.optimizeNuclearcraftNcpfExport;
            case "minecraft.MixinAbstractTexture",
                 "minecraft.forge.MixinCloudRenderer" -> NovaEngCoreConfig.CLIENT.optimizeCloudColorUpload;
            case "ic2.MixinBlockStateContainer",
                 "ic2.MixinIc2BlockStateInstance" -> NovaEngCoreConfig.CLIENT.optimizeIc2PropertyTables;
            case "cofh.MixinTransposerRecipeCategoryFill",
                 "cofh.MixinTransposerRecipeCategoryExtract" -> NovaEngCoreConfig.CLIENT.optimizeThermalTransposerRecipes;
            case "customloadingscreen.MixinMinecraftDisplayerRenderer" ->
                customLoadingScreenLoaded && NovaEngCoreConfig.CLIENT.optimizeCustomLoadingScreen;
            case "codechickenlib.MixinCCBlockStateLoader" ->
                codeChickenLibLoaded && NovaEngCoreConfig.CLIENT.optimizeCodeChickenBlockstateScan;
            case "codechickenlib.MixinModelBakeryErrorState" ->
                codeChickenLibLoaded && NovaEngCoreConfig.CLIENT.optimizeCodeChickenErrorStateLog;
            case "minecraft.MixinResourceLocation" ->
                NovaEngCoreConfig.CLIENT.optimizeResourceLocationStableValue;
            case "librarianlib.MixinSavingFieldCache" ->
                librarianLibLoaded && NovaEngCoreConfig.CLIENT.optimizeLibrarianLibFieldScan;
            case "jei.MixinElementSearch" -> NovaEngCoreConfig.CLIENT.optimizeJeiAsyncSearchIndex;
            case "jei.MixinStackHelper" -> NovaEngCoreConfig.CLIENT.optimizeJeiStackUidCache;
            case "jei.MixinItemStackListFactory" -> NovaEngCoreConfig.CLIENT.optimizeJeiItemList;
            case "contenttweaker.MixinCreativeTabsResourceList",
                 "minecraft.AccessorCreativeTabs" -> NovaEngCoreConfig.CLIENT.optimizeCreativeTabLookup;
            case "journeymap.MixinFileHandler" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapThemeCopy;
            case "konkrete.MixinLocalsCopy" ->
                hasClassBytes("de.keksuccino.konkrete.localization.Locals")
                    && NovaEngCoreConfig.CLIENT.optimizeKonkreteLocalsCopy;
            case "journeymap.MixinJMChunkCache" ->
                journeymapLoaded && NovaEngCoreConfig.CLIENT.optimizeJourneymapChunkDrain;
            case "journeymap.MixinJMChunkStorage" ->
                journeymapLoaded && NovaEngCoreConfig.CLIENT.optimizeJourneymapChunkBuffer;
            case "journeymap.MixinVanillaBlockSpriteProxy" ->
                journeymapLoaded && (NovaEngCoreConfig.CLIENT.optimizeJourneymapBlockSprites
                    || NovaEngCoreConfig.CLIENT.optimizeJourneymapSpriteDedupe);
            case "minecraft.forge.MixinEventBus" -> NovaEngCoreConfig.CLIENT.optimizeEventBusRegistration;
            case "zenscript.MixinTypeRegistry",
                 "zenscript.MixinJavaMethod" -> NovaEngCoreConfig.CLIENT.optimizeZenScriptCompilation;
            case "minecraft.MixinFallbackResourceManager" -> NovaEngCoreConfig.CLIENT.optimizeModelJsonMetadata;
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
            case "minecraft.MixinTileEntityRendererDistance" ->
                NovaEngCoreConfig.CLIENT.optimizeTesrRenderDistance;
            case "minecraft.MixinClippingHelperFrustumCapture" ->
                NovaEngCoreConfig.CLIENT.optimizeTesrFrustumCulling;
            case "minecraft.MixinInventoryChangeTrigger$Instance",
                 "minecraft.MixinInventoryChangeTrigger$Listeners",
                 "minecraft.MixinInventoryPlayer",
                 "minecraft.MixinEntityPlayerMPInventoryWork" -> NovaEngCoreConfig.CLIENT.optimizeInventoryTickWork;
            case "minecraft.MixinNonNullList" -> NovaEngCoreConfig.CLIENT.optimizeNonNullListClear;
            case "biomesoplenty.MixinFogEventHandlerColorCache" -> NovaEngCoreConfig.CLIENT.optimizeBopFog;
            case "mmce.MixinTileEnergyHatchDEBinding",
                 "draconicevolution.MixinTileEnergyStorageCoreBinding" -> deCoreBindingEnabled();
            case "draconicevolution.MixinRenderTileReactorCoreGeometry",
                 "draconicadditions.MixinRenderTileChaosStabilizerGeometry" ->
                draconicEvolutionLoaded && draconicAdditionsLoaded
                    && NovaEngCoreConfig.CLIENT.optimizeDraconicModelGeometry;
            case "minecraft.forge.MixinASMEventHandler",
                 "minecraft.forge.MixinEventBusPost" -> NovaEngCoreConfig.SERVER.optimizeForgeEventDispatch;
            case "minecraft.MixinWorldEntitySpawner" ->
                NovaEngCoreConfig.SERVER.optimizeSpawnListResolution;
            case "minecraft.forge.MixinFMLOutboundHandlerReply" ->
                NovaEngCoreConfig.SERVER.guardForgeNetworkReplyTarget;
            case "mekanism.MixinEnergyNetworkTargetPool",
                 "mekanism.MixinGasNetworkTargetPool",
                 "mekanism.MixinTargetReset" -> NovaEngCoreConfig.SERVER.optimizeMekanismNetworkTargets;
            case "mekanism.MixinTransmitterModelQuadsCache" ->
                NovaEngCoreConfig.CLIENT.optimizeMekanismObjQuads;
            case "mekanism.MixinModuleHelperModuleCache" ->
                NovaEngCoreConfig.CLIENT.optimizeMekanismModuleLookup;
            case "ctm.MixinTextureCTMConnectToCache" -> ctmLoaded;
            case "ctm.MixinDirConnectionPosCache" ->
                ctmLoaded && NovaEngCoreConfig.CLIENT.optimizeChiselCtmConnectionOffsets;
            case "minecraft.MixinClassInheritanceMultiMapIterator" ->
                NovaEngCoreConfig.CLIENT.optimizeEntityLookupIteration;
            case "minecraft.MixinBlockStatePaletteLinear",
                 "minecraft.MixinBlockStatePaletteHashMap" -> NovaEngCoreConfig.SERVER.optimizeChunkPaletteGrowth;
            case "minecraft.MixinMapGenStructure" -> NovaEngCoreConfig.SERVER.optimizeStructureLookup;
            case "minecraft.MixinBiome",
                 "minecraft.forge.MixinForgeRegistry" -> NovaEngCoreConfig.SERVER.optimizeBiomeIdLookup;
            case "ae2.MixinItemListCapacity" -> NovaEngCoreConfig.SERVER.optimizeAe2ItemListCapacity;
            case "universaltweaks.MixinUTEntityDesync" -> NovaEngCoreConfig.SERVER.optimizeUniversalTweaksEntityBlacklist;
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
