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

    @Config.Name("Network")
    public static Network NETWORK = new Network();

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
        @Config.Comment("Cull tile entity renderers whose render volume lies entirely outside the view frustum. The renderer only culls whole chunks, so a renderer in a visible chunk but behind the camera is drawn in full; nothing it draws can reach the screen.")
        public boolean optimizeTesrFrustumCulling = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeDraconicModelGeometry")
        @Config.Comment("The draconic reactor core and chaos stabiliser core draw one CodeChicken Lib model every frame with a spin transform as their only operation, and the library applies it to every vertex while writing them. Move the transform to the GL matrix and remember the vertices, so a frame costs one addVertexData per vertex instead of the library's per-vertex pipeline. The shaders these renderers use read the standard matrices and follow on their own.")
        public boolean optimizeDraconicModelGeometry = true;

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
        @Config.Name("OptimizeJeiItemList")
        @Config.Comment("Fill the creative tabs of JEI's item list concurrently instead of one after another. Each tab walks the whole item registry on its own, so this is the bulk of the item list build. Disable if a mod misbehaves when its creative tab code runs on another thread.")
        public boolean optimizeJeiItemList = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeCustomLoadingScreen")
        @Config.Comment("Skip the second of the two back-to-back resource reloads Custom Loading Screen performs while constructing its renderer; the first one already refreshed every resource pack.")
        public boolean optimizeCustomLoadingScreen = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeLibrarianLibFieldScan")
        @Config.Comment("Skip the kotlin-reflect property walk LibrarianLib performs on every registered class while building its save field cache, for classes that were not compiled by Kotlin and therefore have no Kotlin properties.")
        public boolean optimizeLibrarianLibFieldScan = true;

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
        @Config.Comment("Chisel's CTM resolves the neighbouring block of a face through Dir.applyConnection, which is pos.add(offset), and CTMLogic.buildConnectionMap asks all eight directions of that face - each one with the same position and facing, so each one computing the same neighbour and allocating a BlockPos for it. Remember the last answer per thread so the repeat asks reuse it. While sections are being meshed this was the largest single allocation site in the client, about 30% of everything allocated.")
        public boolean optimizeChiselCtmConnectionOffsets = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeEntityLookupIteration")
        @Config.Comment("ClassInheritanceMultiMap.getByClass returns an anonymous Iterable whose iterator() takes an iterator from the section's list and wraps it in a filter - three throwaway objects per call, on the path every World.getEntitiesWithinAABB takes. Walk the list by index with the same isInstance test instead, which allocates one object instead of three. This is an overwrite with no runtime fallback, so the setting only takes effect when the mixin is applied.")
        public boolean optimizeEntityLookupIteration = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeMekanismObjQuads")
        @Config.Comment("Mekanism's OBJ transmitter model cannot answer for the block states JourneyMap asks it about: it keys its cache on unlisted properties, which a state read straight out of the chunk does not carry, so the lookup throws inside its own try block and every call rebuilds every vertex, UV, normal and float array from scratch. Remember the quads it produced for a state instance instead. The list is handed out unchanged, keyed by the state, the render layer and the model.")
        public boolean optimizeMekanismObjQuads = true;

        @Config.RequiresMcRestart
        @Config.Name("OptimizeResourceLocationStableValue")
        @Config.Comment("Serve ResourceLocation.toString() and hashCode() from one lazily built stable string instead of concatenating (and re-hashing) namespace and path on every call. Costs one extra reference per instance and keeps one string per location alive; disable to restore the vanilla behaviour.")
        public boolean optimizeResourceLocationStableValue = true;

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
        @Config.Name("OptimizeChunkPaletteGrowth")
        @Config.Comment("Grow a chunk section's block state palette by two bits per overflow instead of one, which halves how often the section's 4096 entries have to be rebuilt through a fresh palette while the section is read from NBT.")
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
