@file:Suppress("DEPRECATION")

package github.kasuminova.novaeng.common.registry

import github.kasuminova.novaeng.common.util.Functions
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager
import github.kasuminova.novaeng.mixin.mmce.AccessorAbstractMachine
import hellfirepvp.astralsorcery.common.lib.BlocksAS
import hellfirepvp.astralsorcery.common.lib.MultiBlockArrays
import hellfirepvp.astralsorcery.common.structure.array.PatternBlockArray
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.util.BlockArray
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor
import it.unimi.dsi.fastutil.objects.ObjectLists
import mekanism.generators.common.GeneratorsBlocks
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.translation.I18n
import vazkii.botania.common.block.ModBlocks

@Suppress("DEPRECATION", "KotlinConstantConditions")
object RegistryAssembly {

    fun packBlock(block: IBlockState): BlockArray.BlockInformation {
        return BlockArray.BlockInformation(ObjectLists.singleton(IBlockStateDescriptor(block)))
    }

    fun regAll() {
        regAS()
        regMEK()
        regBot()
    }

    private fun regAS() {
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.blockAltar, 1),
            transformationBlockArrays(
                MultiBlockArrays.patternAltarAttunement,
                "AltarAttunement".camelToSnake()
            ).setName("tile.blockaltar.altar_2.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.blockAltar, 2),
            transformationBlockArrays(
                MultiBlockArrays.patternAltarConstellation,
                "AltarConstellation".camelToSnake()
            ).setName("tile.blockaltar.altar_3.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.blockAltar, 3),
            transformationBlockArrays(
                MultiBlockArrays.patternAltarTrait,
                "AltarTrait".camelToSnake()
            ).setName("tile.blockaltar.altar_4.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.ritualPedestal, 0),
            transformationBlockArrays(
                MultiBlockArrays.patternRitualPedestal,
                "RitualPedestal".camelToSnake()
            ).setName("tile.blockritualpedestal.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.blockBore, 0),
            transformationBlockArrays(
                MultiBlockArrays.patternFountain,
                "Fountain".camelToSnake()
            ).setName("tile.blockfountain.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.attunementRelay, 0),
            transformationBlockArrays(
                MultiBlockArrays.patternCollectorRelay,
                "CollectorRelay".camelToSnake()
            ).setName("tile.blockattunementrelay.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.attunementAltar, 0),
            transformationBlockArrays(
                MultiBlockArrays.patternAttunementFrame,
                "AttunementFrame".camelToSnake()
            ).setName("tile.blockstructural.attunement_altar_struct.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.celestialGateway, 0),
            transformationBlockArrays(
                MultiBlockArrays.patternCelestialGateway,
                "CelestialGateway".camelToSnake()
            ).setName("tile.blockcelestialgateway.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.starlightInfuser, 0),
            transformationBlockArrays(
                MultiBlockArrays.patternStarlightInfuser,
                "StarlightInfuser".camelToSnake()
            ).setName("tile.blockstarlightinfuser.name")
        )
        NEWMachineAssemblyManager.setConstructors(
            NEWMachineAssemblyManager.BlockPair(BlocksAS.celestialCollectorCrystal, 0),
            transformationBlockArrays(
                MultiBlockArrays.patternCollectorEnhancement,
                "CollectorEnhancement".camelToSnake()
            ).setName("research.ENHANCED_COLLECTOR.name")
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun regMEK() {
        val reactorMachine = DynamicMachine("")
        if (reactorMachine is AccessorAbstractMachine) {
            reactorMachine.setRL(ResourceLocation("mek", "reactor"))
        }
        val reactor = NEWMachineAssemblyManager.BlockPair(GeneratorsBlocks.Reactor, 0)
        val mekReactor = reactorMachine.pattern
        val reactorGlass0 = IBlockStateDescriptor(GeneratorsBlocks.ReactorGlass.getStateFromMeta(0))
        val reactorGlass1 = IBlockStateDescriptor(GeneratorsBlocks.ReactorGlass.getStateFromMeta(1))
        val reactor1 = IBlockStateDescriptor(GeneratorsBlocks.Reactor.getStateFromMeta(1))
        val reactor2 = IBlockStateDescriptor(GeneratorsBlocks.Reactor.getStateFromMeta(2))
        val reactor3 = IBlockStateDescriptor(GeneratorsBlocks.Reactor.getStateFromMeta(3))

        val framework = BlockArray.BlockInformation(ObjectLists.singleton(reactor1))
        val functional = BlockArray.BlockInformation(
            Functions.asList(
                reactorGlass0,
                reactor1,
                reactor2,
                reactor3
            )
        )
        val center = BlockArray.BlockInformation(
            Functions.asList(
                reactorGlass1,
                reactor1,
                reactorGlass0,
                reactor2,
                reactor3
            )
        )
        val p = arrayOfNulls<Array<XY>>(5)
        var u = -1
        for (i in -2..2) {
            val pos = arrayOfNulls<XY>(5)
            var j = -1
            for (k in -2..2) {
                pos[++j] = XY(i, k)
            }
            p[++u] = pos as Array<XY>
        }
        /**
         *  X X X X X
         *  X X X X X
         *  X X X X X
         *  X X X X X
         *  X X X X X
         */
        val poss = p as Array<Array<XY>>

        mekReactor.addBlock(0, 0, 0, packBlock(reactor.blockState))

        mekReactor.addBlock(poss[0][2][0], framework)
        mekReactor.addBlock(poss[1][1][0], framework)
        mekReactor.addBlock(poss[1][2][0], functional)
        mekReactor.addBlock(poss[1][3][0], framework)
        mekReactor.addBlock(poss[2][0][0], framework)
        mekReactor.addBlock(poss[2][1][0], functional)
        mekReactor.addBlock(poss[2][3][0], functional)
        mekReactor.addBlock(poss[2][4][0], framework)
        mekReactor.addBlock(poss[3][1][0], framework)
        mekReactor.addBlock(poss[3][2][0], functional)
        mekReactor.addBlock(poss[3][3][0], framework)
        mekReactor.addBlock(poss[4][2][0], framework)
        mekReactor.addBlock(poss[0][1][-1], framework)
        mekReactor.addBlock(poss[0][2][-1], functional)
        mekReactor.addBlock(poss[0][3][-1], framework)
        mekReactor.addBlock(poss[1][0][-1], framework)
        mekReactor.addBlock(poss[1][4][-1], framework)
        mekReactor.addBlock(poss[2][0][-1], functional)
        mekReactor.addBlock(poss[2][4][-1], functional)
        mekReactor.addBlock(poss[3][0][-1], framework)
        mekReactor.addBlock(poss[3][4][-1], framework)
        mekReactor.addBlock(poss[4][1][-1], framework)
        mekReactor.addBlock(poss[4][2][-1], functional)
        mekReactor.addBlock(poss[4][3][-1], framework)
        mekReactor.addBlock(poss[0][0][-2], framework)
        mekReactor.addBlock(poss[0][1][-2], functional)
        mekReactor.addBlock(poss[0][2][-2], center)
        mekReactor.addBlock(poss[0][3][-2], functional)
        mekReactor.addBlock(poss[0][4][-2], framework)
        mekReactor.addBlock(poss[1][0][-2], functional)
        mekReactor.addBlock(poss[1][4][-2], functional)
        mekReactor.addBlock(poss[2][0][-2], center)
        mekReactor.addBlock(poss[2][4][-2], center)
        mekReactor.addBlock(poss[3][0][-2], functional)
        mekReactor.addBlock(poss[3][4][-2], functional)
        mekReactor.addBlock(poss[4][0][-2], framework)
        mekReactor.addBlock(poss[4][1][-2], functional)
        mekReactor.addBlock(poss[4][2][-2], center)
        mekReactor.addBlock(poss[4][3][-2], functional)
        mekReactor.addBlock(poss[4][4][-2], framework)
        mekReactor.addBlock(poss[0][1][-3], framework)
        mekReactor.addBlock(poss[0][2][-3], functional)
        mekReactor.addBlock(poss[0][3][-3], framework)
        mekReactor.addBlock(poss[1][0][-3], framework)
        mekReactor.addBlock(poss[1][4][-3], framework)
        mekReactor.addBlock(poss[2][0][-3], functional)
        mekReactor.addBlock(poss[2][4][-3], functional)
        mekReactor.addBlock(poss[3][0][-3], framework)
        mekReactor.addBlock(poss[3][4][-3], framework)
        mekReactor.addBlock(poss[4][1][-3], framework)
        mekReactor.addBlock(poss[4][2][-3], functional)
        mekReactor.addBlock(poss[4][3][-3], framework)
        mekReactor.addBlock(poss[0][2][-4], framework)
        mekReactor.addBlock(poss[1][1][-4], framework)
        mekReactor.addBlock(poss[1][2][-4], functional)
        mekReactor.addBlock(poss[1][3][-4], framework)
        mekReactor.addBlock(poss[2][0][-4], framework)
        mekReactor.addBlock(poss[2][1][-4], functional)
        mekReactor.addBlock(poss[2][2][-4], functional)
        mekReactor.addBlock(poss[2][3][-4], functional)
        mekReactor.addBlock(poss[2][4][-4], framework)
        mekReactor.addBlock(poss[3][1][-4], framework)
        mekReactor.addBlock(poss[3][2][-4], functional)
        mekReactor.addBlock(poss[3][3][-4], framework)
        mekReactor.addBlock(poss[4][2][-4], framework)

        NEWMachineAssemblyManager.setConstructors(
            reactor,
            reactorMachine.setName("tile.Reactor.name")
        )
    }

    private fun regBot() {
        val terraPlateMachine = DynamicMachine("")
        if (terraPlateMachine is AccessorAbstractMachine) {
            terraPlateMachine.setRL(ResourceLocation("botania", "terra_plate"))
        }
        val terraplateArray = terraPlateMachine.pattern
        val stone = packBlock(ModBlocks.livingrock.defaultState)
        val lapis = packBlock(Blocks.LAPIS_BLOCK.defaultState)
        val terraPlate = NEWMachineAssemblyManager.BlockPair(ModBlocks.terraPlate, 0)

        terraplateArray.addBlock(0, 0, 0, packBlock(terraPlate.blockState))
        terraplateArray.addBlock(0, -1, 0, stone)
        terraplateArray.addBlock(-1, -1, -1, stone)
        terraplateArray.addBlock(-1, -1, 1, stone)
        terraplateArray.addBlock(1, -1, -1, stone)
        terraplateArray.addBlock(1, -1, 1, stone)
        terraplateArray.addBlock(0, -1, 1, lapis)
        terraplateArray.addBlock(0, -1, -1, lapis)
        terraplateArray.addBlock(1, -1, 0, lapis)
        terraplateArray.addBlock(-1, -1, 0, lapis)

        NEWMachineAssemblyManager.setConstructors(
            terraPlate,
            terraPlateMachine.setName("tile.botania:terraPlate.name")
        )
    }

    private class XY(val x: Int, val z: Int) {
        operator fun get(y: Int): BlockPos {
            return BlockPos(x, y, z)
        }
    }

    private fun transformationBlockArrays(array: PatternBlockArray, name: String): DynamicMachine {
        val machine = DynamicMachine("")
        if (machine is AccessorAbstractMachine) {
            machine.setRL(ResourceLocation("astralsorcery", name))
        }
        val newBlcokArray = machine.pattern
        for (entry in array.pattern) {
            val info = BlockArray.BlockInformation(ObjectLists.singleton(IBlockStateDescriptor(entry.value.state)))
            newBlcokArray.addBlock(entry.key, info)
        }
        return machine
    }

    private fun DynamicMachine.setName(name: String): DynamicMachine {
        this.localizedName = I18n.translateToLocal(name)
        return this
    }

    /**
     * @param this 驼峰结构的字符串
     * @return 蛇形结构的字符串
     */
    private fun String.camelToSnake(): String {
        val len = this.length
        val buffer = CharArray(len * 2)
        var index = 0
        var prevWasLower = false

        for (i in 0..<len) {
            val c = this[i]
            val isUpper = (c in 'A'..'Z')
            val isLower = (c in 'a'..'z')

            if (isUpper) {
                if (prevWasLower) {
                    buffer[index++] = '_'
                }
                buffer[index++] = (c.code + 32).toChar()
                prevWasLower = false
            } else {
                buffer[index++] = c
                prevWasLower = isLower
            }
        }
        return String(buffer, 0, index)
    }
}