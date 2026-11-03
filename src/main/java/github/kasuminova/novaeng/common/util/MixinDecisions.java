package github.kasuminova.novaeng.common.util;

import com.cleanroommc.discovery.CleanroomModDiscoverer;
import github.kasuminova.novaeng.NovaEngCoreConfig;

public final class MixinDecisions {

    public static final boolean ae2Loaded = isPresent("appliedenergistics2");
    public static final boolean actiniumLoaded = isPresent("actinium");
    public static final boolean arLoaded = isPresent("advancedrocketry");
    public static final boolean enderioLoaded = isPresent("enderio");
    public static final boolean threngLoaded = isPresent("threng");
    public static final boolean avaritiaLoaded = isPresent("avaritia");
    public static final boolean astralSorceryLoaded = isPresent("astralsorcery");
    public static final boolean bibliocraftLoaded = isPresent("bibliocraft");
    public static final boolean botaniaLoaded = isPresent("botania");
    public static final boolean cofhLoaded = isPresent("cofhcore");
    public static final boolean cofhWorldLoaded = isPresent("cofhworld");
    public static final boolean deepMobLearningLoaded = isPresent("deepmoblearning");
    public static final boolean reidLoaded = isPresent("jeid") || isPresent("reid");
    public static final boolean draconicAdditionsLoaded = isPresent("draconicadditions");
    public static final boolean draconicEvolutionLoaded = isPresent("draconicevolution");
    public static final boolean ic2Loaded = isPresent("ic2");
    public static final boolean immersiveEngineeringLoaded = isPresent("immersiveengineering");
    public static final boolean neoFontRenderUiLoaded = isPresent("neofontrender_ui_enhancements");
    public static final boolean neoFontRenderLoaded = isPresent("neofontrender");
    public static final boolean rpsIdeasLoaded = isPresent("rpsideas");
    public static final boolean ctmLoaded = isPresent("ctm");
    public static final boolean mmceLoaded = isPresent("modularmachinery");
    public static final boolean packagedAutoLoaded = isPresent("packagedauto");
    public static final boolean packagedAstralLoaded = isPresent("packagedastral");
    public static final boolean circulationNetworksLoaded = isPresent("circulation_networks");
    public static final boolean stellarCoreLoaded = isPresent("stellar_core");

    private MixinDecisions() {
    }

    public static boolean deCoreBindingEnabled() {
        return mmceLoaded && circulationNetworksLoaded && draconicEvolutionLoaded
            && NovaEngCoreConfig.SERVER.optimizeDECoreHatchScan;
    }

    public static boolean recipeContextPoolEnabled() {
        return mmceLoaded && NovaEngCoreConfig.PERFORMANCE.boundedRecipeContexts;
    }

    public static boolean aeOutputPreparationEnabled() {
        return mmceLoaded && ae2Loaded && NovaEngCoreConfig.PERFORMANCE.batchAe2Outputs;
    }

