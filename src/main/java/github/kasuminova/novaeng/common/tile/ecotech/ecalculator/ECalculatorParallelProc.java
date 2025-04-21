package github.kasuminova.novaeng.common.tile.ecotech.ecalculator;

import github.kasuminova.novaeng.common.block.ecotech.ecalculator.BlockECalculatorParallelProc;

public class ECalculatorParallelProc extends ECalculatorPart {

    public int parallelism = 0;

    public ECalculatorParallelProc() {
    }

    public ECalculatorParallelProc(final BlockECalculatorParallelProc parallelism) {
        this.parallelism = parallelism.getParallelism();
    }

    public int getParallelism() {
        return parallelism;
    }

    @Override
    public void onDisassembled() {
        super.onDisassembled();
        markForUpdateSync();
    }

    @Override
    public void onAssembled() {
        super.onAssembled();
        markForUpdateSync();
    }

}
