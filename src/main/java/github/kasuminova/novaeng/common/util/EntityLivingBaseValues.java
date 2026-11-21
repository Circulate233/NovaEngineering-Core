package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class EntityLivingBaseValues {

    public static final DataParameter<Float> HEALTH;

    static {
        DataParameter<Float> health = null;
        try {
            health = ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, null, "field_184632_c");
        } catch (Exception e) {
            NovaEngineeringCore.log.warn("Cannot get field EntityLivingBase.HEALTH!", e);
        }
        HEALTH = health;
    }

}
