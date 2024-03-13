package github.kasuminova.novaeng.common.integration.theoneprobe;

import mcjty.theoneprobe.TheOneProbe;

public class IntegrationTOP {
    public static void registerProvider() {
        TheOneProbe.theOneProbeImp.registerProvider(HyperNetInfoProvider.INSTANCE);
        TheOneProbe.theOneProbeImp.registerProvider(SpecialMachineInfoProvider.INSTANCE);
    }
}
