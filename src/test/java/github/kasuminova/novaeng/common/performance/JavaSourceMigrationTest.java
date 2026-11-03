package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.util.IBlockPosEx;
import github.kasuminova.novaeng.common.util.OnlyWriteEnergyContainer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceMigrationTest {

    private static List<String> calls(final MethodNode method) {
        return Stream.of(method.instructions.toArray())
                     .filter(instruction -> instruction instanceof MethodInsnNode)
                     .map(instruction -> (MethodInsnNode) instruction)
                     .map(call -> call.owner + '.' + call.name + call.desc)
                     .toList();
    }

    @Test
    void receiveOnlyEnergyWrapperPreservesSimulationAndStorageReplacement() {
        final EnergyStorage first = new EnergyStorage();
        final OnlyWriteEnergyContainer wrapper = new OnlyWriteEnergyContainer(first);
        assertEquals(5, wrapper.receiveEnergy(5, true));
        assertEquals(0, wrapper.getEnergyStored());
        assertEquals(5, wrapper.receiveEnergy(5, false));
        assertEquals(5, wrapper.getEnergyStored());
        assertEquals(100, wrapper.getMaxEnergyStored());
        assertEquals(0, wrapper.extractEnergy(5, false));
        assertEquals(0, first.extractions);
        assertFalse(wrapper.canExtract());
        assertTrue(wrapper.canReceive());
        final EnergyStorage second = new EnergyStorage();
        assertSame(wrapper, wrapper.setStorage(second));
        wrapper.receiveEnergy(8, false);
        assertEquals(8, second.stored);
        assertEquals(5, first.stored);
    }

    @Test
    void relativePositionTransformsPreserveAllSixFacingsAndIntegerOverflow() {
        for (final int x : new int[]{2, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            final BlockPos origin = new BlockPos(11, 22, 33);
            assertEquals(origin.add(x, 3, 5), IBlockPosEx.createPosByFacing(origin, EnumFacing.NORTH, x, 3, 5));
            assertEquals(origin.add(-x, 3, -5), IBlockPosEx.createPosByFacing(origin, EnumFacing.SOUTH, x, 3, 5));
            assertEquals(origin.add(-5, 3, x), IBlockPosEx.createPosByFacing(origin, EnumFacing.EAST, x, 3, 5));
            assertEquals(origin.add(5, 3, -x), IBlockPosEx.createPosByFacing(origin, EnumFacing.WEST, x, 3, 5));
            assertEquals(origin.add(x, -5, 3), IBlockPosEx.createPosByFacing(origin, EnumFacing.UP, x, 3, 5));
            assertEquals(origin.add(x, 5, 3), IBlockPosEx.createPosByFacing(origin, EnumFacing.DOWN, x, 3, 5));
        }
    }

    private static final class EnergyStorage implements IEnergyStorage {
        private int stored;
        private int extractions;

        @Override
        public int receiveEnergy(final int amount, final boolean simulate) {
            if (!simulate) {
                this.stored += amount;
            }
            return amount;
        }

        @Override
        public int extractEnergy(final int amount, final boolean simulate) {
            this.extractions++;
            return amount;
        }

        @Override
        public int getEnergyStored() {
            return this.stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return 100;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
