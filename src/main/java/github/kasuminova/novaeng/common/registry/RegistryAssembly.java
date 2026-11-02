package github.kasuminova.novaeng.common.registry;

import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.novaeng.common.util.Functions;
import github.kasuminova.novaeng.common.util.MixinDecisions;
import github.kasuminova.novaeng.common.util.NEWDynamicMachine;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager;
import github.kasuminova.novaeng.mixin.mmce.AccessorAbstractMachine;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import hellfirepvp.astralsorcery.common.lib.MultiBlockArrays;
import hellfirepvp.astralsorcery.common.structure.array.PatternBlockArray;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import mekanism.common.MekanismBlocks;
import mekanism.generators.common.GeneratorsBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import thelm.packagedastral.block.BlockAttunementCrafter;
import thelm.packagedastral.block.BlockConstellationCrafter;
import thelm.packagedastral.block.BlockTraitCrafter;
import vazkii.botania.common.block.ModBlocks;

@SuppressWarnings("deprecation")
public final class RegistryAssembly {
    public static final RegistryAssembly INSTANCE = new RegistryAssembly();

    private RegistryAssembly() {
    }

    public static BlockArray.BlockInformation packBlock(final IBlockState block) {
        return new BlockArray.BlockInformation(ObjectLists.singleton(new IBlockStateDescriptor(block)));
    }

    public static BlockArray.BlockInformation packBlock(final IBlockState... blocks) {
        final var descriptors = new ObjectArrayList<IBlockStateDescriptor>(blocks.length);
        for (final var block : blocks) {
            descriptors.add(new IBlockStateDescriptor(block));
        }
        return new BlockArray.BlockInformation(descriptors);
    }

    private static NEWDynamicMachine newMachine(final String namespace, final String name) {
        final var machine = new NEWDynamicMachine("");
        if (machine instanceof AccessorAbstractMachine accessor) {
            accessor.setRegistryName(new ResourceLocation(namespace, name));
        }
        return machine;
    }

    private static NEWDynamicMachine setName(final NEWDynamicMachine machine, final String name) {
        machine.setLocalizedName(I18n.translateToLocal(name));
        return machine;
    }

