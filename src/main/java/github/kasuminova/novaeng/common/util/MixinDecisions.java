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
    public static final boolean deepMobLearningLoaded = isPresent("deepmoblearning");
    public static final boolean reidLoaded = isPresent("jeid") || isPresent("reid");
    public static final boolean draconicAdditionsLoaded = isPresent("draconicadditions");
    public static final boolean draconicEvolutionLoaded = isPresent("draconicevolution");
    public static final boolean ic2Loaded = isPresent("ic2");
    public static final boolean immersiveEngineeringLoaded = isPresent("immersiveengineering");
    public static final boolean neoFontRenderLoaded = isPresent("neofontrender");
    public static final boolean rpsIdeasLoaded = isPresent("rpsideas");
    public static final boolean ctmLoaded = isPresent("ctm");
    public static final boolean mmceLoaded = isPresent("modularmachinery");
    private static final boolean mmceCopyShellPoolAvailable = mmceLoaded
        && MixinDecisions.class.getClassLoader().getResource(
        "hellfirepvp/modularmachinery/common/util/ItemUtils$CopyShellPool.class") != null;
    public static final boolean packagedAutoLoaded = isPresent("packagedauto");
    public static final boolean packagedAstralLoaded = isPresent("packagedastral");
    public static final boolean circulationNetworksLoaded = isPresent("circulation_networks");
    public static final boolean stellarCoreLoaded = isPresent("stellar_core");
    private static final boolean pulsarLoaded = isPresent("pulsar");

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
            case "enderio.MixinCollidableComponent", "enderio.MixinBlockConduitBundleBounds",
                 "enderio.AccessorEnderIoProfiler", "enderio.MixinRelayingModelProfilerLabels" -> {
                return enderioLoaded && isPresent("endercore") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "enderutilities.MixinGuiEnderUtilitiesTooltip" -> {
                return isPresent("enderutilities") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "zenutils.MixinItemTooltipMatching" -> {
                return isPresent("zenutils") && isPresent("crafttweaker")
                    && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "journeymap.MixinBlockMDScanPositions", "journeymap.MixinSurfaceHeightValues",
                 "journeymap.MixinTopoHeightValues", "journeymap.MixinJMChunkSnapshotWriter",
                 "journeymap.MixinForgeRegistryMetadataEpoch" -> {
                return isPresent("journeymap") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "neofontrender.MixinTooltipIdentityText", "neofontrender.AccessorTooltipNbtCompound",
                 "neofontrender.AccessorTooltipNbtList" -> {
                return neoFontRenderLoaded && isPresent("neofontrender_ui_enhancements")
                    && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "neofontrender.MixinFontBindingsSnapshot" -> {
                return neoFontRenderLoaded && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "draconicevolution.MixinFusionInjectorScan" -> {
                return draconicEvolutionLoaded && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "nuclearcraft.MixinProcessorAutoPushDirections" -> {
                return isPresent("nuclearcraft") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "ae2.MixinAEPartLocationLookup", "ae2.MixinCableBusLightValues", "ae2.MixinFacingToRotationLookup" -> {
                return ae2Loaded && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations
                    && NovaEngCoreConfig.PERFORMANCE.cacheEnumValuesArrays;
            }
            case "mmce.MixinRequirementItemOwnedCopy", "mmce.MixinRecipeModifierAppliers",
                 "mmce.AccessorBlockPos2ValueMap", "mmce.MixinBlockArrayRuntimeQueries" -> {
                return mmceLoaded && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "extrabotany.MixinCosmeticItemRenderLayerModels" -> {
                return isPresent("extrabotany") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "theoneprobe.MixinConfigTextStyleCache" -> {
                return isPresent("theoneprobe") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "itemborders.MixinTextColorCache" -> {
                return isPresent("itemborders") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "circulation.MixinFullBrightDisplayListLookup" -> {
                return circulationNetworksLoaded && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "tconevo.MixinItemStackBarEventLazySet" -> {
                return isPresent("tconevo") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "minecraft.MixinStatListDisplayMemory" -> {
                return NovaEngCoreConfig.CLIENT.compactStatDisplays;
            }
            case "minecraft.forge.MixinOwnedTransformFactories", "minecraft.forge.MixinPerspectiveTransformSharing" -> {
                return NovaEngCoreConfig.CLIENT.compactModelTransforms && NovaEngCoreConfig.CLIENT.compactBakedGeometry;
            }
            case "minecraft.MixinAnimationFrameStorage" -> {
                return NovaEngCoreConfig.CLIENT.compressAnimationFrames;
            }
            case "actinium.MixinSpriteMipPixels" -> {
                return NovaEngCoreConfig.CLIENT.compressAnimationFrames && actiniumLoaded;
            }
            case "tconstruct.MixinColoredTextureFrameRead" -> {
                return NovaEngCoreConfig.CLIENT.compressAnimationFrames && isPresent("tconstruct");
            }
            case "jei.MixinColorGetterFrameRead" -> {
                return NovaEngCoreConfig.CLIENT.compressAnimationFrames && isPresent("jei");
            }
            case "minecraft.MixinModelResourceLocationStrings" -> {
                return NovaEngCoreConfig.CLIENT.optimizeResourceLocationStableValue;
            }
            case "minecraft.MixinTextComponentStringSharing", "minecraft.MixinBlockPartFaceStrings",
                 "minecraft.MixinModelBlockTextureStrings", "minecraft.MixinModelBlockJsonStrings",
                 "codechicken.MixinObfRemapperStrings" -> {
                return NovaEngCoreConfig.PERFORMANCE.shareImmutableStrings;
            }
            case "mmce.MixinMachineRecipeStrings" -> {
                return mmceLoaded && NovaEngCoreConfig.PERFORMANCE.shareImmutableStrings;
            }
            case "jei.MixinHeiIngredientStrings" -> {
                return isPresent("jei") && NovaEngCoreConfig.PERFORMANCE.shareImmutableStrings;
            }
            case "rtg.MixinConfigPropertyStrings" -> {
                return isPresent("rtg") && NovaEngCoreConfig.PERFORMANCE.shareImmutableStrings;
            }
            case "jei.MixinJeiStarterHashCacheCleanup", "jei.MixinRecipeRegistryIndexMemory",
                 "jei.MixinHeiTableMemoryAccess" -> {
                return isPresent("jei") && NovaEngCoreConfig.CLIENT.optimizeJeiRecipeIndex;
            }
            case "astralsorcery.MixinBufferBatchCapacity" -> {
                return astralSorceryLoaded && NovaEngCoreConfig.CLIENT.optimizeRetainedRenderBuffers;
            }
            case "neofontrender.MixinCosmicTessellatorCapacity" -> {
                return neoFontRenderLoaded && NovaEngCoreConfig.CLIENT.optimizeRetainedRenderBuffers;
            }
            case "customloadingscreen.MixinTextureAnimatorUploadMemory" -> {
                return isPresent("customloadingscreen") && NovaEngCoreConfig.CLIENT.optimizeRetainedRenderBuffers;
            }
            case "neofontrender.MixinTextRouteCacheKeyHash", "neofontrender.MixinFontConfigPaths",
                 "neofontrender.MixinCosmicReusableWords" -> {
                return neoFontRenderLoaded && NovaEngCoreConfig.CLIENT.optimizeMenuWorkingAllocations;
            }
            case "neofontrender.MixinUiNavigationRuntime", "neofontrender.MixinUiTreeValidator" -> {
                return neoFontRenderLoaded && isPresent("neofontrender_ui_enhancements")
                    && NovaEngCoreConfig.CLIENT.optimizeMenuWorkingAllocations;
            }
            case "mmce.MixinActiveRecipeLazyData" -> {
                return mmceLoaded && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "mmce.MixinItemHandlerGenericCopy", "mmce.MixinItemUtilsGenericCopy",
                 "mmce.InvokerItemHandlerCopyShellPool" -> {
                return mmceCopyShellPoolAvailable && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "mekanism.MixinDimensionalStabilizerOverlaySnapshot" -> {
                return isPresent("mekanism") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
            case "tconstruct.MixinTraitTagQueries", "tconstruct.MixinAbstractTraitTagQuery" -> {
                return isPresent("tconstruct") && NovaEngCoreConfig.PERFORMANCE.reduceRuntimeAllocations;
            }
        }
        if (mixinName.startsWith("moreoverlays."))
            return isPresent("moreoverlays") && NovaEngCoreConfig.CLIENT.optimizeLightOverlay;
        if (mixinName.equals("mekanism.MixinFrequencyManagerTick") || mixinName.equals("mekanism.MixinMekanismFrequencyManagerLifecycle"))
            return isPresent("mekanism") && NovaEngCoreConfig.SERVER.optimizeFrequencyManagers;
        if (mixinName.equals("minecraft.MixinLazyAttributeModifierMaps") || mixinName.equals("minecraft.MixinLazyPathHeap")
            || mixinName.equals("minecraft.MixinLazyPathFinder") || mixinName.equals("minecraft.MixinAttributeNameIndex")
            || mixinName.equals("minecraft.MixinAttributeNameIndexStorage"))
            return NovaEngCoreConfig.SERVER.optimizeEntityConstruction;
        if (mixinName.startsWith("geometry.")) {
            if (!NovaEngCoreConfig.CLIENT.compactBakedGeometry) return false;
            if (mixinName.startsWith("geometry.actinium.")) return actiniumLoaded;
            if (mixinName.startsWith("geometry.ctm.")) return ctmLoaded && NovaEngCoreConfig.CLIENT.optimizeCtmMaterialProbe;
            if (mixinName.equals("geometry.MixinGeometryRenderItem")) return !isPresent("celeritasextra");
            if (mixinName.equals("geometry.MixinGeometryItemFrameLod")) return isPresent("celeritasextra");
            return true;
        }
        // Target availability is handled by Mixin; mod checks here express cross-mod requirements.
        switch (mixinName) {
            case "minecraft.MixinRegionRenderCacheBuilderCapacity", "lumenized.MixinBloomRenderLayerCapacity" -> {
                return actiniumLoaded;
            }
            case "mmce.MixinBufferBuilderPoolMemory", "mmce.MixinReusableVBOUploaderGrowth",
                 "mmce.MixinControllerModelTaskRetirement" -> {
                return isPresent("geckolib3");
            }
            case "ae2.MixinMeteorWriteBatch" -> {
                return NovaEngCoreConfig.SERVER.optimizeMeteorSpawnDataIo && NovaEngCoreConfig.SERVER.batchMeteorSpawnWrites;
            }
            case "artemislib.MixinAttachAttributes", "astralsorcery.MixinAttributeOwner",
                 "mekanism.MixinWorldgenFactories", "actuallyadditions.MixinLushCaveAllocation",
                 "cofh.MixinFlatBedrock", "wizardry.MixinStructurePlacementChoices",
                 "chisel.MixinBasaltScan", "bettermineshafts.MixinBiomeTypes",
                 "biomesoplenty.MixinGenerationStages", "biomesoplenty.MixinGeneratorStageRevision",
                 "tconstruct.MixinIslandList", "tconstruct.MixinIslandQueries",
                 "minecraft.MixinLeafBlockKind", "minecraft.MixinPathStateMemo",
                 "minecraft.MixinPathNodeClassification", "minecraft.MixinWorldCollisionReads",
                 "minecraft.MixinWorldEmptyLightListener", "minecraft.MixinLazyAttributeOperations",
                 "minecraft.MixinBiomeCacheLastEntry", "minecraft.MixinLowerStringMapNames" -> {
                return NovaEngCoreConfig.SERVER.optimizeWorldgenAuxiliaries;
            }
            case "bettermeteor.MixinCompassTargetScan" -> {
                return NovaEngCoreConfig.SERVER.optimizeMeteorCompassTargetScan && isPresent("better_meteorite");
            }
            case "kirino.InvokerArrayRange", "architecturecraft.InvokerMessageSide",
                 "minecraft.forge.AccessorFluidRegistryIds", "minecraft.forge.InvokerFluidRegistryDelegate" -> {
                return NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps;
            }
        }
        if (mixinName.endsWith("PrimitiveBiMap")) {
            return NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps;
        }

        final Boolean startupEnabled = switch (mixinName) {
            case "minecraft.MixinDynamicTextureUploadBinding" -> actiniumLoaded
                && NovaEngCoreConfig.CLIENT.optimizeAtlasUploadBinding;
            case "minecraft.forge.MixinForgelinConstructionAdapter" -> isPresent("forgelin_continuous");
            case "cofh.MixinThermalStartupScope" -> cofhLoaded;
            case "mekanism.MixinMekanismStartupScope" -> isPresent("universaltweaks");
            case "mekanism.MixinSolarActivatorTexturePreload" ->
                isPresent("mekanismmultiblockmachine") && NovaEngCoreConfig.CLIENT.deferSolarActivatorScreen;
            case "ftbquests.MixinQuestWrapper" ->
                isPresent("ftbquests") && isPresent("itemfilters") && isPresent("jei");
            case "enderio.MixinRecipesRegistration" -> enderioLoaded;
            case "enderio.AccessorItemStackThing", "enderio.MixinThingsSimpleRecipeOutput",
                 "enderio.MixinShapedRecipeOutput" -> enderioLoaded && isPresent("endercore");
            case "chisel.MixinChiselFuelHandler" -> isPresent("chisel");
            case "contenttweaker.MixinBaseLanguageMigration" -> isPresent("base");
            case "cookingforblockheads.MixinCookingRecipeOutput" -> isPresent("cookingforblockheads");
            case "jeresources.MixinLootTableManagerLookup", "jeresources.MixinPreviewTreasureMapTrade" ->
                isPresent("jeresources");
            case "jeresources.MixinPreviewOreveinMapTrade" -> immersiveEngineeringLoaded && isPresent("jeresources");
            case "onlyoneitem.MixinRecipeCleanupOutput" -> isPresent("only_one_item");
            case "techguns.MixinFxListScanner" -> isPresent("techguns");
            case "ctm.MixinResourceLocationConversion" -> ctmLoaded && NovaEngCoreConfig.CLIENT.optimizeModelBakeInputs;
            case "geckolib3.MixinRawGeometryTreeLookup" -> isPresent("geckolib3");
            // Primitive registration applies the same lazy field policy directly in its overwrite.
            case "librarianlib.MixinLibrarianPacketPrewarm" -> !NovaEngCoreConfig.PERFORMANCE.primitiveBiMaps;
            case "journeymap.MixinThemeTexturePreload", "journeymap.MixinWidgetTexturePreload",
                 "techguns.MixinTemplateInputBuffer", "immersiveengineering.MixinManualRecipeDisplay",
                 "jeresources.MixinEnchantmentWrapper", "jeresources.InvokerEnchantmentWrapper",
                 "neofontrender.MixinSpellcheckLazy", "neofontrender.MixinChatHistoryLazy",
                 "ic2.MixinIc2LazyDummies",
                 "solcarrot.MixinFoodCatalogLazy", "ingameinfoxml.MixinLazyTagFactories",
                 "geckolib3.MixinGeckoConverter", "jecharacters.MixinPinyinAcceleratedNode",
                 "codechickenlib.MixinCclFactoryProbe", "ar.MixinWavefrontSyntax",
                 "zenutils.MixinBytecodeClassAliases", "universaltweaks.MixinUtStartupCrafting",
                 "cofh.MixinCoFHStartupCrafting", "avaritia.MixinAvaritiaFactoryProbe",
                 "tconstruct.AccessorTinkerMeltingRegistry",
                 "tconstruct.MixinTinkerMeltingRegistry", "tconstruct.MixinMaterialMeltingInputs",
                 "librarianlib.MixinLibrarianFieldCache", "librarianlib.MixinLibrarianTilePrewarm",
                 "kirino.MixinKirinoScanning",
                 "minecraft.forge.MixinOreIngredientInputs", "minecraft.MixinIngredientRecipeRevision" -> true;
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
            case "revoui" -> actiniumLoaded;
            default -> true;
        };
    }

    private static boolean isOptimizationEnabled(final String mixinName) {
        if (mixinName.startsWith("diagnostic.")) {
            return NovaEngCoreConfig.CLIENT.diagObjModelProbe;
        }
        return switch (mixinName) {
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
            case "enderio.MixinRecipeFactoryXmlInputFactory", "enderio.MixinStaxFactory" -> NovaEngCoreConfig.CLIENT.optimizeEnderIoXmlFactory;
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
            case "immersiveengineering.MixinMetalComponentLookup" ->
                immersiveEngineeringLoaded && NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork;
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
            case "minecraft.forge.MixinForgeRegistrySyncAllocations",
                 "minecraft.forge.MixinLoadControllerOwnerLookup" -> NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork;
            case "minecraft.MixinConditionPropertyValueCanonicalization",
                 "minecraft.MixinConditionAndCanonicalization",
                 "minecraft.MixinConditionOrCanonicalization" -> NovaEngCoreConfig.CLIENT.optimizeFerritePredicates;
            case "minecraft.forge.MixinChunkIOProviderNbt" -> NovaEngCoreConfig.PERFORMANCE.optimizeFerriteChunkNbt;
            case "minecraft.MixinMultipartModelBuilder" -> NovaEngCoreConfig.CLIENT.optimizeFerriteMultipartModels;
            case "jei.MixinElementSearch", "jei.MixinIngredientInformation" ->
                NovaEngCoreConfig.CLIENT.optimizeJeiAsyncSearchIndex;
            case "vintagefix.MixinDefaultPackJarUri", "patchouli.MixinBookContentsLocale" ->
                NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork;
            case "bogosorter.MixinOreDictHelper", "mcjtylib.MixinTrueTypeFontPixels" ->
                NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork;
            case "zenscript.MixinZenTokenerDfa" ->
                isPresent("crafttweaker") && NovaEngCoreConfig.PERFORMANCE.optimizeStartupWork;
            case "jei.MixinStackHelper" -> NovaEngCoreConfig.CLIENT.optimizeJeiStackUidCache;
            case "jei.MixinRecipeRegistryId" -> NovaEngCoreConfig.CLIENT.optimizeJeiRecipeId;
            case "jei.MixinRecipeMapDeduplication", "jei.MixinItemStackIngredientHash", "jei.AccessorItemStackHelper",
                 "jei.AccessorStackHelper",
                 "jei.AccessorIngredients", "jei.MixinRecipeRegistryInsertion", "jei.MixinIngredientsCapacity" ->
                NovaEngCoreConfig.CLIENT.optimizeJeiRecipeIndex;
            case "jei.MixinRecipeHandlerSelection" -> NovaEngCoreConfig.CLIENT.optimizeJeiRecipeHandlerSelection;
            case "mantle.MixinRecipeItemCombination" -> NovaEngCoreConfig.CLIENT.optimizeMantleRecipeCombinations;
            case "environmentaltech.MixinTargetableRegistry" -> NovaEngCoreConfig.CLIENT.optimizeEnvironmentalTechTargets;
            case "actinium.MixinQuadTreeConstruction" -> NovaEngCoreConfig.CLIENT.optimizeActiniumQuadTreeConstruction;
            case "minecraft.forge.MixinVanillaModelTextureLocations",
                 "minecraft.forge.MixinVanillaModelMatrixCheck" -> NovaEngCoreConfig.CLIENT.optimizeModelBakeInputs;
            case "minecraft.forge.MixinCraftingHelperJarScan" -> NovaEngCoreConfig.CLIENT.optimizeForgeRecipeJarScan;
            case "stellarcore.MixinClasspathIndexWorker" ->
                NovaEngCoreConfig.CLIENT.optimizeClasspathIndexWorker;
            case "stellarcore.InvokerDirectoryPathIndex", "stellarcore.MixinDirectoryPathIndexScan" ->
                stellarCoreLoaded && NovaEngCoreConfig.CLIENT.optimizeClasspathIndexWorker;
            case "mmce.MixinBlockArrayIngredients", "mmce.MixinBlockInformationIngredients" ->
                NovaEngCoreConfig.CLIENT.optimizeMmcePreviewIngredients;
            case "mmce.MixinBlockStateDescriptorHash" -> NovaEngCoreConfig.CLIENT.optimizeMmceBlockStateDescriptorHash;
            case "nuclearcraft.MixinValidFluidInputs" -> NovaEngCoreConfig.CLIENT.optimizeNuclearCraftValidFluids;
            case "neofontrender.MixinDoubleMeta" -> NovaEngCoreConfig.CLIENT.optimizeSpellingDictionaryRegions;
            case "stellarcore.MixinNBTTagBackingMapEquality" ->
                NovaEngCoreConfig.PERFORMANCE.optimizeStellarNbtMapEquality;
            case "pulsar.MixinLightEngineCacheFailure", "pulsar.MixinLightQueue",
                 "pulsar.MixinBlockLightColors", "pulsar.MixinBlockLightFilters", "pulsar.MixinRgbStateProperties",
                 "pulsar.MixinChunkLightCompletion", "pulsar.MixinChunkTaskReservations", "pulsar.MixinChunkTasks",
                 "pulsar.MixinInitialLightCoordinator", "pulsar.MixinLightEngineWorkerPool",
                 "pulsar.MixinPartialFaceLight",
                 "pulsar.MixinSnapshotChunkMap", "pulsar.MixinSWMRNibbleArray", "pulsar.MixinWorldLightManager" ->
                pulsarLoaded && PulsarBackportSelection.PULSAR_0_6_2;
            case "pulsar.MixinLightDataSerializer", "pulsar.MixinScalarLightEdges" ->
                pulsarLoaded && PulsarBackportSelection.SCALAR;
            // Pulsar keeps the underlying client light data current while Fullbright changes display brightness.
            case "minecraft.MixinVanillaLightPropagation" -> !pulsarLoaded;
            case "rtg.MixinChunkGeneratorBiomeWeights" -> NovaEngCoreConfig.SERVER.optimizeRtgBiomeWeights;
            case "rtg.MixinSpacedCellularNoiseBounds" -> NovaEngCoreConfig.SERVER.optimizeRtgCellularBounds;
            case "jei.MixinItemStackListFactory" -> NovaEngCoreConfig.CLIENT.optimizeJeiItemList;
            case "contenttweaker.MixinCreativeTabsResourceList",
                 "minecraft.AccessorCreativeTabs" -> NovaEngCoreConfig.CLIENT.optimizeCreativeTabLookup;
            case "journeymap.MixinFileHandler" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapThemeCopy;
            case "journeymap.MixinTextureCacheArchiveNames" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapArchiveNames;
            case "konkrete.MixinLocalsCopy" -> NovaEngCoreConfig.CLIENT.optimizeKonkreteLocalsCopy;
            case "journeymap.MixinJMChunkCache" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapChunkDrain;
            case "journeymap.MixinJMChunkStorage" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapChunkBuffer;
            case "journeymap.MixinVanillaBlockSpriteProxy" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapBlockSprites;
            case "journeymap.MixinVanillaBlockSpriteDedupe" -> NovaEngCoreConfig.CLIENT.optimizeJourneymapSpriteDedupe;
            case "minecraft.forge.MixinEventBus" -> NovaEngCoreConfig.CLIENT.optimizeEventBusRegistration;
            case "zenscript.MixinTypeRegistry",
                 "zenscript.MixinScriptPathParsing",
                 "zenscript.MixinGenericTypeArguments",
                 "zenscript.MixinJavaMethod",
                 "zenutils.MixinClasspathArchiveVisitor",
                 "zenutils.MixinClasspathBytesPath",
                 "zenscript.MixinZenTypeNativeMethods" -> NovaEngCoreConfig.CLIENT.optimizeZenScriptCompilation;
            case "zenutils.MixinLogOptionPosition", "crafttweaker.MixinCraftTweakerScriptPosition" ->
                NovaEngCoreConfig.CLIENT.optimizeZenUtilsLogPosition;
            case "crafttweaker.MixinRecipeOutputRemoval", "crafttweaker.MixinShapelessRecipeRemoval" ->
                NovaEngCoreConfig.CLIENT.optimizeCraftTweakerOutputRemoval;
            case "onlyoneitem.MixinMatchItemHandler" -> NovaEngCoreConfig.CLIENT.optimizeOnlyOneItemMatching;
            case "thermaldynamics.MixinModelHelperFaceFilter" -> NovaEngCoreConfig.CLIENT.optimizeThermalDynamicsFaceMerge;
            case "librarianlib.MixinSavedJavaFields" -> NovaEngCoreConfig.CLIENT.optimizeLibrarianJavaSavedFields;
            case "neofontrender.MixinJiebaDictionarySplit" -> NovaEngCoreConfig.CLIENT.optimizeJiebaDictionarySplit;
            case "neofontrender.MixinJiebaDictionaryCharacters" -> NovaEngCoreConfig.CLIENT.optimizeJiebaDictionaryCharacters;
            case "minecraft.MixinTextureMapUploadBinding" ->
                actiniumLoaded && NovaEngCoreConfig.CLIENT.optimizeAtlasUploadBinding;
            case "actinium.MixinGLSMTexturesUploadBinding" -> NovaEngCoreConfig.CLIENT.optimizeAtlasUploadBinding;
            case "minecraft.MixinFallbackResourceManager", "ae2.MixinUVLModelLoader" ->
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
                 "actinium.MixinGLSMCurrentAttribsGenericAttributes",
                 "actinium.MixinRenderSystemGenericAttributes",
                 "actinium.MixinShaderManagerGenericAttributes" -> NovaEngCoreConfig.CLIENT.optimizeGenericAttributes;
            case "actinium.MixinGLStateManagerVertexArray",
                 "actinium.MixinGLSMVertexInputVertexArray",
                 "actinium.MixinGLSMAttribStackVertexArray",
                 "actinium.MixinImmediateCommandListVertexArray",
                 "actinium.MixinPassThroughGLStateManagerVertexArray" -> NovaEngCoreConfig.CLIENT.optimizeVaoBindings;
            case "minecraft.MixinTileEntityRendererDistance",
                 "minecraft.MixinEntityRendererTileEntityCulling" ->
                NovaEngCoreConfig.CLIENT.optimizeTesrRenderDistance || NovaEngCoreConfig.CLIENT.optimizeTesrFrustumCulling;
            case "minecraft.MixinSoundHandlerUpdateBatch", "minecraft.MixinSoundSystemWakeups",
                 "minecraft.MixinSoundSystemInitialization" ->
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
            case "ic2.AccessorEnergyNetDelegate",
                 "ic2.MixinEnergyNetChangeHandler", "ic2.MixinEnergyNetEventHandlerRecovery",
                 "ic2.MixinEnergyNetLocalRecovery" -> ic2Loaded;
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
                cofhLoaded && (NovaEngCoreConfig.SERVER.optimizeWorldgenOrePredicates
                    || NovaEngCoreConfig.SERVER.unifiedOreGeneration);
            case "ae2.MixinMeteorDataNameEncoder", "ae2.MixinSpawnDataInput" ->
                NovaEngCoreConfig.SERVER.optimizeMeteorSpawnDataIo;
            case "cofh.MixinWorldGenMinableClusterGeometry" ->
                cofhLoaded && (NovaEngCoreConfig.SERVER.optimizeOreVeinGeometry
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
            case "minecraft.MixinMapGenStructure" -> NovaEngCoreConfig.SERVER.optimizeStructureLookup;
            case "minecraft.MixinStateImplementationProperties" ->
                NovaEngCoreConfig.SERVER.optimizeBlockStateProperties;
            case "minecraft.MixinEntityDataManagerLookup" -> NovaEngCoreConfig.SERVER.optimizeEntityDataManagerLookup;
            case "minecraft.MixinBiome",
                 "minecraft.forge.MixinForgeRegistry" -> NovaEngCoreConfig.SERVER.optimizeBiomeIdLookup;
            case "universaltweaks.MixinUTEntityDesync" -> NovaEngCoreConfig.SERVER.optimizeUniversalTweaksEntityBlacklist;
            default -> true;
        };
    }

    private static boolean isPresent(final String modId) {
        return CleanroomModDiscoverer.instance().isModPresent(modId);
    }
}
