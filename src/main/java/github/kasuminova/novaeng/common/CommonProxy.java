package github.kasuminova.novaeng.common;

import appeng.api.AEApi;
import appeng.api.storage.ICellHandler;
import github.kasuminova.mmce.common.integration.ModIntegrationAE2;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.adapter.RecipeAdapterExtended;
import github.kasuminova.novaeng.common.config.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.container.ContainerECalculatorController;
import github.kasuminova.novaeng.common.container.ContainerEFabricatorController;
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternBus;
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternSearch;
import github.kasuminova.novaeng.common.container.ContainerEStorageController;
import github.kasuminova.novaeng.common.container.ContainerGeocentricDrill;
import github.kasuminova.novaeng.common.container.ContainerHyperNetTerminal;
import github.kasuminova.novaeng.common.container.ContainerModularServerAssembler;
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
import github.kasuminova.novaeng.common.handler.OreHandler;
import github.kasuminova.novaeng.common.handler.WorldLoadedHandler;
import github.kasuminova.novaeng.common.hypernet.old.HyperNetTerminal;
import github.kasuminova.novaeng.common.hypernet.old.machine.AssemblyLine;
import github.kasuminova.novaeng.common.hypernet.old.recipe.HyperNetRecipeManager;
import github.kasuminova.novaeng.common.integration.IntegrationCRT;
import github.kasuminova.novaeng.common.integration.ic2.IntegrationIC2;
import github.kasuminova.novaeng.common.integration.theoneprobe.IntegrationTOP;
import github.kasuminova.novaeng.common.machine.BiogenicSimulationComputer;
import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import github.kasuminova.novaeng.common.machine.Drills.DifferentWorld;
import github.kasuminova.novaeng.common.machine.Drills.ManaOreDrill;
import github.kasuminova.novaeng.common.machine.Drills.MineralExtractor;
import github.kasuminova.novaeng.common.machine.Drills.OrichalcosDrill;
import github.kasuminova.novaeng.common.machine.Drills.SmallOreDrill;
import github.kasuminova.novaeng.common.machine.Drills.VoidMiner;
import github.kasuminova.novaeng.common.machine.GeocentricDrill;
import github.kasuminova.novaeng.common.machine.IllumPool;
import github.kasuminova.novaeng.common.machine.MMAltar;
import github.kasuminova.novaeng.common.machine.MaterialSequenceProcessing;
import github.kasuminova.novaeng.common.machine.SingularityCore;
import github.kasuminova.novaeng.common.machine.SpaceGenerator;
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
import github.kasuminova.novaeng.mixin.ae2.AccessorCellRegistry;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
public class CommonProxy implements IGuiHandler {

    public CommonProxy() {
        MinecraftForge.EVENT_BUS.register(new RegistryBlocks());
        MinecraftForge.EVENT_BUS.register(new RegistryItems());
    }

    public boolean isClient(){
        return false;
    }

    public void construction() {
        if (Loader.isModLoaded("ecoaeextension")){
            throw new RuntimeException(I18n.translateToLocal("mod.ecoae.warning"));
        }
    }

