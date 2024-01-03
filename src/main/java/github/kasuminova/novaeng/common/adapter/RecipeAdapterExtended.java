package github.kasuminova.novaeng.common.adapter;

import github.kasuminova.novaeng.common.adapter.mc.AdapterMCFurnaceWithExp;
import github.kasuminova.novaeng.common.adapter.nco.AdapterNCOElectrolyzer;
import github.kasuminova.novaeng.common.adapter.nco.AdapterNCOPressurizer;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;

public class RecipeAdapterExtended {

    public static void registerAdapter() {
        RegistriesMM.ADAPTER_REGISTRY.register(new AdapterNCOPressurizer());
        RegistriesMM.ADAPTER_REGISTRY.register(new AdapterNCOElectrolyzer());
        RegistriesMM.ADAPTER_REGISTRY.register(new AdapterMCFurnaceWithExp());
        RegistriesMM.ADAPTER_REGISTRY.register(new AdapterShredder());
    }

}