    @Optional.Method(modid = "astralsorcery")
    private static void regAS() {
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.blockAltar, 1), setName(transform(MultiBlockArrays.patternAltarAttunement, "astralsorcery", "AltarAttunement"), "tile.blockaltar.altar_2.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.blockAltar, 2), setName(transform(MultiBlockArrays.patternAltarConstellation, "astralsorcery", "AltarConstellation"), "tile.blockaltar.altar_3.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.blockAltar, 3), setName(transform(MultiBlockArrays.patternAltarTrait, "astralsorcery", "AltarTrait"), "tile.blockaltar.altar_4.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.ritualPedestal, 0), setName(transform(MultiBlockArrays.patternRitualPedestal, "astralsorcery", "RitualPedestal"), "tile.blockritualpedestal.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.blockBore, 0), setName(transform(MultiBlockArrays.patternFountain, "astralsorcery", "Fountain"), "tile.blockfountain.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.attunementRelay, 0), setName(transform(MultiBlockArrays.patternCollectorRelay, "astralsorcery", "CollectorRelay"), "tile.blockattunementrelay.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.attunementAltar, 0), setName(transform(MultiBlockArrays.patternAttunementFrame, "astralsorcery", "AttunementFrame"), "tile.blockstructural.attunement_altar_struct.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.celestialGateway, 0), setName(transform(MultiBlockArrays.patternCelestialGateway, "astralsorcery", "CelestialGateway"), "tile.blockcelestialgateway.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.starlightInfuser, 0), setName(transform(MultiBlockArrays.patternStarlightInfuser, "astralsorcery", "StarlightInfuser"), "tile.blockstarlightinfuser.name"));
        NEWMachineAssemblyManager.setConstructors(new NEWMachineAssemblyManager.BlockPair(BlocksAS.celestialCollectorCrystal, 0), setName(transform(MultiBlockArrays.patternCollectorEnhancement, "astralsorcery", "CollectorEnhancement"), "research.ENHANCED_COLLECTOR.name"));
    }

    @Optional.Method(modid = "mekanism")
    private static void regMEK() {
        final var reactorMachine = newMachine("mek", "reactor");
        final var reactor = new NEWMachineAssemblyManager.BlockPair(GeneratorsBlocks.Reactor, 0);
        final var mekReactor = reactorMachine.getPattern();
        final var reactorGlass0 = new IBlockStateDescriptor(GeneratorsBlocks.ReactorGlass.getStateFromMeta(0));
        final var reactorGlass1 = new IBlockStateDescriptor(GeneratorsBlocks.ReactorGlass.getStateFromMeta(1));
        final var reactor1 = new IBlockStateDescriptor(GeneratorsBlocks.Reactor.getStateFromMeta(1));
        final var reactor2 = new IBlockStateDescriptor(GeneratorsBlocks.Reactor.getStateFromMeta(2));
        final var reactor3 = new IBlockStateDescriptor(GeneratorsBlocks.Reactor.getStateFromMeta(3));
        final var framework = new BlockArray.BlockInformation(ObjectLists.singleton(reactor1));
        final var functional = new BlockArray.BlockInformation(Functions.asList(reactorGlass0, reactor1, reactor2, reactor3));
        final var center = new BlockArray.BlockInformation(Functions.asList(reactorGlass1, reactor1, reactorGlass0, reactor2, reactor3));
        mekReactor.addBlock(BlockPos.ORIGIN, packBlock(reactor.getBlockState()));
        mekReactor.addBlock(new BlockPos(-2, 0, 0), framework);
        mekReactor.addBlock(new BlockPos(-1, 0, -1), framework);
        mekReactor.addBlock(new BlockPos(-1, 0, 0), functional);
        mekReactor.addBlock(new BlockPos(-1, 0, 1), framework);
        mekReactor.addBlock(new BlockPos(0, 0, -2), framework);
        mekReactor.addBlock(new BlockPos(0, 0, -1), functional);
        mekReactor.addBlock(new BlockPos(0, 0, 1), functional);
        mekReactor.addBlock(new BlockPos(0, 0, 2), framework);
        mekReactor.addBlock(new BlockPos(1, 0, -1), framework);
        mekReactor.addBlock(new BlockPos(1, 0, 0), functional);
        mekReactor.addBlock(new BlockPos(1, 0, 1), framework);
        mekReactor.addBlock(new BlockPos(2, 0, 0), framework);
        mekReactor.addBlock(new BlockPos(-2, -1, -1), framework);
        mekReactor.addBlock(new BlockPos(-2, -1, 0), functional);
        mekReactor.addBlock(new BlockPos(-2, -1, 1), framework);
        mekReactor.addBlock(new BlockPos(-1, -1, -2), framework);
        mekReactor.addBlock(new BlockPos(-1, -1, 2), framework);
        mekReactor.addBlock(new BlockPos(0, -1, -2), functional);
        mekReactor.addBlock(new BlockPos(0, -1, 2), functional);
        mekReactor.addBlock(new BlockPos(1, -1, -2), framework);
        mekReactor.addBlock(new BlockPos(1, -1, 2), framework);
        mekReactor.addBlock(new BlockPos(2, -1, -1), framework);
        mekReactor.addBlock(new BlockPos(2, -1, 0), functional);
        mekReactor.addBlock(new BlockPos(2, -1, 1), framework);
        mekReactor.addBlock(new BlockPos(-2, -2, -2), framework);
        mekReactor.addBlock(new BlockPos(-2, -2, -1), functional);
        mekReactor.addBlock(new BlockPos(-2, -2, 0), center);
        mekReactor.addBlock(new BlockPos(-2, -2, 1), functional);
        mekReactor.addBlock(new BlockPos(-2, -2, 2), framework);
        mekReactor.addBlock(new BlockPos(-1, -2, -2), functional);
        mekReactor.addBlock(new BlockPos(-1, -2, 2), functional);
        mekReactor.addBlock(new BlockPos(0, -2, -2), center);
        mekReactor.addBlock(new BlockPos(0, -2, 2), center);
        mekReactor.addBlock(new BlockPos(1, -2, -2), functional);
        mekReactor.addBlock(new BlockPos(1, -2, 2), functional);
        mekReactor.addBlock(new BlockPos(2, -2, -2), framework);
        mekReactor.addBlock(new BlockPos(2, -2, -1), functional);
        mekReactor.addBlock(new BlockPos(2, -2, 0), center);
        mekReactor.addBlock(new BlockPos(2, -2, 1), functional);
        mekReactor.addBlock(new BlockPos(2, -2, 2), framework);
        mekReactor.addBlock(new BlockPos(-2, -3, -1), framework);
        mekReactor.addBlock(new BlockPos(-2, -3, 0), functional);
        mekReactor.addBlock(new BlockPos(-2, -3, 1), framework);
        mekReactor.addBlock(new BlockPos(-1, -3, -2), framework);
        mekReactor.addBlock(new BlockPos(-1, -3, 2), framework);
        mekReactor.addBlock(new BlockPos(0, -3, -2), functional);
        mekReactor.addBlock(new BlockPos(0, -3, 2), functional);
        mekReactor.addBlock(new BlockPos(1, -3, -2), framework);
        mekReactor.addBlock(new BlockPos(1, -3, 2), framework);
        mekReactor.addBlock(new BlockPos(2, -3, -1), framework);
        mekReactor.addBlock(new BlockPos(2, -3, 0), functional);
        mekReactor.addBlock(new BlockPos(2, -3, 1), framework);
        mekReactor.addBlock(new BlockPos(-2, -4, 0), framework);
        mekReactor.addBlock(new BlockPos(-1, -4, -1), framework);
        mekReactor.addBlock(new BlockPos(-1, -4, 0), functional);
        mekReactor.addBlock(new BlockPos(-1, -4, 1), framework);
        mekReactor.addBlock(new BlockPos(0, -4, -2), framework);
        mekReactor.addBlock(new BlockPos(0, -4, -1), functional);
        mekReactor.addBlock(new BlockPos(0, -4, 0), functional);
        mekReactor.addBlock(new BlockPos(0, -4, 1), functional);
        mekReactor.addBlock(new BlockPos(0, -4, 2), framework);
        mekReactor.addBlock(new BlockPos(1, -4, -1), framework);
        mekReactor.addBlock(new BlockPos(1, -4, 0), functional);
        mekReactor.addBlock(new BlockPos(1, -4, 1), framework);
        mekReactor.addBlock(new BlockPos(2, -4, 0), framework);
        NEWMachineAssemblyManager.setConstructors(reactor, setName(reactorMachine, "tile.Reactor.name"));
        final var thermalEvaporationMachine = newMachine("mek", camelToSnake("thermalEvaporation"));
        final var thermalEvaporationArray = thermalEvaporationMachine.getPattern();
        final var thermalEvaporation = new NEWMachineAssemblyManager.BlockPair(MekanismBlocks.BasicBlock, 14);
        final var thermal = packBlock(MekanismBlocks.BasicBlock2.getDefaultState());
        final var thermalP = packBlock(MekanismBlocks.BasicBlock2.getDefaultState(), MekanismBlocks.BasicBlock.getStateFromMeta(15));
        thermalEvaporationArray.addBlock(-1, -1, 3, thermal);
        thermalEvaporationArray.addBlock(0, -1, 3, thermal);
        thermalEvaporationArray.addBlock(1, -1, 3, thermal);
        thermalEvaporationArray.addBlock(-2, -1, 3, thermal);
        thermalEvaporationArray.addBlock(-1, -1, 2, thermal);
        thermalEvaporationArray.addBlock(0, -1, 2, thermal);
        thermalEvaporationArray.addBlock(1, -1, 2, thermal);
        thermalEvaporationArray.addBlock(-2, -1, 2, thermal);
        thermalEvaporationArray.addBlock(-1, -1, 1, thermal);
        thermalEvaporationArray.addBlock(0, -1, 1, thermal);
        thermalEvaporationArray.addBlock(1, -1, 1, thermal);
        thermalEvaporationArray.addBlock(-2, -1, 1, thermal);
        thermalEvaporationArray.addBlock(-1, -1, 0, thermal);
        thermalEvaporationArray.addBlock(0, -1, 0, thermal);
        thermalEvaporationArray.addBlock(1, -1, 0, thermal);
        thermalEvaporationArray.addBlock(-2, -1, 0, thermal);
        thermalEvaporationArray.addBlock(-1, 0, 0, thermalP);
        thermalEvaporationArray.addBlock(BlockPos.ORIGIN, packBlock(thermalEvaporation.getBlockState()));
        thermalEvaporationArray.addBlock(1, 0, 0, thermalP);
        thermalEvaporationArray.addBlock(-2, 0, 0, thermalP);
        thermalEvaporationArray.addBlock(-1, 0, 3, thermalP);
        thermalEvaporationArray.addBlock(0, 0, 3, thermalP);
        thermalEvaporationArray.addBlock(1, 0, 3, thermalP);
        thermalEvaporationArray.addBlock(-2, 0, 3, thermalP);
        thermalEvaporationArray.addBlock(1, 0, 2, thermalP);
        thermalEvaporationArray.addBlock(1, 0, 1, thermalP);
        thermalEvaporationArray.addBlock(-2, 0, 2, thermalP);
        thermalEvaporationArray.addBlock(-2, 0, 1, thermalP);
        final var dy = new DynamicPattern("thermal", new TaggedPositionBlockArray(), new TaggedPositionBlockArray(), 1, 16);
        dy.getFaces().add(EnumFacing.UP);
        dy.setStructureSizeOffset(dy.getStructureSizeOffset().add(0, 1, 0));
        final var patter = dy.getPattern();
        final var patterEnd = dy.getPatternEnd();
        patter.addBlock(-1, 1, 0, thermalP);
        patter.addBlock(0, 1, 0, thermalP);
        patter.addBlock(1, 1, 0, thermalP);
        patter.addBlock(-2, 1, 0, thermalP);
        patter.addBlock(-1, 1, 3, thermalP);
        patter.addBlock(0, 1, 3, thermalP);
        patter.addBlock(1, 1, 3, thermalP);
        patter.addBlock(-2, 1, 3, thermalP);
        patter.addBlock(1, 1, 2, thermalP);
        patter.addBlock(1, 1, 1, thermalP);
        patter.addBlock(-2, 1, 2, thermalP);
        patter.addBlock(-2, 1, 1, thermalP);
        patterEnd.addBlock(-1, 0, 0, thermalP);
        patterEnd.addBlock(BlockPos.ORIGIN, thermalP);
        patterEnd.addBlock(1, 0, 0, thermalP);
        patterEnd.addBlock(-2, 0, 0, thermalP);
        patterEnd.addBlock(-1, 0, 3, thermalP);
        patterEnd.addBlock(0, 0, 3, thermalP);
        patterEnd.addBlock(1, 0, 3, thermalP);
        patterEnd.addBlock(-2, 0, 3, thermalP);
        patterEnd.addBlock(1, 0, 2, thermalP);
        patterEnd.addBlock(1, 0, 1, thermalP);
        patterEnd.addBlock(-2, 0, 2, thermalP);
        patterEnd.addBlock(-2, 0, 1, thermalP);
        thermalEvaporationMachine.addDynamicPattern("thermal", dy);
        NEWMachineAssemblyManager.setConstructors(thermalEvaporation, setName(thermalEvaporationMachine, "tile.ThermalEvaporation.name"));
    }

    @Optional.Method(modid = "botania")
    private static void regBot() {
        final var terraPlateMachine = newMachine("botania", "terra_plate");
        final var terraplateArray = terraPlateMachine.getPattern();
        final var stone = packBlock(ModBlocks.livingrock.getDefaultState());
        final var lapis = packBlock(Blocks.LAPIS_BLOCK.getDefaultState());
        final var terraPlate = new NEWMachineAssemblyManager.BlockPair(ModBlocks.terraPlate, 0);
        terraplateArray.addBlock(BlockPos.ORIGIN, packBlock(terraPlate.getBlockState()));
        terraplateArray.addBlock(0, -1, 0, stone);
        terraplateArray.addBlock(-1, -1, -1, stone);
        terraplateArray.addBlock(-1, -1, 1, stone);
        terraplateArray.addBlock(1, -1, -1, stone);
        terraplateArray.addBlock(1, -1, 1, stone);
        terraplateArray.addBlock(0, -1, 1, lapis);
        terraplateArray.addBlock(0, -1, -1, lapis);
        terraplateArray.addBlock(1, -1, 0, lapis);
        terraplateArray.addBlock(-1, -1, 0, lapis);
        NEWMachineAssemblyManager.setConstructors(terraPlate, setName(terraPlateMachine, "tile.botania:terraPlate.name"));
        final var alfheimportalMachine = newMachine("botania", "alfheimportal");
        final var alfheimportalArray = alfheimportalMachine.getPattern();
        final var wood = packBlock(ModBlocks.livingwood.getDefaultState());
        final var lightwood = packBlock(ModBlocks.livingwood.getStateFromMeta(5));
        final var alfheimportal = new NEWMachineAssemblyManager.BlockPair(ModBlocks.alfPortal, 0);
        alfheimportalArray.addBlock(BlockPos.ORIGIN, packBlock(alfheimportal.getBlockState()));
        alfheimportalArray.addBlock(1, 0, 0, wood);
        alfheimportalArray.addBlock(-1, 0, 0, wood);
        alfheimportalArray.addBlock(2, 1, 0, wood);
        alfheimportalArray.addBlock(-2, 1, 0, wood);
        alfheimportalArray.addBlock(2, 2, 0, lightwood);
        alfheimportalArray.addBlock(-2, 2, 0, lightwood);
        alfheimportalArray.addBlock(2, 3, 0, wood);
        alfheimportalArray.addBlock(-2, 3, 0, wood);
        alfheimportalArray.addBlock(0, 4, 0, lightwood);
        alfheimportalArray.addBlock(1, 4, 0, wood);
        alfheimportalArray.addBlock(-1, 4, 0, wood);
        NEWMachineAssemblyManager.setConstructors(alfheimportal, setName(alfheimportalMachine, "tile.Alfheimportal.name"));
    }

    @Optional.Method(modid = "packagedastral")
    private static void regPackagedastral() {
        {
            final var block = new NEWMachineAssemblyManager.BlockPair(BlockAttunementCrafter.INSTANCE, 0);
            final var machine = setName(transform(MultiBlockArrays.patternAltarAttunement, "packagedastral", "AltarAttunement"), "tile.packagedastral.attunement_crafter.name");
            machine.getPattern().addBlock(BlockPos.ORIGIN, packBlock(block.getBlockState()));
            NEWMachineAssemblyManager.setConstructors(block, machine);
        }
        {
            final var block = new NEWMachineAssemblyManager.BlockPair(BlockConstellationCrafter.INSTANCE, 0);
            final var machine = setName(transform(MultiBlockArrays.patternAltarConstellation, "packagedastral", "AltarConstellation"), "tile.packagedastral.constellation_crafter.name");
            machine.getPattern().addBlock(BlockPos.ORIGIN, packBlock(block.getBlockState()));
            NEWMachineAssemblyManager.setConstructors(block, machine);
        }
        final var block = new NEWMachineAssemblyManager.BlockPair(BlockTraitCrafter.INSTANCE, 0);
        final var machine = setName(transform(MultiBlockArrays.patternAltarTrait, "packagedastral", "AltarTrait"), "tile.packagedastral.trait_crafter.name");
        machine.getPattern().addBlock(BlockPos.ORIGIN, packBlock(block.getBlockState()));
        NEWMachineAssemblyManager.setConstructors(block, machine);
    }

    @Optional.Method(modid = "astralsorcery")
    private static NEWDynamicMachine transform(final PatternBlockArray array, final String namespace, final String name) {
        final var machine = newMachine(namespace, camelToSnake(name));
        for (final var entry : array.getPattern().entrySet()) {
            machine.getPattern().addBlock(entry.getKey(), packBlock(entry.getValue().state));
        }
        return machine;
    }

    private static String camelToSnake(final String value) {
        final char[] buffer = new char[value.length() * 2];
        int index = 0;
        boolean previousLower = false;
        for (int i = 0; i < value.length(); i++) {
            final char character = value.charAt(i);
            if (character >= 'A' && character <= 'Z') {
                if (previousLower) {
                    buffer[index++] = '_';
                }
                buffer[index++] = (char) (character + 32);
                previousLower = false;
            } else {
                buffer[index++] = character;
                previousLower = character >= 'a' && character <= 'z';
            }
        }
        return new String(buffer, 0, index);
    }

    public void regAll() {
        if (MixinDecisions.astralSorceryLoaded) {
            regAS();
        }
        if (Loader.isModLoaded("mekanism")) {
            regMEK();
        }
        if (MixinDecisions.botaniaLoaded) {
            regBot();
        }
        if (MixinDecisions.packagedAutoLoaded && MixinDecisions.packagedAstralLoaded) {
            regPackagedastral();
        }
    }
}
