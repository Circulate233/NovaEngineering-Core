package github.kasuminova.novaeng.mixin.minecraft;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.util.ObjectIntIdentityMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.IdentityHashMap;
import java.util.List;

@Mixin(ObjectIntIdentityMap.class)
public class MixinObjectIntIdentityMap<T> {
    @Shadow
    @Final
    protected List<T> objectList;
    @Mutable
    @Shadow
    @Final
    protected IdentityHashMap<T, Integer> identityMap;
    @Unique
    protected Reference2IntMap<T> nova$identityMap;

    @Unique
    protected boolean nova$isMCMap() {
        return ((Object) this).getClass() == ObjectIntIdentityMap.class;
    }

    @Redirect(method = "<init>(I)V", at = @At(value = "NEW", target = "(I)Ljava/util/IdentityHashMap;"))
    private IdentityHashMap<?, ?> clearOldMap(int expectedMaxSize) {
        if (nova$isMCMap()) {
            return new IdentityHashMap<>(0);
        } else {
            return new IdentityHashMap<>(expectedMaxSize);
        }
    }

    @Redirect(method = "<init>(I)V", at = @At(value = "FIELD", target = "Lnet/minecraft/util/ObjectIntIdentityMap;identityMap:Ljava/util/IdentityHashMap;", opcode = Opcodes.PUTFIELD))
    private void clearOldMap(ObjectIntIdentityMap<?> instance, IdentityHashMap<T, Integer> value) {
        if (nova$isMCMap()) {
            nova$identityMap = new Reference2IntOpenHashMap<>();
            nova$identityMap.defaultReturnValue(-1);
        } else {
            identityMap = value;
        }
    }

    /**
     * @author circulation
     * @reason 避开旧Map调用
     */
    @Overwrite
    public void put(T key, int value) {
        if (nova$isMCMap()) {
            nova$identityMap.put(key, value);
        } else {
            identityMap.put(key, value);
        }

        while (this.objectList.size() <= value) {
            this.objectList.add(null);
        }

        this.objectList.set(value, key);
    }

    /**
     * @author circulation
     * @reason 避开旧Map调用
     */
    @Overwrite
    public int get(T key) {
        if (nova$isMCMap()) {
            return nova$identityMap.getInt(key);
        } else {
            return identityMap.get(key);
        }
    }

    /**
     * @author circulation
     * @reason 避开旧Map调用
     */
    @Overwrite
    public int size() {
        if (nova$isMCMap()) {
            return this.nova$identityMap.size();
        } else {
            return this.identityMap.size();
        }
    }
}
