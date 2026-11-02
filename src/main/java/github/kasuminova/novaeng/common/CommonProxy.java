package github.kasuminova.novaeng.common;

import appeng.api.AEApi;
import baubles.api.BaublesApi;
import com.circulation.random_complement.common.util.MEHandler;
import github.kasuminova.mmce.common.integration.ModIntegrationAE2;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.adapter.RecipeAdapterExtended;
import github.kasuminova.novaeng.common.container.ContainerECalculatorController;
import github.kasuminova.novaeng.common.container.ContainerEFabricatorController;
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternBus;
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternSearch;
import github.kasuminova.novaeng.common.container.ContainerEStorageController;
import github.kasuminova.novaeng.common.container.ContainerGeocentricDrill;
import github.kasuminova.novaeng.common.container.ContainerHyperNetTerminal;
import github.kasuminova.novaeng.common.container.ContainerModularServerAssembler;
import github.kasuminova.novaeng.common.container.ContainerNEWCraftConfirm;
import github.kasuminova.novaeng.common.container.ContainerSingularityCore;
import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import github.kasuminova.novaeng.common.estorage.EStorageCellHandler;
import github.kasuminova.novaeng.common.handler.ECalculatorEventHandler;
import github.kasuminova.novaeng.common.handler.EFabricatorEventHandler;
import github.kasuminova.novaeng.common.handler.EStorageEventHandler;
import github.kasuminova.novaeng.common.handler.EnchantmentHandler;
import github.kasuminova.novaeng.common.handler.FTBHandler;
import github.kasuminova.novaeng.common.handler.HyperNetEventHandler;
import github.kasuminova.novaeng.common.handler.HyperNetMachineEventHandler;
import github.kasuminova.novaeng.common.handler.IEHandler;
import github.kasuminova.novaeng.common.handler.MachineAssemblyHandler;
import github.kasuminova.novaeng.common.handler.OreHandler;
import github.kasuminova.novaeng.common.handler.RegistryRemapHandler;
import github.kasuminova.novaeng.common.handler.WorldCacheCleanupHandler;
import github.kasuminova.novaeng.common.handler.WorldLoadedHandler;
import github.kasuminova.novaeng.common.hypernet.old.HyperNetTerminal;
import github.kasuminova.novaeng.common.hypernet.old.machine.AssemblyLine;
import github.kasuminova.novaeng.common.hypernet.old.recipe.HyperNetRecipeManager;
import github.kasuminova.novaeng.common.integration.IntegrationCRT;
import github.kasuminova.novaeng.common.integration.ic2.IntegrationIC2;
import github.kasuminova.novaeng.common.integration.theoneprobe.IntegrationTOP;
import github.kasuminova.novaeng.common.machine.BiogenicSimulationComputer;
import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import github.kasuminova.novaeng.common.machine.GeocentricDrill;
import github.kasuminova.novaeng.common.machine.IllumPool;
import github.kasuminova.novaeng.common.machine.LifeExtractsAltar;
import github.kasuminova.novaeng.common.machine.MMAltar;
import github.kasuminova.novaeng.common.machine.MaterialSequenceProcessing;
import github.kasuminova.novaeng.common.machine.SingularityCore;
import github.kasuminova.novaeng.common.machine.SpaceGenerator;
import github.kasuminova.novaeng.common.machine.drills.DifferentWorld;
import github.kasuminova.novaeng.common.machine.drills.ManaOreDrill;
import github.kasuminova.novaeng.common.machine.drills.MineralExtractor;
import github.kasuminova.novaeng.common.machine.drills.OrichalcosDrill;
import github.kasuminova.novaeng.common.machine.drills.SmallOreDrill;
import github.kasuminova.novaeng.common.machine.drills.VoidMiner;
import github.kasuminova.novaeng.common.registry.RegistryAssembly;
import github.kasuminova.novaeng.common.registry.RegistryBlocks;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import github.kasuminova.novaeng.common.registry.RegistryMachineSpecial;
import github.kasuminova.novaeng.common.tile.TileHyperNetTerminal;
import github.kasuminova.novaeng.common.tile.TileModularServerAssembler;
import github.kasuminova.novaeng.common.tile.ecotech.ecalculator.ECalculatorController;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorController;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorPatternBus;
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageController;
import github.kasuminova.novaeng.common.tile.machine.GeocentricDrillController;
import github.kasuminova.novaeng.common.trait.Register;
import github.kasuminova.novaeng.common.util.MachineCoolants;
import github.kasuminova.novaeng.common.util.MixinDecisions;
import github.kasuminova.novaeng.mixin.ae2.AccessorCellRegistry;
import github.kasuminova.novaeng.novaeng_core.Tags;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class CommonProxy implements IGuiHandler {

    public CommonProxy() {
        MinecraftForge.EVENT_BUS.register(new RegistryBlocks());
        MinecraftForge.EVENT_BUS.register(new RegistryItems());
        MinecraftForge.EVENT_BUS.register(RegistryRemapHandler.INSTANCE);
    }

    private static boolean hasDeniedAccess(final EFabricatorController controller, final EntityPlayer player) {
        return controller != null && controller.getChannel() != null && ModIntegrationAE2.securityCheck(player, controller.getChannel().getProxy());
    }

    public void setColor(final String ore, final int color) {
    }

    public boolean isClient() {
        return false;
    }

    public void construction() {
        if (Loader.isModLoaded("ecoaeextension")) {
            throw new RuntimeException("Repeatedly added MOD:ECOAEExtension");
        }
    }

    public void preInit() {
        NetworkRegistry.INSTANCE.registerGuiHandler(Tags.MOD_ID, this);
        MinecraftForge.EVENT_BUS.register(IntegrationCRT.INSTANCE);
        MinecraftForge.EVENT_BUS.register(HyperNetEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EStorageEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EFabricatorEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ECalculatorEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WorldLoadedHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WorldCacheCleanupHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EnchantmentHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(OreHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(MachineAssemblyHandler.INSTANCE);
        if (Loader.isModLoaded("ftbquests")) {
            MinecraftForge.EVENT_BUS.register(FTBHandler.INSTANCE);
        }
        if (NovaEngCoreConfig.SERVER.specialMachine) {
            MinecraftForge.EVENT_BUS.register(IEHandler.INSTANCE);
        }
        if (MixinDecisions.ic2Loaded) {
            IntegrationIC2.preInit();
        }
        ForgeRegistries.ENCHANTMENTS.register(MagicBreaking.MAGICBREAKING);
    }

    public void init() {
        RegistryHyperNet.registerHyperNetNode(new ResourceLocation(ModularMachinery.MODID, "hypernet_terminal"), HyperNetTerminal.class);
        if (Loader.isModLoaded("theoneprobe")) {
            IntegrationTOP.registerProvider();
        }
        RecipeAdapterExtended.registerAdapter();
        AssemblyLine.registerNetNode();
        HyperNetRecipeManager.registerRecipes();
        if (NovaEngCoreConfig.SERVER.specialMachine) {
            if (Mods.ASTRAL_SORCERY.isPresent() && Mods.BOTANIA.isPresent()) {
                RegistryMachineSpecial.registrySpecialMachine(IllumPool.INSTANCE);
            }
            if (Mods.GECKOLIB.isPresent()) {
                RegistryMachineSpecial.registrySpecialMachine(SingularityCore.INSTANCE);
            }
            if (Mods.BM2.isPresent()) {
                RegistryMachineSpecial.registrySpecialMachine(MMAltar.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(LifeExtractsAltar.INSTANCE);
            }
            RegistryMachineSpecial.registrySpecialMachine(DreamEnergyCore.INSTANCE);
            RegistryMachineSpecial.registrySpecialMachine(GeocentricDrill.INSTANCE);
            if (MixinDecisions.deepMobLearningLoaded) {
                RegistryMachineSpecial.registrySpecialMachine(MaterialSequenceProcessing.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(BiogenicSimulationComputer.INSTANCE);
            }
            if (MixinDecisions.avaritiaLoaded) {
                RegistryMachineSpecial.registrySpecialMachine(SpaceGenerator.INSTANCE);
            }
            if (MixinDecisions.immersiveEngineeringLoaded) {
                RegistryMachineSpecial.registrySpecialMachine(MineralExtractor.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(VoidMiner.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(DifferentWorld.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(ManaOreDrill.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(OrichalcosDrill.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(SmallOreDrill.INSTANCE);
            }
        }
        if (Mods.AE2.isPresent()) {
            ((AccessorCellRegistry) AEApi.instance().registries().cell()).getHandlers().addFirst(EStorageCellHandler.INSTANCE);
        }
        Register.INSTANCE.registerModifiers();
    }

    public void postInit() {
        RegistryAssembly.INSTANCE.regAll();
        MachineCoolants.INSTANCE.init();
        HyperNetMachineEventHandler.registerHandler();
        OreHandler.registry();
    }

    public void loadComplete() {
    }

    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        final var types = GuiType.VALUES;
        final var type = types[MathHelper.clamp(id, 0, types.length - 1)];
        TileEntity present = null;
        if (type.requiredTileEntity != null) {
            present = world.getTileEntity(new BlockPos(x, y, z));
            if (!type.requiredTileEntity.isInstance(present)) {
                return null;
            }
        }
        return switch (type) {
            case HYPERNET_TERMINAL -> new ContainerHyperNetTerminal((TileHyperNetTerminal) present, player);
            case MODULAR_SERVER_ASSEMBLER ->
                new ContainerModularServerAssembler((TileModularServerAssembler) present, player);
            case ESTORAGE_CONTROLLER -> new ContainerEStorageController((EStorageController) present, player);
            case SINGULARITY_CORE ->
                new ContainerSingularityCore((github.kasuminova.novaeng.common.tile.machine.SingularityCore) present, player);
            case EFABRICATOR_CONTROLLER -> {
                final var controller = (EFabricatorController) present;
                yield hasDeniedAccess(controller, player) ? null : new ContainerEFabricatorController(controller, player);
            }
            case EFABRICATOR_PATTERN_SEARCH -> {
                final var controller = (EFabricatorController) present;
                yield hasDeniedAccess(controller, player) ? null : new ContainerEFabricatorPatternSearch(controller, player);
            }
            case EFABRICATOR_PATTERN_BUS -> {
                final var bus = (EFabricatorPatternBus) present;
                yield hasDeniedAccess(bus == null ? null : bus.getController(), player) ? null : new ContainerEFabricatorPatternBus(bus, player);
            }
            case GEOCENTRIC_DRILL_CONTROLLER ->
                new ContainerGeocentricDrill((GeocentricDrillController) present, player);
            case ECALCULATOR_CONTROLLER -> {
                final var controller = (ECalculatorController) present;
                yield controller != null && controller.getChannel() != null && ModIntegrationAE2.securityCheck(player, controller.getChannel().getProxy())
                    ? null : new ContainerECalculatorController(controller, player);
            }
            case AUTO_CRAFTGUI -> {
                final var stack = y == 1 ? BaublesApi.getBaublesHandler(player).getStackInSlot(x) : player.inventory.getStackInSlot(x);
                final var terminal = MEHandler.getTerminalGuiObject(stack, player, x, y);
                yield terminal == null ? null : new ContainerNEWCraftConfirm(player.inventory, terminal);
            }
            case MACHINE_ASSEMBLY_TOOL -> null;
        };
    }

    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        return null;
    }

    public enum GuiType {
        HYPERNET_TERMINAL(TileHyperNetTerminal.class), MODULAR_SERVER_ASSEMBLER(TileModularServerAssembler.class),
        ESTORAGE_CONTROLLER(EStorageController.class), SINGULARITY_CORE(github.kasuminova.novaeng.common.tile.machine.SingularityCore.class),
        EFABRICATOR_CONTROLLER(EFabricatorController.class), EFABRICATOR_PATTERN_SEARCH(EFabricatorController.class),
        EFABRICATOR_PATTERN_BUS(EFabricatorPatternBus.class), GEOCENTRIC_DRILL_CONTROLLER(GeocentricDrillController.class),
        ECALCULATOR_CONTROLLER(ECalculatorController.class), AUTO_CRAFTGUI(null), MACHINE_ASSEMBLY_TOOL(null);
        public static final GuiType[] VALUES = values();
        public final Class<? extends TileEntity> requiredTileEntity;

        GuiType(final Class<? extends TileEntity> type) {
            this.requiredTileEntity = type;
        }
    }
}
