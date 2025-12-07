package github.kasuminova.novaeng.common

import appeng.api.AEApi
import baubles.api.BaublesApi
import com.circulation.random_complement.common.util.MEHandler
import github.kasuminova.mmce.common.integration.ModIntegrationAE2
import github.kasuminova.novaeng.NovaEngCoreConfig
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.adapter.RecipeAdapterExtended
import github.kasuminova.novaeng.common.container.ContainerECalculatorController
import github.kasuminova.novaeng.common.container.ContainerEFabricatorController
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternBus
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternSearch
import github.kasuminova.novaeng.common.container.ContainerEStorageController
import github.kasuminova.novaeng.common.container.ContainerGeocentricDrill
import github.kasuminova.novaeng.common.container.ContainerHyperNetTerminal
import github.kasuminova.novaeng.common.container.ContainerModularServerAssembler
import github.kasuminova.novaeng.common.container.ContainerNEWCraftConfirm
import github.kasuminova.novaeng.common.container.ContainerSingularityCore
import github.kasuminova.novaeng.common.enchantment.MagicBreaking
import github.kasuminova.novaeng.common.estorage.EStorageCellHandler
import github.kasuminova.novaeng.common.handler.ECalculatorEventHandler
import github.kasuminova.novaeng.common.handler.EFabricatorEventHandler
import github.kasuminova.novaeng.common.handler.EStorageEventHandler
import github.kasuminova.novaeng.common.handler.EnchantmentHandler
import github.kasuminova.novaeng.common.handler.FTBHandler
import github.kasuminova.novaeng.common.handler.HyperNetEventHandler
import github.kasuminova.novaeng.common.handler.HyperNetMachineEventHandler
import github.kasuminova.novaeng.common.handler.IEHandler
import github.kasuminova.novaeng.common.handler.MachineAssemblyHandler
import github.kasuminova.novaeng.common.handler.OreHandler
import github.kasuminova.novaeng.common.handler.WorldLoadedHandler
import github.kasuminova.novaeng.common.hypernet.old.HyperNetTerminal
import github.kasuminova.novaeng.common.hypernet.old.machine.AssemblyLine
import github.kasuminova.novaeng.common.hypernet.old.recipe.HyperNetRecipeManager
import github.kasuminova.novaeng.common.integration.IntegrationCRT
import github.kasuminova.novaeng.common.integration.ic2.IntegrationIC2
import github.kasuminova.novaeng.common.integration.theoneprobe.IntegrationTOP
import github.kasuminova.novaeng.common.machine.BiogenicSimulationComputer
import github.kasuminova.novaeng.common.machine.DreamEnergyCore
import github.kasuminova.novaeng.common.machine.GeocentricDrill
import github.kasuminova.novaeng.common.machine.IllumPool
import github.kasuminova.novaeng.common.machine.MMAltar
import github.kasuminova.novaeng.common.machine.MaterialSequenceProcessing
import github.kasuminova.novaeng.common.machine.SingularityCore
import github.kasuminova.novaeng.common.machine.SpaceGenerator
import github.kasuminova.novaeng.common.machine.drills.DifferentWorld
import github.kasuminova.novaeng.common.machine.drills.ManaOreDrill
import github.kasuminova.novaeng.common.machine.drills.MineralExtractor
import github.kasuminova.novaeng.common.machine.drills.OrichalcosDrill
import github.kasuminova.novaeng.common.machine.drills.SmallOreDrill
import github.kasuminova.novaeng.common.machine.drills.VoidMiner
import github.kasuminova.novaeng.common.registry.RegistryAssembly
import github.kasuminova.novaeng.common.registry.RegistryBlocks
import github.kasuminova.novaeng.common.registry.RegistryHyperNet
import github.kasuminova.novaeng.common.registry.RegistryItems
import github.kasuminova.novaeng.common.registry.RegistryMachineSpecial
import github.kasuminova.novaeng.common.tile.TileHyperNetTerminal
import github.kasuminova.novaeng.common.tile.TileModularServerAssembler
import github.kasuminova.novaeng.common.tile.ecotech.ecalculator.ECalculatorController
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorController
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorPatternBus
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageController
import github.kasuminova.novaeng.common.tile.machine.GeocentricDrillController
import github.kasuminova.novaeng.common.trait.Register.registerModifiers
import github.kasuminova.novaeng.common.util.MachineCoolants
import github.kasuminova.novaeng.mixin.ae2.AccessorCellRegistry
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.base.Mods
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.network.IGuiHandler
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.registry.ForgeRegistries

