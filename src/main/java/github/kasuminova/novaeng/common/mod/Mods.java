package github.kasuminova.novaeng.common.mod;

import net.minecraftforge.fml.common.Loader;

public enum Mods {

    AE2("ae2"),
    IC2("ic2"),
    MEK("mekanism"),
    MEKCEU("mekanism") {
        @Override
        public boolean loaded() {
            if (!MEK.loaded()) {
                return false;
            }
            if (this.initialized) {
                return this.loaded;
            }

            try {
                Class.forName("mekanism.common.config.MEKCEConfig");
                this.initialized = true;
                return this.loaded = true;
            } catch (Throwable e) {
                return this.loaded = false;
            }
        }
    };

    protected final String modID;
    protected boolean loaded = false;
    protected boolean initialized = false;

    Mods(final String modID) {
        this.modID = modID;
    }

    public boolean loaded() {
        if (!this.initialized) {
            this.loaded = Loader.isModLoaded(this.modID);
            this.initialized = true;
        }
        return this.loaded;
    }

}