    public static boolean shouldApply(final String mixinName) {
        switch (mixinName) {
            case "minecraft.forge.MixinOreRecipeRevision" -> {
                return true;
            }
            case "mmce.MixinOredictCache", "mmce.MixinCraftingCheckResultAllocation" -> {
                return mmceLoaded;
            }
            case "ae2.AccessorItemVariantRecords", "ae2.MixinItemListBulkIteration",
                 "ae2.MixinItemListIgnoreCraftingCopies", "ae2.MixinMECraftingInventoryIteration" -> {
                return ae2Loaded;
            }
            case "neofontrender.AccessorSignOcclusionCuller" -> {
                return neoFontRenderLoaded;
            }
            case "neofontrender.MixinCosmicCacheLifecycle" -> {
                return neoFontRenderLoaded;
            }
            case "geckolib3.MixinAnimationControllerQueues", "geckolib3.MixinAnimationProcessorTrackers",
                 "geckolib3.MixinAnimationTrackerScope", "geckolib3.MixinAnimationFactoryIds" -> {
                return isPresent("geckolib3");
            }
            case "rpsideas.AccessorItemBioticSensor" -> {
                return rpsIdeasLoaded;
            }
            case "konkrete.MixinStringUtils" -> {
                return isPresent("konkrete");
            }
            case "neofontrender.MixinVanillaGuiTreeSession" -> {
                return neoFontRenderUiLoaded;
            }
            case "minecraft.MixinRegionRenderCacheBuilderCapacity" -> {
                return actiniumLoaded;
            }
            case "lumenized.MixinBloomRenderLayerCapacity" -> {
                return actiniumLoaded && isPresent("lumenized");
            }
            case "mmce.MixinBufferBuilderPoolMemory", "mmce.MixinReusableVBOUploaderGrowth",
                 "mmce.MixinControllerModelTaskRetirement" -> {
                return mmceLoaded && isPresent("geckolib3");
            }
            case "ae2.MixinMeteorWriteBatch" -> {
                return ae2Loaded && NovaEngCoreConfig.SERVER.optimizeMeteorSpawnDataIo && NovaEngCoreConfig.SERVER.batchMeteorSpawnWrites;
            }
        }
        final Boolean worldgenAuxiliary = switch (mixinName) {
            case "artemislib.MixinAttachAttributes" -> isPresent("artemislib");
            case "astralsorcery.MixinAttributeOwner" -> astralSorceryLoaded;
            case "mekanism.MixinWorldgenFactories" -> isPresent("mekanism");
            case "actuallyadditions.MixinLushCaveAllocation" -> isPresent("actuallyadditions");
            case "cofh.MixinFlatBedrock" -> cofhWorldLoaded;
            case "circulation.MixinPocketHostEmpty" -> circulationNetworksLoaded;
            case "wizardry.MixinStructurePlacementChoices" -> isPresent("ebwizardry");
            case "chisel.MixinBasaltScan" -> isPresent("chisel");
            case "bettermineshafts.MixinBiomeTypes" -> isPresent("bettermineshafts");
            case "biomesoplenty.MixinGenerationStages", "biomesoplenty.MixinGeneratorStageRevision" ->
                isPresent("biomesoplenty");
            case "tconstruct.MixinIslandList", "tconstruct.MixinIslandQueries" -> isPresent("tconstruct");
            case "minecraft.MixinLeafBlockKind", "minecraft.MixinPathStateMemo",
                 "minecraft.MixinPathNodeClassification", "minecraft.MixinWorldCollisionReads",
                 "minecraft.MixinWorldEmptyLightListener", "minecraft.MixinLazyAttributeOperations",
                 "minecraft.MixinBiomeCacheLastEntry", "minecraft.MixinLowerStringMapNames" -> true;
            default -> null;
        };
        if (worldgenAuxiliary != null) return NovaEngCoreConfig.SERVER.optimizeWorldgenAuxiliaries && worldgenAuxiliary;
        switch (mixinName) {
            case "actuallyadditions.MixinWorldgenOreFactory" -> {
                return isPresent("actuallyadditions");
            }
            case "bettermeteor.MixinCompassTargetScan" -> {
                return NovaEngCoreConfig.SERVER.optimizeMeteorCompassTargetScan && ae2Loaded && isPresent("better_meteorite");
            }
            case "kirino.InvokerArrayRange" -> {
                return NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps;
            }
            case "architecturecraft.InvokerMessageSide" -> {
                return NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps && isPresent("architecturecraft");
            }
            case "minecraft.forge.AccessorFluidRegistryIds", "minecraft.forge.InvokerFluidRegistryDelegate" -> {
                return NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps;
            }
        }
        if (mixinName.endsWith("PrimitiveBiMap")) {
            if (!NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps) return false;
            final String owner = mixinName.substring(0, mixinName.indexOf('.'));
            return switch (owner) {
                case "minecraft", "kirino" -> true;
                case "cofh" -> cofhLoaded;
                case "libvulpes" -> isPresent("libvulpes");
                default -> isPresent(owner);
            };
        }

        final Boolean startupEnabled = switch (mixinName) {
            case "minecraft.MixinDynamicTextureUploadBinding" -> actiniumLoaded
                && NovaEngCoreConfig.CLIENT.optimizeAtlasUploadBinding;
            case "journeymap.MixinThemeTexturePreload", "journeymap.MixinWidgetTexturePreload" ->
                isPresent("journeymap");
            case "techguns.MixinTemplateInputBuffer" -> isPresent("techguns");
            case "immersiveengineering.MixinManualRecipeDisplay" -> immersiveEngineeringLoaded;
            case "minecraft.forge.MixinForgelinConstructionAdapter" -> isPresent("forgelin_continuous");
            case "jeresources.MixinEnchantmentWrapper", "jeresources.InvokerEnchantmentWrapper" ->
                isPresent("jeresources");
            case "neofontrender.MixinSpellcheckLazy", "neofontrender.MixinChatHistoryLazy" -> neoFontRenderUiLoaded;
            case "actinium.MixinShaderTextCache" -> actiniumLoaded;
            case "ic2.MixinIc2LazyDummies" -> ic2Loaded;
            case "solcarrot.MixinFoodCatalogLazy" -> isPresent("solcarrot");
            case "ingameinfoxml.MixinLazyTagFactories" -> isPresent("ingameinfoxml");
            case "geckolib3.MixinGeckoConverter" -> isPresent("geckolib3");
            case "jecharacters.MixinPinyinAcceleratedNode" -> isPresent("jecharacters");
            case "codechickenlib.MixinCclFactoryProbe" -> isPresent("codechickenlib");
            case "ar.MixinWavefrontSyntax" -> arLoaded;
            case "zenutils.MixinBytecodeClassAliases" -> isPresent("zenutils");
            case "universaltweaks.MixinUtStartupCrafting" -> isPresent("universaltweaks");
            case "cofh.MixinCoFHStartupCrafting" -> cofhLoaded;
            case "cofh.MixinThermalStartupScope" -> cofhLoaded && isPresent("thermalexpansion");
            case "mekanism.MixinMekanismStartupScope" -> isPresent("mekanism") && isPresent("universaltweaks");
            case "avaritia.MixinAvaritiaFactoryProbe" -> avaritiaLoaded;
            case "enderio.MixinTriItemLookupAccess" -> enderioLoaded;
            case "tconstruct.AccessorTinkerMeltingRegistry", "tconstruct.MixinTinkerMeltingRegistry",
                 "tconstruct.MixinMaterialMeltingInputs" -> isPresent("tconstruct");
            // Primitive registration applies the same lazy field policy directly in its overwrite.
            case "librarianlib.MixinLibrarianPacketPrewarm" -> isPresent("librarianlib")
                && !NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps;
            case "librarianlib.MixinLibrarianFieldCache", "librarianlib.MixinLibrarianTilePrewarm" ->
                isPresent("librarianlib");
            case "kirino.MixinKirinoScanning",
                 "minecraft.forge.MixinOreIngredientInputs", "minecraft.MixinIngredientRecipeRevision",
                 "minecraft.MixinLocaleParsing" -> true;
            default -> null;
        };
        if (startupEnabled != null) {
            return NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork && startupEnabled;
        }
        final int split = mixinName.indexOf('.');
        if (split < 0) {
            return true;
        }
        if (mixinName.startsWith("enumcache.")) {
            return NovaEngCoreConfig.PERFORMANCE.cacheEnumValuesArrays;
        }
        if (!isOptimizationEnabled(mixinName)) {
            return false;
        }

        return switch (mixinName.substring(0, split)) {
            case "reid" -> reidLoaded;
            case "botania_r" -> NovaEngCoreConfig.SERVER.bot;
            case "base" -> NovaEngCoreConfig.CLIENT.optimizeResourceExistence;
            case "biomesoplenty" -> NovaEngCoreConfig.CLIENT.optimizeBopFog;
            case "revoui" -> neoFontRenderUiLoaded && actiniumLoaded;
            default -> true;
        };
    }