    public void preInit() {
        NetworkRegistry.INSTANCE.registerGuiHandler(NovaEngineeringCore.MOD_ID, this);

        MinecraftForge.EVENT_BUS.register(IntegrationCRT.INSTANCE);
        MinecraftForge.EVENT_BUS.register(HyperNetEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EStorageEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EFabricatorEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ECalculatorEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WorldLoadedHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EnchantmentHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(OreHandler.INSTANCE);

        if (Loader.isModLoaded("ftbquests"))
            MinecraftForge.EVENT_BUS.register(FTBHandler.INSTANCE);

        if (NovaEngCoreConfig.SERVER.SpecialMachine)
            MinecraftForge.EVENT_BUS.register(IEHandler.INSTANCE);

        if (Loader.isModLoaded("ic2")) {
            IntegrationIC2.preInit();
        }

        ForgeRegistries.ENCHANTMENTS.register(MagicBreaking.MAGICBREAKING);
    }

    public void init() {
        RegistryHyperNet.registerHyperNetNode(
                new ResourceLocation(ModularMachinery.MODID, "hypernet_terminal"),
                HyperNetTerminal.class
        );
        if (Loader.isModLoaded("theoneprobe")) IntegrationTOP.registerProvider();
        RecipeAdapterExtended.registerAdapter();
        AssemblyLine.registerNetNode();
        HyperNetRecipeManager.registerRecipes();
        if (NovaEngCoreConfig.SERVER.SpecialMachine) {
            if (Mods.ASTRAL_SORCERY.isPresent() && Mods.BOTANIA.isPresent()) {
                RegistryMachineSpecial.registrySpecialMachine(IllumPool.INSTANCE);
            }
            if (Mods.GECKOLIB.isPresent()) {
                RegistryMachineSpecial.registrySpecialMachine(SingularityCore.INSTANCE);
            }
            if (Mods.BM2.isPresent()) {
                RegistryMachineSpecial.registrySpecialMachine(MMAltar.INSTANCE);
            }
            RegistryMachineSpecial.registrySpecialMachine(DreamEnergyCore.INSTANCE);
            RegistryMachineSpecial.registrySpecialMachine(GeocentricDrill.INSTANCE);
            if (Loader.isModLoaded("deepmoblearning")) {
                RegistryMachineSpecial.registrySpecialMachine(MaterialSequenceProcessing.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(BiogenicSimulationComputer.INSTANCE);
            }
            if (Loader.isModLoaded("avaritia")) {
                RegistryMachineSpecial.registrySpecialMachine(SpaceGenerator.INSTANCE);
            }
            if (Loader.isModLoaded("immersiveengineering")) {
                RegistryMachineSpecial.registrySpecialMachine(MineralExtractor.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(VoidMiner.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(DifferentWorld.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(ManaOreDrill.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(OrichalcosDrill.INSTANCE);
                RegistryMachineSpecial.registrySpecialMachine(SmallOreDrill.INSTANCE);
            }
        }
        if (Mods.AE2.isPresent()) {
            List<ICellHandler> handlers = ((AccessorCellRegistry) (AEApi.instance().registries().cell())).getHandlers();
            handlers.add(0, EStorageCellHandler.INSTANCE);
        }
        Register.TRAITREGISTER.registerModifiers();
    }

    public void postInit() {
        MachineCoolants.INSTANCE.init();
        HyperNetMachineEventHandler.registerHandler();
        OreHandler.registry();
    }

    public void loadComplete() {

    }

    @Nullable
    @Override
    public Object getServerGuiElement(final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        GuiType type = GuiType.values()[MathHelper.clamp(ID, 0, GuiType.values().length - 1)];
        Class<? extends TileEntity> required = type.requiredTileEntity;
        TileEntity present = null;
        if (required != null) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te != null && required.isAssignableFrom(te.getClass())) {
                present = te;
            } else {
                return null;
            }
        }

        return switch (type) {
            case HYPERNET_TERMINAL -> new ContainerHyperNetTerminal((TileHyperNetTerminal) present, player);
            case MODULAR_SERVER_ASSEMBLER -> new ContainerModularServerAssembler((TileModularServerAssembler) present, player);
            case ESTORAGE_CONTROLLER -> new ContainerEStorageController((EStorageController) present, player);
            case SINGULARITY_CORE -> new ContainerSingularityCore((github.kasuminova.novaeng.common.tile.machine.SingularityCore) present, player);
            case EFABRICATOR_CONTROLLER -> {
                EFabricatorController efController = (EFabricatorController) present;
                if (efController.getChannel() != null && ModIntegrationAE2.securityCheck(player, efController.getChannel().getProxy())) {
                    yield null;
                }
                yield new ContainerEFabricatorController(efController, player);
            }
            case EFABRICATOR_PATTERN_SEARCH -> {
                EFabricatorController efController = (EFabricatorController) present;
                if (efController.getChannel() != null && ModIntegrationAE2.securityCheck(player, efController.getChannel().getProxy())) {
                    yield null;
                }
                yield new ContainerEFabricatorPatternSearch(efController, player);
            }
            case EFABRICATOR_PATTERN_BUS -> {
                EFabricatorPatternBus efPatternBus = (EFabricatorPatternBus) present;
                EFabricatorController efController = efPatternBus.getController();
                if (efController != null && efController.getChannel() != null && ModIntegrationAE2.securityCheck(player, efController.getChannel().getProxy())) {
                    yield null;
                }
                yield new ContainerEFabricatorPatternBus(efPatternBus, player);
            }
            case GEOCENTRIC_DRILL_CONTROLLER -> new ContainerGeocentricDrill((GeocentricDrillController) present, player);
            case ECALCULATOR_CONTROLLER -> {
                ECalculatorController ecController = (ECalculatorController) present;
                if (ecController.getChannel() != null && ModIntegrationAE2.securityCheck(player, ecController.getChannel().getProxy())) {
                    yield null;
                }
                yield new ContainerECalculatorController((ECalculatorController) present, player);
            }
        };
    }

    @Nullable
    @Override
    public Object getClientGuiElement(final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        return null;
    }

    public enum GuiType {
        HYPERNET_TERMINAL(TileHyperNetTerminal.class),
        MODULAR_SERVER_ASSEMBLER(TileModularServerAssembler.class),
        ESTORAGE_CONTROLLER(EStorageController.class),
        SINGULARITY_CORE(github.kasuminova.novaeng.common.tile.machine.SingularityCore.class),
        EFABRICATOR_CONTROLLER(EFabricatorController.class),
        EFABRICATOR_PATTERN_SEARCH(EFabricatorController.class),
        EFABRICATOR_PATTERN_BUS(EFabricatorPatternBus.class),
        GEOCENTRIC_DRILL_CONTROLLER(GeocentricDrillController.class),
        ECALCULATOR_CONTROLLER(ECalculatorController.class),
        ;

        public final Class<? extends TileEntity> requiredTileEntity;

        GuiType(@Nullable Class<? extends TileEntity> requiredTileEntity) {
            this.requiredTileEntity = requiredTileEntity;
        }
    }
}
