package github.kasuminova.novaeng;

import github.kasuminova.novaeng.common.performance.WorldgenNotificationMode;
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

    @Config.Name("Network")
    public static Network NETWORK = new Network();

    @Config.Name("Performance")
    public static Performance PERFORMANCE = new Performance();

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
    public static class Performance {
        @Config.RequiresMcRestart
        @Config.Name("PrimitiveBiMaps")
        @Config.Comment("Use primitive bidirectional ID maps and primitive call sites in supported mods.")
        public boolean primitiveBiMaps = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeStartupWork")
        @Config.Comment("Reuse startup indexes and immutable parsing metadata, and initialize deferred client data when it is first needed.")
        public boolean optimizeStartupWork = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeStellarNbtMapEquality")
        @Config.Comment("Reuse fastutil entries while comparing StellarCore NBT maps, preserving lookup order and value equality direction.")
        public boolean optimizeStellarNbtMapEquality = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeFerriteChunkNbt")
        @Config.Comment("Losslessly compact decoded section arrays while asynchronous chunks wait for completion. Restore complete NBT before entity loading and Forge ChunkDataEvent.Load; preserve mod tags and pending-save data.")
        public boolean optimizeFerriteChunkNbt = true;

        @Config.RequiresMcRestart
        @Config.Comment("Create entity class indexes only when that chunk section is queried.")
        public boolean lazyEntityClassIndexes = true;
        @Config.RequiresMcRestart
        @Config.Comment("Bound idle MMCE recipe contexts and discard data rebuilt on the next borrow.")
        public boolean boundedRecipeContexts = true;
        @Config.RangeInt(min = 0, max = 1024)
        public int recipeContextsPerRecipe = 8;
        @Config.RangeInt(min = 0, max = 65536)
        public int recipeContextLimit = 1024;
        @Config.RequiresMcRestart
        @Config.Comment("Share a stack index within one inventory_changed evaluation.")
        public boolean sharedInventoryView = true;
        @Config.RequiresMcRestart
        @Config.Comment("Avoid replaying MMCE comparator notifications for an already committed mutation.")
        public boolean coalesceMmceNotifications = true;
        @Config.RequiresMcRestart
        @Config.Comment("Reuse supported AE interface insertion preparation within an MMCE output operation.")
        public boolean batchAe2Outputs = true;
        @Config.RequiresMcRestart
        @Config.Comment("Release oversized idle VintageFix scratch after two low-use 64-task windows. Actinium mesh scratch is controlled in actinium-options.json.")
        public boolean trimChunkBuildScratch = true;
        @Config.RequiresMcRestart
        @Config.Comment("Apply non-blocking byte backpressure to ordinary Actinium chunk rebuilds.")
        public boolean chunkBuildMemoryBudget = true;
        @Config.RangeInt(min = 16, max = 2048)
        public int chunkBuildMemoryBudgetMiB = 128;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAxisAlignedBBIntercept")
        @Config.Comment("Compute AxisAlignedBB ray-trace candidates in primitive locals, avoiding the temporary Vec3d allocated for each tested face.")
        public boolean optimizeAxisAlignedBBIntercept = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeVec3iHash")
        @Config.Comment("Use an avalanche hash for Vec3i/BlockPos coordinates so dense position maps distribute across power-of-two buckets more evenly.")
        public boolean optimizeVec3iHash = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeNbtLongArrayRead")
        @Config.Comment("Bulk-read large NBTTagLongArray payloads from disk-backed DataInputStreams while retaining the vanilla loop for small and network-backed arrays.")
        public boolean optimizeNbtLongArrayRead = true;

        @Config.RequiresMcRestart
        @Config.Name("CacheEnumValuesArrays")
        @Config.Comment("Reuse the clone that the compiler-generated Enum.values() performs, so repeated calls stop allocating a new array. Applied only to enums whose returned array is never written by this modpack.")
        public boolean cacheEnumValuesArrays = true;
    }

    @SuppressWarnings("CanBeFinal")
    public static class Network {
        @Config.RequiresMcRestart
        @Config.Comment("Collect and print a bandwidth summary when a client disconnects or the server stops. Disabled by default and independent of optimization.")
        public boolean enableBandwidthMonitoring = false;

        @Config.RequiresMcRestart
        @Config.Comment("Aggregate serialized PLAY traffic with Zstd after Forge login on dedicated-server and LAN TCP connections. Keep this switch the same on both endpoints. Pure single-player LocalChannel traffic bypasses the complete byte-processing chain; the chunk cache remains independent and can still run locally.")
        public boolean enableBandwidthOptimization = true;

        @Config.RequiresMcRestart
        @Config.Comment("Train a server-side connection dictionary from observed PLAY packets. Connections always synchronize the current dictionary; disabling this planner uses the empty version-zero dictionary.")
        public boolean enableBandwidthDictionary = true;

        @Config.RequiresMcRestart
        @Config.Comment("Increase both CustomPayload limits, the outer frame limit and the decompression limit. Large packets require support on both endpoints and any proxy.")
        public boolean expandPacketLimits = true;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 1, max = 64)
        @Config.Comment("Maximum ordinary CustomPayload size in MiB in either direction. Configure the same limit on both endpoints.")
        public int maxCustomPayloadMiB = 8;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 1, max = 20)
        @Config.Comment("Maximum batching delay in milliseconds, excluding event-loop scheduling delays.")
        public int aggregationDelayMillis = 20;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 4, max = 1024)
        @Config.Comment("Flush a batch once it reaches this many KiB; a larger individual packet is flushed immediately.")
        public int aggregationTargetKiB = 64;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 21, max = 25)
        @Config.Comment("Zstd history window log2(bytes): 21 = 2 MiB, 23 = 8 MiB, 25 = 32 MiB. Native memory usage scales with connections.")
        public int compressionWindowLog = 23;

        @Config.RequiresMcRestart
        @Config.Comment("Reuse Zstd history across outgoing batches. Disable on the sender for independently decompressible batches, for example for packet recording.")
        public boolean reuseCompressionContext = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCompressionBuffers")
        @Config.Comment("Reuse the transient byte arrays used by vanilla Zlib compression handlers. The cache is capped so an oversized packet cannot pin a large buffer per connection.")
        public boolean optimizeCompressionBuffers = true;

        @Config.RequiresMcRestart
        @Config.Comment("Replace repeated outgoing CustomPayload channel names with bounded connection-local indices. Definitions travel in-band; no capability negotiation is needed.")
        public boolean indexCustomPayloadChannels = true;

        @Config.RequiresMcRestart
        @Config.Comment("Server-side: retain already-sent chunks just outside a remote player's view, with live block/tile updates, to avoid full resends when returning.")
        public boolean delayedChunkCache = true;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 0, max = 1024)
        @Config.Comment("Maximum extra watched chunks per player. Zero disables retention.")
        public int delayedChunkCacheSize = 60;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 0, max = 16)
        @Config.Comment("Maximum cached distance beyond the normal server view radius, in chunks. Does not increase the normal view distance.")
        public int delayedChunkCacheDistance = 5;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 1, max = 600)
        @Config.Comment("Retention timeout in monotonic elapsed seconds, checked every server world tick.")
        public int delayedChunkCacheSeconds = 60;

        @Config.RequiresMcRestart
        @Config.Comment("CustomPayload channels which flush preceding packets and bypass aggregation, for proxy/mod compatibility.")
        public String[] unaggregatedChannels = new String[]{"FML|HS", "REGISTER", "UNREGISTER", "MC|Brand"};
    }

    @SuppressWarnings("CanBeFinal")
    public static class Client {
        @Config.RequiresMcRestart
        @Config.Name("OptimizeFerritePredicates")
        @Config.Comment("Canonicalize multipart model predicates by property/value and by their sorted And/Or inputs, following FerriteCore's predicate cache.")
        public boolean optimizeFerritePredicates = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeFerriteMultipartModels")
        @Config.Comment("Reuse baked multipart models with identical selectors and child models in the same order. Disable VintageFix mixin.model_optimizations.dedup_multipart when enabling this.")
        public boolean optimizeFerriteMultipartModels = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeFerriteModelResourceLocation")
        @Config.Comment("Bypass ModelResourceLocation's toString/parse constructor round trip for ordinary ResourceLocations.")
        public boolean optimizeFerriteModelResourceLocation = true;

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
        @Config.Name("OptimizeAstralSorceryRecipeLookup")
        @Config.Comment("Keep Astral Sorcery's recipe output scan ordered, but remove per-recipe comparator sets and varargs arrays.")
        public boolean optimizeAstralSorceryRecipeLookup = true;

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
        @Config.Name("OptimizeNuclearcraftTilePackets")
        public boolean optimizeNuclearcraftTilePackets = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEnderIoXmlFactory")
        @Config.Comment("Reuse Ender IO's single-threaded XML input factory while keeping readers per document.")
        public boolean optimizeEnderIoXmlFactory = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEnderIoAlloyIndex")
        @Config.Comment("Deduplicate item identities before building Ender IO alloy recipe permutations.")
        public boolean optimizeEnderIoAlloyIndex = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeShapelessRecipeAllocation")
        @Config.Comment("Allocate only the matcher inputs selected by the recipe's immutable simplicity flag. Keeps Forge's original matching algorithms.")
        public boolean optimizeShapelessRecipeAllocation = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMipmapTraversal")
        @Config.Comment("Generate mipmap pixels in row order while preserving the active color blender and supplied mip levels.")
        public boolean optimizeMipmapTraversal = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMekanismSawmillRegistration")
        @Config.Comment("Register log recipes sequentially instead of dispatching workers that contend for the same crafting lock. Applies on both sides.")
        public boolean optimizeMekanismSawmillRegistration = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMmcePreviewIngredients")
        @Config.Comment("Index preview ingredient groups by their first item while retaining the original matching and merge order.")
        public boolean optimizeMmcePreviewIngredients = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMmceBlockStateDescriptorHash")
        @Config.Comment("Disperse singleton vanilla-state descriptor hashes in MMCE's canonical pool. Keeps state equality, descriptor contents and pool lifecycle.")
        public boolean optimizeMmceBlockStateDescriptorHash = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeNuclearCraftValidFluids")
        @Config.Comment("Index standard NuclearCraft fluid inputs only while building the allowed-fluid sets. Keeps custom matchers, overridden handlers and exception filters on the original scan.")
        public boolean optimizeNuclearCraftValidFluids = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJourneymapArchiveNames")
        @Config.Comment("Check PNG suffixes and texture-directory prefixes before allocating archive-entry path substrings during JourneyMap texture discovery.")
        public boolean optimizeJourneymapArchiveNames = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeSpellingDictionaryRegions")
        @Config.Comment("Match Jazzy phonetic dictionary regions without creating a substring for each comparison.")
        public boolean optimizeSpellingDictionaryRegions = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeForgeRecipeJarScan")
        @Config.Comment("Read factory entries directly and reject absent recipe roots before mounting ZIP filesystems. Keeps existing recipe callbacks and traversal order. Applies on both sides.")
        public boolean optimizeForgeRecipeJarScan = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeClasspathIndexWorker")
        @Config.Comment("Avoid submitting and joining jar scans from the same fixed pool that initializes namespace indexes. Reuses the existing workers.")
        public boolean optimizeClasspathIndexWorker = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeModelBakeInputs")
        @Config.Comment("Reuse parsed texture identifiers and avoid the integer-check scratch matrix during model baking. Preserves texture lookups and culling results.")
        public boolean optimizeModelBakeInputs = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMantleRecipeCombinations")
        @Config.Comment("Use ordered index bits for small Mantle recipe combinations and allocate result lists only when matched. Applies on both sides.")
        public boolean optimizeMantleRecipeCombinations = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEnvironmentalTechTargets")
        @Config.Comment("Index standard item and ore weights during large targeted-list builds. Preserves matching order and custom matcher fallbacks. Applies on both sides.")
        public boolean optimizeEnvironmentalTechTargets = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCraftTweakerOutputRemoval")
        @Config.Comment("Skip impossible prefixes of large standard-item recipe removal batches while preserving the original matching loop. Applies on both sides.")
        public boolean optimizeCraftTweakerOutputRemoval = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeOnlyOneItemMatching")
        @Config.Comment("Skip blacklist lookups when OnlyOneItem has no replacement for the item and metadata. Keeps live rules and custom registry-key behavior.")
        public boolean optimizeOnlyOneItemMatching = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeThermalDynamicsFaceMerge")
        @Config.Comment("Avoid pair scans and temporary reversals when Thermal Dynamics' existing face-merging condition cannot pass. Keeps degenerate faces and custom vertex callbacks on the original path.")
        public boolean optimizeThermalDynamicsFaceMerge = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeLibrarianJavaSavedFields")
        @Config.Comment("Skip redundant Kotlin property discovery for ordinary Java classes whose instance fields are all explicitly saved by LibrarianLib. Keeps other property and accessor discovery unchanged.")
        public boolean optimizeLibrarianJavaSavedFields = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeActiniumQuadTreeConstruction")
        @Config.Comment("Create atlas quadtree children when an entry first reaches the node, avoiding construction of empty subtrees. Preserves lookup order and depth limits.")
        public boolean optimizeActiniumQuadTreeConstruction = true;

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
        @Config.Name("OptimizeFramebufferBlit")
        @Config.Comment("Present the vanilla framebuffer with a hardware blit when the driver supports framebuffer blitting; retain the shader path for blending, unsupported drivers and compatibility-sensitive callers.")
        public boolean optimizeFramebufferBlit = true;

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
        @Config.Comment("Thermal Expansion's transposer JEI pages add one auto-generated entry per fluid container item in the pack (pour into it / drain it), and the drain probe behind those rows is the expensive half of its JEI setup. Skip that per-container scan only; the machine's own registered recipes and its JEI catalyst entry stay.")
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
        @Config.Name("OptimizeZenUtilsLogPosition")
        @Config.Comment("Resolve script stack positions only when ZenUtils log filters need them. Applies on both sides.")
        public boolean optimizeZenUtilsLogPosition = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJiebaDictionarySplit")
        @Config.Comment("Compile Jieba's dictionary separator once instead of once per line while Revo UI loads it.")
        public boolean optimizeJiebaDictionarySplit = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJiebaDictionaryCharacters")
        @Config.Comment("Reuse character keys while constructing the Jieba dictionary. Keeps its canonical-character map and trie updates.")
        public boolean optimizeJiebaDictionaryCharacters = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAtlasUploadBinding")
        @Config.Comment("Reuse the physical texture binding within one atlas sprite upload while GLSM's splash-time cache is disabled.")
        public boolean optimizeAtlasUploadBinding = true;

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

        @Config.Name("OptimizeTesrRenderDistance")
        @Config.Comment("Skip a tile entity renderer once its whole render volume sits further away than TesrRenderDistance. Render volumes larger than two chunks and infinite bounding boxes are never skipped.")
        public boolean optimizeTesrRenderDistance = true;

        @Config.Name("TesrRenderDistance")
        @Config.Comment("Block distance past which a tile entity renderer is skipped. 0 disables the check.")
        @Config.RangeInt(min = 0, max = 512)
        public int tesrRenderDistance = 64;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeTesrFrustumCulling")
        @Config.Comment("Cull finite, non-global tile entity renderers outside the current world render camera. Independent of the distance limit; shadow passes are preserved.")
        public boolean optimizeTesrFrustumCulling = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeDraconicModelGeometry")
        @Config.Comment("Move the draconic reactor and chaos stabiliser model transform to the GL matrix and append each cached static model in one bulk vertex write.")
        public boolean optimizeDraconicModelGeometry = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeSoundUpdateWakeups")
        @Config.Comment("Batch sound parameter notifications and wake initialization waits when the sound library finishes. Commands and play/stop notifications remain unchanged.")
        public boolean optimizeSoundUpdateWakeups = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeInventoryTickWork")
        @Config.Comment("Coalesce inventory_changed checks caused by main-inventory slot updates into one evaluation at the end of the player tick. Criteria are evaluated against the final inventory state of that tick.")
        public boolean optimizeInventoryTickWork = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeVintageFixJarCache")
        @Config.Comment("Let VintageFix read back its jar discovery cache by giving its serializer the constructorless instantiator it needs. Applies on both sides.")
        public boolean optimizeVintageFixJarCache = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeNonNullListClear")
        @Config.Comment("Clear a NonNullList through its delegate instead of removing every entry one at a time. Applies on both sides.")
        public boolean optimizeNonNullListClear = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJeiAsyncSearchIndex")
        @Config.Comment("Build the JEI default and tooltip search indexes on HEI's background worker instead of on the loading thread, which shortens start-up by roughly two seconds. Disable if a mod misbehaves when its tooltip code runs off-thread.")
        public boolean optimizeJeiAsyncSearchIndex = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJeiStackUidCache")
        @Config.Comment("Let JEI reuse stack unique identifiers by stack content instead of by instance identity, so repeated ingredients stop being re-identified from scratch.")
        public boolean optimizeJeiStackUidCache = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJeiRecipeId")
        @Config.Comment("Flatten HEI ingredient maps once per recipe ID while preserving hash order.")
        public boolean optimizeJeiRecipeId = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJeiRecipeIndex")
        @Config.Comment("Combine local recipe-ID membership checks with insertion and read cached item hashes without boxing. Preserves ingredient callbacks and recipe ordering.")
        public boolean optimizeJeiRecipeIndex = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJeiRecipeHandlerSelection")
        @Config.Comment("Cache HEI's original handler selection by recipe class and category within each recipe registry.")
        public boolean optimizeJeiRecipeHandlerSelection = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJeiItemList")
        @Config.Comment("Fill the creative tabs of JEI's item list concurrently instead of one after another. Each tab walks the whole item registry on its own, so this is the bulk of the item list build. Disable if a mod misbehaves when its creative tab code runs on another thread.")
        public boolean optimizeJeiItemList = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCustomLoadingScreen")
        @Config.Comment("Skip the second of the two back-to-back resource reloads Custom Loading Screen performs while constructing its renderer; the first one already refreshed every resource pack.")
        public boolean optimizeCustomLoadingScreen = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCodeChickenBlockstateScan")
        @Config.Comment("Answer CodeChicken Lib's per-block blockstate existence probes from StellarCore's classpath asset index instead of walking every resource pack and throwing a FileNotFoundException for each miss.")
        public boolean optimizeCodeChickenBlockstateScan = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCodeChickenErrorStateLog")
        @Config.Comment("CodeChicken Lib logs a FATAL per tick whenever it bakes a block state that declares ModelErrorStateProperty.ERROR_STATE without a value, which is the normal state of every block state that has not been through ModelBakery.handleExtendedState yet; JourneyMap's worker threads hit it on every block they visit. Report it once per game session instead, and keep the model unrendered as before.")
        public boolean optimizeCodeChickenErrorStateLog = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJourneymapBlockSprites")
        @Config.Comment("JourneyMap re-reads the raw block state and bakes it straight from the chunk, which no CodeChicken Lib bakery can bake: the sprite map and the model error state only exist after ModelBakery.handleExtendedState ran. Resolve block sprites from the block's own extended state instead, so blocks baked by CodeChicken Lib (Thermal Expansion and Thermal Dynamics) show their sprites on the map instead of showing nothing.")
        public boolean optimizeJourneymapBlockSprites = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJourneymapChunkDrain")
        @Config.Comment("JourneyMap writes its pending chunks back by rebuilding the pending map's iterator on every loop iteration, so draining N chunks allocates N iterators for a map that only ever needs one. Walk it once and remove through the iterator instead.")
        public boolean optimizeJourneymapChunkDrain = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJourneymapChunkBuffer")
        @Config.Comment("JourneyMap serialises each chunk into a ByteArrayOutputStream that starts at 8 KB and doubles from there, copying the whole buffer on every growth, and throws the stream away once the bytes have been copied out of it. Serve each write from a per-thread buffer that is reset and reused instead, so the stream and its array are allocated once per writing thread rather than once per chunk.")
        public boolean optimizeJourneymapChunkBuffer = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeJourneymapSpriteDedupe")
        @Config.Comment("JourneyMap's sprite pass builds a HashSet and an ArrayList per block purely to drop duplicate quads before reading their sprites, and discards both when the loop ends. Hand both out from a thread local and clear them for the next call instead, so colouring a chunk allocates two containers rather than two per block.")
        public boolean optimizeJourneymapSpriteDedupe = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeChiselCtmConnectionOffsets")
        @Config.Comment("Reuse repeated neighbouring positions while Chisel CTM builds connection maps.")
        public boolean optimizeChiselCtmConnectionOffsets = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEntityLookupIteration")
        @Config.Comment("Use indexed entity lists for vanilla chunk AABB queries and for ClassInheritanceMultiMap type lookups, avoiding short-lived iterator wrappers on the entity query path.")
        public boolean optimizeEntityLookupIteration = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMekanismObjQuads")
        @Config.Comment("Mekanism's OBJ transmitter model cannot answer for the block states JourneyMap asks it about: it keys its cache on unlisted properties, which a state read straight out of the chunk does not carry, so the lookup throws inside its own try block and every call rebuilds every vertex, UV, normal and float array from scratch. Remember the quads it produced for a state instance instead. The list is handed out unchanged, keyed by the state, the render layer and the model.")
        public boolean optimizeMekanismObjQuads = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeResourceLocationStableValue")
        @Config.Comment("Cache ResourceLocation string and hash results, and skip repeated lowercase conversion of its canonical default namespace.")
        public boolean optimizeResourceLocationStableValue = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeFrozenRegistryIdLookup")
        @Config.Comment("Use bounded dense ID lookups while Forge registries are frozen. Preserves iterator order and invalidates on registry mutations and freeze transitions. Applies on both sides.")
        public boolean optimizeFrozenRegistryIdLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMekanismModuleLookup")
        @Config.Comment("Remember the module list Mekanism deserialises for an item stack instead of rebuilding it on every lookup. The MekaSuit armour asks for the modules of its own stack whenever attribute modifiers are recalculated, and each ask re-read the modules NBT and reconstructed every module. The cached list is invalidated by the stack's tag hash, so installing or removing a module is picked up.")
        public boolean optimizeMekanismModuleLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeKonkreteLocalsCopy")
        @Config.Comment("Konkrete unpacks each bundled localisation file into a String before writing it out, concatenating one line at a time, which copies the whole file again on every line and then once more on write. Write each line through as it is read instead: same bytes, one pass. FancyMenu runs this for every translation during client setup.")
        public boolean optimizeKonkreteLocalsCopy = true;

    }

    @SuppressWarnings("CanBeFinal")
    public static class Server {
        @Config.RequiresMcRestart
        @Config.Name("BatchMeteorSpawnWrites")
        @Config.Comment("Enabled by default. Coalesce changed meteor region writes until an AE2 queue drain exits (including exceptions). Flushes at save/unload/server stop and retries failed writes at server tick end. Disable to use synchronous writes.")
        public boolean batchMeteorSpawnWrites = true;
        @Config.RequiresMcRestart
        @Config.Name("OptimizeWorldgenAuxiliaries")
        @Config.Comment("Reuse generation helpers and stable attribute/biome metadata. Keeps world-dependent callbacks live.")
        public boolean optimizeWorldgenAuxiliaries = true;

        @Config.Name("WorldgenOreNotifications")
        @Config.Comment("SILENT_ROCK is the default: skip neighbor and observer notifications for non-tile rock replacements during ore generation. Chunk lifecycle callbacks and lighting still run. OBSERVERS_ONLY retains observer notifications; PRESERVE restores the original notification flags.")
        public WorldgenNotificationMode worldgenOreNotifications = WorldgenNotificationMode.SILENT_ROCK;

        @Config.RequiresMcRestart
        @Config.Name("BulkChunkPrimerImport")
        @Config.Comment("Build supported ChunkPrimer sections through reusable short ID buffers before normal constructor-tail lighting callbacks. Custom state getters and Chunk/primer subclasses use the original loop.")
        public boolean bulkChunkPrimerImport = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeObserverNotifications")
        @Config.Comment("Skip observer state reads for local palettes containing only empty callbacks. Keeps neighbor chunk loading, real callbacks, and their ordering.")
        public boolean optimizeObserverNotifications = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeSetBlockStateReads")
        @Config.Comment("Read the previous state from the chunk already resolved by setBlockState on standard server worlds, retaining the original path for snapshots and custom worlds.")
        public boolean optimizeSetBlockStateReads = true;

        @Config.RequiresMcRestart
        @Config.Name("ReserveChunkPaletteCapacity")
        @Config.Comment("Grow full local section palettes through 4, 6 and 8 bits to avoid repeated repacking. Network-specified widths are unchanged; partially filled palettes may retain extra capacity.")
        public boolean reserveChunkPaletteCapacity = true;

        @Config.RequiresMcRestart
        @Config.Name("UnifiedOreGeneration")
        @Config.Comment("Route standard vanilla and CoFH veins through one deduplicated scanner. Sample the largest overlapping ellipsoid per group for speed, slightly reducing ore volume. Changes vein shapes, random consumption and repeated callback counts.")
        public boolean unifiedOreGeneration = true;

        @Config.RequiresMcRestart
        @Config.Name("GenerateOresInTerrain")
        @Config.Comment("Generate supported vanilla and CoFH ores in new Overworld/RTG terrain before lighting. Changes ore positions and generation timing; persisted chunk markers prevent duplicate decoration.")
        public boolean generateOresInTerrain = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAlfheimDeduplicationSets")
        @Config.Comment("Start Alfheim queues small, defer FIFO shrinking to pass boundaries, and shrink deduplication tables after sustained sparse passes. Insertion and deduplication order are unchanged.")
        public boolean optimizeAlfheimDeduplicationSets = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAlfheimChunkLookup")
        @Config.Comment("Reuse an Alfheim lighting pass's most recently loaded chunk for adjacent light probes, while clearing the lookup at each sky or block light phase.")
        public boolean optimizeAlfheimChunkLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAlfheimNeighborData")
        @Config.Comment("Reuse decoded coordinates and the current section for Alfheim neighbor probes; boundary chunks and custom chunks retain their original lookup path.")
        public boolean optimizeAlfheimNeighborData = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeLeafDecayPropagation")
        @Config.Comment("Propagate leaf support through four queue layers, retaining block probes, Forge callbacks and decay decisions.")
        public boolean optimizeLeafDecayPropagation = true;

        @Config.Name("OptimizeMeteorCompassTargetScan")
        @Config.Comment("Filter BetterMeteorite compass targets before decoding block metadata and cache immutable state matches per thread. Keeps the configured target identities and submitted compass updates.")
        public boolean optimizeMeteorCompassTargetScan = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeRtgBiomeWeights")
        @Config.Comment("Skip zero RTG biome samples without changing float accumulation or terrain evaluation order.")
        public boolean optimizeRtgBiomeWeights = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeRtgCellularBounds")
        @Config.Comment("Skip RTG cellular regions whose exact square-distance lower bound cannot replace either nearest point.")
        public boolean optimizeRtgCellularBounds = true;

        @Config.RequiresMcRestart
        @Config.Name("ForceChunkHandler")
        public boolean forceChunkHandler = true;

        @Config.RequiresMcRestart
        @Config.Name("SpecialMachine")
        public boolean specialMachine = true;

        @Config.RequiresMcRestart
        @Config.Name("bot")
        public boolean bot = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeDECoreHatchScan")
        @Config.Comment("Bind Modular Machinery energy hatches to Draconic Evolution energy cores from Circulation Networks block entity lifecycle events instead of rescanning a cube around every hatch on a timer.")
        public boolean optimizeDECoreHatchScan = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeStructureLookup")
        @Config.Comment("Walk the known structure starts of a map generator from a cached array instead of probing its open hash map on every position query.")
        public boolean optimizeStructureLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeAe2ItemListCapacity")
        @Config.Comment("Start an AE2 item list with the capacity of the largest list seen so far instead of growing it from the default capacity, which removes a chain of rehashes from every item list built for a crafting job.")
        public boolean optimizeAe2ItemListCapacity = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeBiomeIdLookup")
        @Config.Comment("Resolve a biome from a copied id table. A registry keeps its ids in a Guava HashBiMap, so asking it for a biome by id boxes the id and walks a hash bucket, and Forge's wrapper never fills the vanilla array that would answer that in one step; a world generator asks for a biome by id for every chunk it decorates. The table is dropped whenever the registry rewrites its id mapping, including the renumbering done when a world is loaded. Applies on both sides.")
        public boolean optimizeBiomeIdLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeForgeEventDispatch")
        @Config.Comment("Replace EventBus#post so an event that cannot be canceled dispatches without asking every listener whether the event was canceled, and without allocating a callback handle per post; the generic-event filter is still applied. Applies on both sides.")
        public boolean optimizeForgeEventDispatch = true;

        @Config.RequiresMcRestart
        @Config.Comment("Skip empty ZenUtils world-task views while preserving pending-task merge timing and insertion order.")
        public boolean optimizeZenUtilsTickWork = true;

        @Config.RequiresMcRestart
        @Config.Comment("Reuse the parsed IC2 ender-chest configuration path, without caching configuration nodes or values.")
        public boolean optimizeIc2TickConfigPath = true;

        @Config.RequiresMcRestart
        @Config.Comment("Use int[] dimension IDs only in the server tick loop, retaining Forge's periodic leak check and leaving its public Integer[] API unchanged.")
        public boolean optimizeDimensionTickIds = true;

        @Config.RequiresMcRestart
        @Config.Comment("Replace StellarCore's chunk-tick stream with an eager, ordered snapshot backed by bounded reusable arrays. Outstanding iterators keep independent storage.")
        public boolean optimizeChunkTickSnapshot = true;

        @Config.RequiresMcRestart
        @Config.Comment("Reuse Advanced Rocketry's temperature geometry and pressure factors independently as live numerical inputs change, avoid empty atmosphere-blob wrappers and enum values() clones. No world-state or atmosphere-result caching.")
        public boolean optimizeWorldgenAtmosphere = true;

        @Config.RequiresMcRestart
        @Config.Comment("Use primitive-key fastutil maps for private Advanced Rocketry atmosphere handlers and AE2 compass regions. Public signatures and region-coordinate packing remain unchanged.")
        public boolean optimizeWorldgenIdMaps = true;

        @Config.RequiresMcRestart
        @Config.Comment("Reuse CoFH World's immutable per-material replacement predicates and read each candidate state once; replacement checks, RNG calls and block notifications remain unchanged.")
        public boolean optimizeWorldgenOrePredicates = true;

        @Config.RequiresMcRestart
        @Config.Comment("Build AE2 meteor filenames without Formatter on ASCII-digit locales and buffer compressed file input with bounded per-thread reusable storage. Reads still access disk every time; no NBT or missing-file caching.")
        public boolean optimizeMeteorSpawnDataIo = true;

        @Config.RequiresMcRestart
        @Config.Comment("Precompute CoFH cluster ellipsoid Z distances and use their exact float minimum to skip impossible X/Y branches. Float arithmetic, repeated placements, callbacks and random draws remain unchanged.")
        public boolean optimizeOreVeinGeometry = true;

        @Config.RequiresMcRestart
        @Config.Comment("Memoize the seven immutable BlockStone metadata transitions per default-state identity. Changes to the default state invalidate the table.")
        public boolean optimizeStoneMetadataStates = true;

        @Config.RequiresMcRestart
        @Config.Comment("Reuse vanilla ore ellipsoid Z distances and their exact minimum without merging duplicate attempts or changing random calls, predicates or notifications.")
        public boolean optimizeVanillaOreGeometry = true;

        @Config.RequiresMcRestart
        @Config.Comment("Use independent mutable fastutil sets for attribute modifier snapshots. Attribute-value evaluation and its floating-point accumulation order are unchanged.")
        public boolean optimizeAttributeModifierSnapshots = true;

        @Config.RequiresMcRestart
        @Config.Comment("Use fixed-arity random choices in vanilla Zoom and FuzzyZoom biome layers, retaining seeds and IntCache requests. Custom subclasses use their original method.")
        public boolean optimizeBiomeZoomRandom = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeSpawnListResolution")
        @Config.Comment("A spawn attempt resolves the same position's spawn list twice, once to pick a candidate and once to check that candidate. Reuse the first result for the second check, which removes a biome lookup and a PotentialSpawns event per attempt; the memo is dropped as soon as the position or creature type changes. Applies on both sides.")
        public boolean optimizeSpawnListResolution = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeHorologiumAccelerationBlacklist")
        @Config.Comment("Astral Sorcery's Horologium effect asks whether a tile may be accelerated for every candidate it probes and every element it accelerates, and each ask lower-cases the tile class name and walks the blacklist. Answer per tile class instead, dropping the memo whenever a blacklist list grows. Applies on both sides.")
        public boolean optimizeHorologiumAccelerationBlacklist = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMekanismNetworkTargets")
        @Config.Comment("Hand the per-acceptor targets of a Mekanism energy network out again instead of allocating one per acceptor on every tick, which also removes the two enum maps each target builds. Applies on both sides.")
        public boolean optimizeMekanismNetworkTargets = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeBlockStateProperties")
        @Config.Comment("Cache material and light-method dispatch metadata. Default context light methods directly use state getters; custom methods and mutable light values remain live.")
        public boolean optimizeBlockStateProperties = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEntityDataManagerLookup")
        @Config.Comment("Keep a small locked direct-mapped cache for frequently read entity data parameters while preserving the original read lock and map fallback.")
        public boolean optimizeEntityDataManagerLookup = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeChunkPaletteGrowth")
        @Config.Comment("Bulk-decode REID sections and preserve local palette IDs during growth, with a translating fallback for unusual palettes and global IDs. Network packing remains compatible.")
        public boolean optimizeChunkPaletteGrowth = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeUniversalTweaksEntityBlacklist")
        @Config.Comment("Remember Universal Tweaks' entity desync blacklist decision per entity type instead of rescanning the list for every entity and every tracked entry. Applies on both sides.")
        public boolean optimizeUniversalTweaksEntityBlacklist = true;

        @Config.RequiresMcRestart
        @Config.Name("GuardForgeNetworkReplyTarget")
        @Config.Comment("Forge keeps the send target of a packet in a channel attribute instead of on the packet, and OutboundTarget.REPLY - the target used to send a reply back to where it came from - resolves it as ImmutableList.of(packet.getDispatcher()) without the null check that PLAYER and TOSERVER have in the same enum. When another send on that channel, a reply in the case seen here, takes the attribute between the set and the outbound handler's read, a broadcast is dispatched as REPLY; its packet was built by the codec and has no dispatcher, so the NullPointerException that follows is caught by FMLProxyPacket.processPacket, which answers with rejectHandshake(\"A fatal error has occurred, this connection is terminated\") and drops the player out of the world. Drop such a packet silently instead - it could not be delivered under that target anyway. Applies on both sides.")
        public boolean guardForgeNetworkReplyTarget = true;
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