    private static boolean isOptimizationEnabled(final String mixinName) {
        if (mixinName.startsWith("diagnostic.")) {
            return NovaEngCoreConfig.CLIENT.diagObjModelProbe;
        }
        return switch (mixinName) {
            case "taiga.MixinDescendingOre" -> isPresent("taiga");
            case "chisel.MixinBasaltNotifications" -> isPresent("chisel");
            case "minecraft.MixinChunkPrimerImport", "minecraft.AccessorSectionCounts" -> reidLoaded
                && NovaEngCoreConfig.SERVER.optimizeChunkPaletteGrowth && NovaEngCoreConfig.SERVER.bulkChunkPrimerImport;
            case "minecraft.MixinObserverPaletteLinear", "minecraft.MixinObserverPaletteHashMap",
                 "minecraft.AccessorObserverPalette",
                 "minecraft.MixinWorldObserverNotifications" -> NovaEngCoreConfig.SERVER.optimizeObserverNotifications;
            case "minecraft.MixinWorldSetBlockReads" -> NovaEngCoreConfig.SERVER.optimizeSetBlockStateReads;
            case "actinium.MixinBuildContextMemory", "actinium.MixinVintageScratchMemory" ->
                NovaEngCoreConfig.PERFORMANCE.trimChunkBuildScratch || NovaEngCoreConfig.PERFORMANCE.chunkBuildMemoryBudget;
            case "actinium.MixinChunkJobMemoryBudget", "actinium.MixinChunkOutputBudget",
                 "actinium.MixinBuildOutputBudgetRelease", "actinium.MixinSortOutputBudgetRelease",
                 "actinium.MixinRenderSectionMemoryBudget" ->
                NovaEngCoreConfig.PERFORMANCE.chunkBuildMemoryBudget;
            case "mmce.InvokerItemOutputBus", "mmce.MixinItemOutputPreparation",
                 "ae2.MixinNetworkInventoryOutputPreparation" -> aeOutputPreparationEnabled();
            case "mmce.MixinTileNotificationGeneration", "mmce.MixinTaskExecutorNotifications" ->
                NovaEngCoreConfig.PERFORMANCE.coalesceMmceNotifications;
            case "minecraft.MixinClassInheritanceMultiMapLazy" -> NovaEngCoreConfig.PERFORMANCE.lazyEntityClassIndexes;
            case "minecraft.MixinChunkEntityQuery", "minecraft.MixinClassInheritanceMultiMapIterator" ->
                NovaEngCoreConfig.CLIENT.optimizeEntityLookupIteration;
            case "minecraft.MixinAxisAlignedBBIntercept" ->
                NovaEngCoreConfig.PERFORMANCE.optimizeAxisAlignedBBIntercept;
            case "minecraft.MixinVec3iHash" -> NovaEngCoreConfig.PERFORMANCE.optimizeVec3iHash;
            case "minecraft.MixinNBTTagLongArrayBulkRead" -> NovaEngCoreConfig.PERFORMANCE.optimizeNbtLongArrayRead;
            case "mmce.MixinRecipeCraftingContextMemory",
                 "mmce.MixinRecipeCraftingContextPool" -> NovaEngCoreConfig.PERFORMANCE.boundedRecipeContexts;
            case "astralsorcery.MixinTexturePreloader",
                 "astralsorcery.MixinBindableResourceLazyAllocation" -> NovaEngCoreConfig.CLIENT.optimizeAstralSorceryTexturePreload;
            case "astralsorcery.MixinCraftingRecipeHelper",
                 "astralsorcery.MixinItemComparator" -> NovaEngCoreConfig.CLIENT.optimizeAstralSorceryRecipeLookup;
            case "astralsorcery.MixinAstralSorceryConfig" -> NovaEngCoreConfig.CLIENT.optimizeAstralSorceryConfigSave;
            case "astralsorcery.MixinTileAccelerationBlacklist" ->
                NovaEngCoreConfig.SERVER.optimizeHorologiumAccelerationBlacklist;
            case "enderio.MixinRecipeFactoryXmlInputFactory" -> NovaEngCoreConfig.CLIENT.optimizeEnderIoXmlFactory;
            case "enderio.MixinAlloyRecipeManager" -> NovaEngCoreConfig.CLIENT.optimizeEnderIoAlloyIndex;
            case "minecraft.MixinShapelessRecipeAllocation" ->
                NovaEngCoreConfig.CLIENT.optimizeShapelessRecipeAllocation;
            case "minecraft.MixinMipmapTraversal" -> actiniumLoaded && NovaEngCoreConfig.CLIENT.optimizeMipmapTraversal;
            case "botania.MixinRenderTileTinyPotatoLazyCosmetics" -> NovaEngCoreConfig.CLIENT.optimizeBotaniaTinyPotato;
            case "bibliocraft.MixinPaintingUtil" -> NovaEngCoreConfig.CLIENT.optimizeBiblioCraftPaintingCache;
            case "biomesoplenty.MixinTrailManager" -> NovaEngCoreConfig.CLIENT.optimizeBopRemoteTrails;
            case "nco.MixinNCPFWriter" -> NovaEngCoreConfig.CLIENT.optimizeNuclearcraftNcpfExport;
            case "nco.MixinITileGui" -> NovaEngCoreConfig.CLIENT.optimizeNuclearcraftTilePackets;
            case "minecraft.MixinAbstractTexture",
                 "minecraft.forge.MixinCloudRenderer" -> NovaEngCoreConfig.CLIENT.optimizeCloudColorUpload;
            case "minecraft.MixinFramebufferBlit" -> NovaEngCoreConfig.CLIENT.optimizeFramebufferBlit;
            case "ic2.MixinBlockStateContainer" -> ic2Loaded && NovaEngCoreConfig.CLIENT.optimizeIc2PropertyTables;
            case "ic2.MixinIc2BlockStateInstance" -> NovaEngCoreConfig.CLIENT.optimizeIc2PropertyTables;
            case "bibliocraft.MixinModelManagerPaintingGeneration" -> bibliocraftLoaded;
            case "ar.MixinTileForgePowerInput", "ar.MixinTileForgePowerOutput" -> arLoaded;
            case "enderio.MixinNNList" -> enderioLoaded;
            case "threng.MixinL9AspectInventory", "threng.MixinL9AspectSlot" -> threngLoaded;
            case "immersiveengineering.MixinTileEntityPumpjack" -> immersiveEngineeringLoaded;
            case "cofh.MixinTypeEntry" -> cofhLoaded;
            case "cofh.MixinTransposerRecipeCategoryFill",
                 "cofh.MixinTransposerRecipeCategoryExtract" ->
                cofhLoaded && NovaEngCoreConfig.CLIENT.optimizeThermalTransposerRecipes;
            case "customloadingscreen.MixinMinecraftDisplayerRenderer" ->
                NovaEngCoreConfig.CLIENT.optimizeCustomLoadingScreen;
            case "codechickenlib.MixinCCBlockStateLoader" -> NovaEngCoreConfig.CLIENT.optimizeCodeChickenBlockstateScan;
            case "codechickenlib.MixinModelBakeryErrorState" ->
                NovaEngCoreConfig.CLIENT.optimizeCodeChickenErrorStateLog;
            case "minecraft.MixinResourceLocation" ->
                NovaEngCoreConfig.CLIENT.optimizeResourceLocationStableValue;
            case "minecraft.forge.MixinForgeRegistryIdLookup" ->
                !NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps && (NovaEngCoreConfig.CLIENT.optimizeFrozenRegistryIdLookup || NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork);
            case "minecraft.forge.MixinForgeRegistryIterator" ->
                !NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps && NovaEngCoreConfig.CLIENT.optimizeFrozenRegistryIdLookup;
            case "minecraft.MixinModelResourceLocationFastPath" ->
                NovaEngCoreConfig.CLIENT.optimizeFerriteModelResourceLocation;
            case "minecraft.MixinConditionPropertyValueCanonicalization",
                 "minecraft.MixinConditionAndCanonicalization",
                 "minecraft.MixinConditionOrCanonicalization" -> NovaEngCoreConfig.CLIENT.optimizeFerritePredicates;
            case "minecraft.forge.MixinChunkIOProviderNbt" -> NovaEngCoreConfig.PERFORMANCE.optimizeFerriteChunkNbt;
            case "minecraft.MixinMultipartModelBuilder" -> NovaEngCoreConfig.CLIENT.optimizeFerriteMultipartModels;
            case "jei.MixinElementSearch" -> NovaEngCoreConfig.CLIENT.optimizeJeiAsyncSearchIndex;
            case "jei.MixinStackHelper" -> NovaEngCoreConfig.CLIENT.optimizeJeiStackUidCache;
            case "jei.MixinRecipeRegistryId" -> NovaEngCoreConfig.CLIENT.optimizeJeiRecipeId;
            case "jei.MixinRecipeMapDeduplication", "jei.MixinItemStackIngredientHash" ->
                NovaEngCoreConfig.CLIENT.optimizeJeiRecipeIndex;
            case "jei.MixinRecipeHandlerSelection" -> NovaEngCoreConfig.CLIENT.optimizeJeiRecipeHandlerSelection;
            case "mantle.MixinRecipeItemCombination" -> isPresent("mantle")
                && NovaEngCoreConfig.CLIENT.optimizeMantleRecipeCombinations;
            case "environmentaltech.MixinTargetableRegistry" -> isPresent("environmentaltech")
                && NovaEngCoreConfig.CLIENT.optimizeEnvironmentalTechTargets;
            case "actinium.MixinQuadTreeConstruction" -> actiniumLoaded
                && NovaEngCoreConfig.CLIENT.optimizeActiniumQuadTreeConstruction;
            case "minecraft.forge.MixinVanillaModelTextureLocations",
                 "minecraft.forge.MixinVanillaModelMatrixCheck" -> NovaEngCoreConfig.CLIENT.optimizeModelBakeInputs;
            case "minecraft.forge.MixinCraftingHelperJarScan" -> NovaEngCoreConfig.CLIENT.optimizeForgeRecipeJarScan;
            case "stellarcore.MixinClasspathIndexWorker" ->
                stellarCoreLoaded && NovaEngCoreConfig.CLIENT.optimizeClasspathIndexWorker;
            case "mmce.MixinBlockArrayIngredients" ->
                mmceLoaded && NovaEngCoreConfig.CLIENT.optimizeMmcePreviewIngredients;
            case "mmce.MixinBlockStateDescriptorHash" -> mmceLoaded
                && NovaEngCoreConfig.CLIENT.optimizeMmceBlockStateDescriptorHash;
            case "nuclearcraft.MixinValidFluidInputs" -> isPresent("nuclearcraft")
                && NovaEngCoreConfig.CLIENT.optimizeNuclearCraftValidFluids;
            case "neofontrender.MixinDoubleMeta" ->
                neoFontRenderUiLoaded && NovaEngCoreConfig.CLIENT.optimizeSpellingDictionaryRegions;
            case "stellarcore.MixinNBTTagBackingMapEquality" ->
                NovaEngCoreConfig.PERFORMANCE.optimizeStellarNbtMapEquality;
            case "alfheim.MixinDeduplicatedLongQueue" -> NovaEngCoreConfig.SERVER.optimizeAlfheimDeduplicationSets;
            case "alfheim.MixinLightingEngine" -> NovaEngCoreConfig.SERVER.optimizeAlfheimChunkLookup;
            case "alfheim.MixinLightingNeighborData" -> isPresent("alfheim")
                && NovaEngCoreConfig.SERVER.optimizeAlfheimNeighborData;
            case "rtg.MixinChunkGeneratorBiomeWeights" -> NovaEngCoreConfig.SERVER.optimizeRtgBiomeWeights;
            case "rtg.MixinSpacedCellularNoiseBounds" -> NovaEngCoreConfig.SERVER.optimizeRtgCellularBounds;
            case "jei.MixinItemStackListFactory" -> NovaEngCoreConfig.CLIENT.optimizeJeiItemList;
            case "contenttweaker.MixinCreativeTabsResourceList",
                 "minecraft.AccessorCreativeTabs" -> NovaEngCoreConfig.CLIENT.optimizeCreativeTabLookup;
            case "journeymap.MixinFileHandler" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapThemeCopy;
            case "journeymap.MixinTextureCacheArchiveNames" -> isPresent("journeymap")
                && NovaEngCoreConfig.CLIENT.optimizeJourneymapArchiveNames;
            case "konkrete.MixinLocalsCopy" -> NovaEngCoreConfig.CLIENT.optimizeKonkreteLocalsCopy;
            case "journeymap.MixinJMChunkCache" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapChunkDrain;
            case "journeymap.MixinJMChunkStorage" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapChunkBuffer;
            case "journeymap.MixinVanillaBlockSpriteProxy" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapBlockSprites
                || NovaEngCoreConfig.CLIENT.optimizeJourneymapSpriteDedupe;
            case "minecraft.forge.MixinEventBus" -> NovaEngCoreConfig.CLIENT.optimizeEventBusRegistration;
            case "zenscript.MixinTypeRegistry",
                 "zenscript.MixinJavaMethod",
                 "zenutils.MixinClasspathBytesPath",
                 "zenscript.MixinZenTypeNativeMethods" -> NovaEngCoreConfig.CLIENT.optimizeZenScriptCompilation;
            case "zenutils.MixinLogOptionPosition", "crafttweaker.MixinCraftTweakerScriptPosition" ->
                NovaEngCoreConfig.CLIENT.optimizeZenUtilsLogPosition;
            case "crafttweaker.MixinRecipeOutputRemoval" -> isPresent("crafttweaker")
                && NovaEngCoreConfig.CLIENT.optimizeCraftTweakerOutputRemoval;
            case "onlyoneitem.MixinMatchItemHandler" -> isPresent("only_one_item")
                && NovaEngCoreConfig.CLIENT.optimizeOnlyOneItemMatching;
            case "thermaldynamics.MixinModelHelperFaceFilter" -> isPresent("thermaldynamics")
                && NovaEngCoreConfig.CLIENT.optimizeThermalDynamicsFaceMerge;
            case "librarianlib.MixinSavedJavaFields" -> isPresent("librarianlib")
                && NovaEngCoreConfig.CLIENT.optimizeLibrarianJavaSavedFields;
            case "neofontrender.MixinJiebaDictionarySplit" -> neoFontRenderUiLoaded
                && NovaEngCoreConfig.CLIENT.optimizeJiebaDictionarySplit;
            case "neofontrender.MixinJiebaDictionaryCharacters" -> neoFontRenderUiLoaded
                && NovaEngCoreConfig.CLIENT.optimizeJiebaDictionaryCharacters;
            case "minecraft.MixinTextureMapUploadBinding", "actinium.MixinGLStateManagerUploadBinding" ->
                actiniumLoaded && NovaEngCoreConfig.CLIENT.optimizeAtlasUploadBinding;
            case "minecraft.MixinFallbackResourceManager", "libnine.MixinL9Models", "ae2.MixinUVLModelLoader" ->
                NovaEngCoreConfig.CLIENT.optimizeModelJsonMetadata;
            case "minecraft.MixinModelManagerResourceGeneration" -> NovaEngCoreConfig.CLIENT.optimizeModelJsonMetadata
                || NovaEngCoreConfig.CLIENT.optimizeModelBakeInputs
                || NovaEngCoreConfig.CLIENT.optimizeResourceExistence
                || NovaEngCoreConfig.CLIENT.optimizeFerritePredicates
                || NovaEngCoreConfig.CLIENT.optimizeFerriteMultipartModels;
            case "actinium.MixinChunk",
                 "actinium.MixinChunkProviderClient",
                 "actinium.MixinWorldClient" -> actiniumLoaded && NovaEngCoreConfig.CLIENT.optimizeEntityGatherer;
            case "actinium.MixinEntityGatherer" -> NovaEngCoreConfig.CLIENT.optimizeEntityGatherer;
            case "actinium.MixinActiniumWorldRendererLazyBatch" -> NovaEngCoreConfig.CLIENT.optimizeTesrLazyBatch;
            case "actinium.MixinGLStateManagerGenericAttributes",
                 "actinium.MixinShaderManagerGenericAttributes" -> NovaEngCoreConfig.CLIENT.optimizeGenericAttributes;
            case "actinium.MixinGLStateManagerVertexArray",
                 "actinium.MixinImmediateCommandListVertexArray",
                 "actinium.MixinPassThroughGLStateManagerVertexArray" -> NovaEngCoreConfig.CLIENT.optimizeVaoBindings;
            case "minecraft.MixinTileEntityRendererDistance",
                 "minecraft.MixinEntityRendererTileEntityCulling" ->
                NovaEngCoreConfig.CLIENT.optimizeTesrRenderDistance || NovaEngCoreConfig.CLIENT.optimizeTesrFrustumCulling;
            case "minecraft.MixinSoundHandlerUpdateBatch", "minecraft.MixinSoundSystemWakeups" ->
                NovaEngCoreConfig.CLIENT.optimizeSoundUpdateWakeups;
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
            case "zenutils.MixinCatenationManager" -> NovaEngCoreConfig.SERVER.optimizeZenUtilsTickWork;
            case "ic2.MixinConfigTickPath" -> NovaEngCoreConfig.SERVER.optimizeIc2TickConfigPath;
            case "minecraft.MixinMinecraftServerDimensionIds", "minecraft.forge.AccessorDimensionManagerWorlds",
                 "minecraft.forge.MixinDimensionManagerLeakCheck" -> NovaEngCoreConfig.SERVER.optimizeDimensionTickIds;
            case "stellarcore.MixinPlayerChunkMapSnapshot" -> !stellarCoreLoaded
                && NovaEngCoreConfig.SERVER.optimizeChunkTickSnapshot;
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
            case "mekanism.MixinSawmillLogRecipes" -> NovaEngCoreConfig.CLIENT.optimizeMekanismSawmillRegistration;
            case "minecraft.MixinMinecraftCtmPreload" -> ctmLoaded;
            case "ctm.MixinCtmLogicConnectionPosCache", "ctm.MixinDirConnectionPosCache" ->
                NovaEngCoreConfig.CLIENT.optimizeChiselCtmConnectionOffsets;
            case "reid.MixinSectionStorage", "reid.MixinSectionStorageClient", "reid.MixinReidSectionAccess",
                 "reid.MixinAnvilSectionCodec" -> NovaEngCoreConfig.SERVER.optimizeChunkPaletteGrowth;
            case "ar.MixinAtmosphereTemperature", "ar.MixinAtmosphereHandler", "ar.MixinAtmosphereTypes",
                 "ar.MixinAtmosphereTemps" -> NovaEngCoreConfig.SERVER.optimizeWorldgenAtmosphere;
            case "ae2.MixinCompassReaderRegions", "ar.MixinAtmosphereDimensionMap" ->
                NovaEngCoreConfig.SERVER.optimizeWorldgenIdMaps;
            case "cofh.MixinWeightedBlockPredicate", "cofh.MixinWorldGenPredicate" ->
                cofhLoaded && cofhWorldLoaded && (NovaEngCoreConfig.SERVER.optimizeWorldgenOrePredicates
                    || NovaEngCoreConfig.SERVER.unifiedOreGeneration);
            case "ae2.MixinMeteorDataNameEncoder", "ae2.MixinSpawnDataInput" ->
                NovaEngCoreConfig.SERVER.optimizeMeteorSpawnDataIo;
            case "cofh.MixinWorldGenMinableClusterGeometry" ->
                cofhLoaded && cofhWorldLoaded && (NovaEngCoreConfig.SERVER.optimizeOreVeinGeometry
                    || NovaEngCoreConfig.SERVER.unifiedOreGeneration);
            case "minecraft.MixinOverworldPrimerOres", "rtg.MixinRtgPrimerOres", "cofh.AccessorPrimerOreCluster",
                 "cofh.AccessorPrimerOreDistribution", "cofh.MixinUniformPrimerOres", "cofh.MixinGaussianPrimerOres",
                 "cofh.MixinPrimerOreReload" -> NovaEngCoreConfig.SERVER.generateOresInTerrain;
            case "minecraft.MixinBlockStoneMetadata" -> NovaEngCoreConfig.SERVER.optimizeStoneMetadataStates;
            case "minecraft.MixinBlockLeavesPropagation" -> NovaEngCoreConfig.SERVER.optimizeLeafDecayPropagation;
            case "minecraft.MixinWorldGenMinableGeometry" ->
                NovaEngCoreConfig.SERVER.optimizeVanillaOreGeometry || NovaEngCoreConfig.SERVER.unifiedOreGeneration;
            case "minecraft.MixinAttributeModifierSnapshot" ->
                NovaEngCoreConfig.SERVER.optimizeAttributeModifierSnapshots;
            case "minecraft.MixinGenLayerZoomRandom" -> NovaEngCoreConfig.SERVER.optimizeBiomeZoomRandom;
            case "minecraft.MixinMapGenStructure" -> NovaEngCoreConfig.SERVER.optimizeStructureLookup;
            case "minecraft.MixinStateImplementationProperties" ->
                NovaEngCoreConfig.SERVER.optimizeBlockStateProperties;
            case "minecraft.MixinEntityDataManagerLookup" -> NovaEngCoreConfig.SERVER.optimizeEntityDataManagerLookup;
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
}