open class CommonProxy : IGuiHandler {
    init {
        MinecraftForge.EVENT_BUS.register(RegistryBlocks())
        MinecraftForge.EVENT_BUS.register(RegistryItems())
    }

    open fun setColor(od: String?, color: Int) {
    }

    open val isClient: Boolean
        get() = false

    open fun construction() {
        if (Loader.isModLoaded("ecoaeextension")) {
            throw RuntimeException("Repeatedly added MOD:ECOAEExtension")
        }
    }

    open fun preInit() {
        NetworkRegistry.INSTANCE.registerGuiHandler(NovaEngineeringCore.MOD_ID, this)

        MinecraftForge.EVENT_BUS.register(IntegrationCRT.INSTANCE)
        MinecraftForge.EVENT_BUS.register(HyperNetEventHandler.INSTANCE)
        MinecraftForge.EVENT_BUS.register(EStorageEventHandler.INSTANCE)
        MinecraftForge.EVENT_BUS.register(EFabricatorEventHandler.INSTANCE)
        MinecraftForge.EVENT_BUS.register(ECalculatorEventHandler.INSTANCE)
        MinecraftForge.EVENT_BUS.register(WorldLoadedHandler.INSTANCE)
        MinecraftForge.EVENT_BUS.register(EnchantmentHandler.INSTANCE)
        MinecraftForge.EVENT_BUS.register(OreHandler.INSTANCE)
        MinecraftForge.EVENT_BUS.register(MachineAssemblyHandler)

        if (Loader.isModLoaded("ftbquests")) MinecraftForge.EVENT_BUS.register(FTBHandler.INSTANCE)

        if (NovaEngCoreConfig.SERVER.specialMachine) MinecraftForge.EVENT_BUS.register(IEHandler.INSTANCE)

        if (Loader.isModLoaded("ic2")) {
            IntegrationIC2.preInit()
        }

        ForgeRegistries.ENCHANTMENTS.register(MagicBreaking.MAGICBREAKING)
    }

    open fun init() {
        RegistryHyperNet.registerHyperNetNode(
            ResourceLocation(ModularMachinery.MODID, "hypernet_terminal"),
            HyperNetTerminal::class.java
        )
        if (Loader.isModLoaded("theoneprobe")) IntegrationTOP.registerProvider()
        RecipeAdapterExtended.registerAdapter()
        AssemblyLine.registerNetNode()
        HyperNetRecipeManager.registerRecipes()
        if (NovaEngCoreConfig.SERVER.specialMachine) {
            if (Mods.ASTRAL_SORCERY.isPresent && Mods.BOTANIA.isPresent) {
                RegistryMachineSpecial.registrySpecialMachine(IllumPool.INSTANCE)
            }
            if (Mods.GECKOLIB.isPresent) {
                RegistryMachineSpecial.registrySpecialMachine(SingularityCore.INSTANCE)
            }
            if (Mods.BM2.isPresent) {
                RegistryMachineSpecial.registrySpecialMachine(MMAltar)
            }
            RegistryMachineSpecial.registrySpecialMachine(DreamEnergyCore.INSTANCE)
            RegistryMachineSpecial.registrySpecialMachine(GeocentricDrill.INSTANCE)
            if (Loader.isModLoaded("deepmoblearning")) {
                RegistryMachineSpecial.registrySpecialMachine(MaterialSequenceProcessing)
                RegistryMachineSpecial.registrySpecialMachine(BiogenicSimulationComputer)
            }
            if (Loader.isModLoaded("avaritia")) {
                RegistryMachineSpecial.registrySpecialMachine(SpaceGenerator)
            }
            if (Loader.isModLoaded("immersiveengineering")) {
                RegistryMachineSpecial.registrySpecialMachine(MineralExtractor)
                RegistryMachineSpecial.registrySpecialMachine(VoidMiner)
                RegistryMachineSpecial.registrySpecialMachine(DifferentWorld)
                RegistryMachineSpecial.registrySpecialMachine(ManaOreDrill)
                RegistryMachineSpecial.registrySpecialMachine(OrichalcosDrill)
                RegistryMachineSpecial.registrySpecialMachine(SmallOreDrill)
            }
        }
        if (Mods.AE2.isPresent) {
            val handlers = ((AEApi.instance().registries().cell()) as AccessorCellRegistry).getHandlers()
            handlers.add(0, EStorageCellHandler.INSTANCE)
        }
        registerModifiers()
    }

