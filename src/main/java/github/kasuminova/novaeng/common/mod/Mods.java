package github.kasuminova.novaeng.common.mod;

import net.minecraftforge.fml.common.Loader;

public enum Mods {

    AE2("appliedenergistics2"),
    AE2EL("appliedenergistics2") {
        @Override
        public boolean loaded() {
            if (this.initialized) {
                return this.loaded;
            }
            this.initialized = true;
            if (!super.loaded()) {
                return this.loaded = false;
            }
            try {
                Class.forName("appeng.core.AE2ELCore");
                return this.loaded = true;
            } catch (Exception e) {
                return this.loaded = false;
            }
        }
    },
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