    open fun postInit() {
        RegistryAssembly.regAll()
        MachineCoolants.INSTANCE.init()
        HyperNetMachineEventHandler.registerHandler()
        OreHandler.registry()
    }

    open fun loadComplete() {

    }

    override fun getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        val type = GuiType.entries[MathHelper.clamp(id, 0, GuiType.entries.size - 1)]
        val required = type.requiredTileEntity
        var present: TileEntity? = null
        if (required != null) {
            val te = world.getTileEntity(BlockPos(x, y, z))
            if (te != null && required.isAssignableFrom(te.javaClass)) {
                present = te
            } else {
                return null
            }
        }

        return when (type) {
            GuiType.HYPERNET_TERMINAL -> ContainerHyperNetTerminal(present as TileHyperNetTerminal?, player)
            GuiType.MODULAR_SERVER_ASSEMBLER -> ContainerModularServerAssembler(
                (present as TileModularServerAssembler?)!!,
                player
            )

            GuiType.ESTORAGE_CONTROLLER -> ContainerEStorageController(present as EStorageController?, player)
            GuiType.SINGULARITY_CORE -> ContainerSingularityCore(
                present as github.kasuminova.novaeng.common.tile.machine.SingularityCore?,
                player
            )

            GuiType.EFABRICATOR_CONTROLLER -> {
                val efController = present as? EFabricatorController
                return if (efController != null && efController.channel != null && ModIntegrationAE2.securityCheck(
                        player, efController.channel!!.proxy
                    )
                ) {
                    null
                } else ContainerEFabricatorController(efController, player)
            }

            GuiType.EFABRICATOR_PATTERN_SEARCH -> {
                val efController = present as? EFabricatorController
                return if (efController != null && efController.channel != null && ModIntegrationAE2.securityCheck(
                        player, efController.channel!!.proxy
                    )
                ) {
                    null
                } else ContainerEFabricatorPatternSearch(efController, player)
            }

            GuiType.EFABRICATOR_PATTERN_BUS -> {
                val efPatternBus = present as? EFabricatorPatternBus
                val efController = efPatternBus?.controller
                return if (efController != null && efController.channel != null && ModIntegrationAE2.securityCheck(
                        player, efController.channel!!.proxy
                    )
                ) {
                    null
                } else ContainerEFabricatorPatternBus(efPatternBus, player)
            }

            GuiType.GEOCENTRIC_DRILL_CONTROLLER -> ContainerGeocentricDrill(
                present as GeocentricDrillController?,
                player
            )

            GuiType.ECALCULATOR_CONTROLLER -> {
                val ecController = present as ECalculatorController?
                return if (ecController != null && ecController.channel != null && ModIntegrationAE2.securityCheck(
                        player, ecController.channel.getProxy()
                    )
                ) {
                    null
                } else ContainerECalculatorController(present, player)
            }

            GuiType.AUTO_CRAFTGUI -> {
                val stack = if (y == 1) BaublesApi.getBaublesHandler(player).getStackInSlot(x)
                else player.inventory.getStackInSlot(x)

                MEHandler.getTerminalGuiObject(stack, player, x, y)?.let {
                    return ContainerNEWCraftConfirm(player.inventory, it)
                }
                return null
            }

            GuiType.MACHINE_ASSEMBLY_TOOL -> null
        }
    }

    override fun getClientGuiElement(id: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? {
        return null
    }

    enum class GuiType(@JvmField val requiredTileEntity: Class<out TileEntity?>?) {
        HYPERNET_TERMINAL(TileHyperNetTerminal::class.java),
        MODULAR_SERVER_ASSEMBLER(TileModularServerAssembler::class.java),
        ESTORAGE_CONTROLLER(EStorageController::class.java),
        SINGULARITY_CORE(github.kasuminova.novaeng.common.tile.machine.SingularityCore::class.java),
        EFABRICATOR_CONTROLLER(EFabricatorController::class.java),
        EFABRICATOR_PATTERN_SEARCH(EFabricatorController::class.java),
        EFABRICATOR_PATTERN_BUS(EFabricatorPatternBus::class.java),
        GEOCENTRIC_DRILL_CONTROLLER(GeocentricDrillController::class.java),
        ECALCULATOR_CONTROLLER(ECalculatorController::class.java),
        AUTO_CRAFTGUI(null),
        MACHINE_ASSEMBLY_TOOL(null)
    }
}