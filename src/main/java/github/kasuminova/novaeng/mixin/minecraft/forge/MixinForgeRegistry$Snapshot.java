package github.kasuminova.novaeng.mixin.minecraft.forge;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

@Mixin(value = ForgeRegistry.Snapshot.class, remap = false)
public class MixinForgeRegistry$Snapshot {

    @Mutable
    @Shadow
    @Final
    public Set<Integer> blocked;

    @Mutable
    @Shadow
    @Final
    public Map<ResourceLocation, Integer> ids;

    @Shadow
    @Final
    public Map<ResourceLocation, ResourceLocation> aliases;

    @Shadow
    @Final
    public Map<ResourceLocation, String> overrides;

    @Shadow
    @Final
    public Set<ResourceLocation> dummied;

    /**
     * @author circulation
     * @reason 替换原始类型
     */
    @Overwrite
    public static ForgeRegistry.Snapshot read(NBTTagCompound nbt) {
        ForgeRegistry.Snapshot ret = new ForgeRegistry.Snapshot();
        if (nbt != null) {
            NBTTagList list = nbt.getTagList("ids", 10);
            list.forEach((e) -> {
                NBTTagCompound comp = (NBTTagCompound) e;
                ((Object2IntMap<ResourceLocation>) ret.ids).put(new ResourceLocation(comp.getString("K")), comp.getInteger("V"));
            });
            list = nbt.getTagList("aliases", 10);
            list.forEach((e) -> {
                NBTTagCompound comp = (NBTTagCompound) e;
                String v = comp.getString("V");
                if (v.indexOf(58) == -1) {
                    ret.overrides.put(new ResourceLocation(comp.getString("K")), v);
                } else {
                    ResourceLocation aliask = new ResourceLocation(comp.getString("K"));
                    ResourceLocation aliasv = new ResourceLocation(v);
                    if (aliasv.equals(aliask)) {
                        FMLLog.log.warn("Found unrecoverable 4894 bugged alias/override: {} -> {}, skipping.", aliask, aliasv);
                    } else {
                        ret.aliases.put(aliask, aliasv);
                    }
                }

            });
            list = nbt.getTagList("overrides", 10);
            list.forEach((e) -> {
                NBTTagCompound comp = (NBTTagCompound) e;
                ret.overrides.put(new ResourceLocation(comp.getString("K")), comp.getString("V"));
            });
            int[] blocked = nbt.getIntArray("blocked");

            for (int i : blocked) {
                ((IntSet) ret.blocked).add(i);
            }

            list = nbt.getTagList("dummied", 10);
            list.forEach((e) -> ret.dummied.add(new ResourceLocation(((NBTTagCompound) e).getString("K"))));
            list = nbt.getTagList("dummied", 8);
            list.forEach((e) -> ret.dummied.add(new ResourceLocation(((NBTTagString) e).getString())));
        }
        return ret;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        blocked = new IntOpenHashSet();
        ids = new Object2IntOpenHashMap<>();
    }

    /**
     * @author circulation
     * @reason 清除其中的stream调用
     */
    @Overwrite
    public NBTTagCompound write() {
        NBTTagCompound data = new NBTTagCompound();
        int size;

        NBTTagList ids = new NBTTagList();
        var idslist = new ObjectArrayList<>(((Object2IntMap<ResourceLocation>) this.ids).object2IntEntrySet());
        idslist.sort(Map.Entry.comparingByKey());
        size = idslist.size();
        for (var i = 0; i < size; i++) {
            var e = idslist.get(i);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K", e.getKey().toString());
            tag.setInteger("V", e.getIntValue());
            ids.appendTag(tag);
        }
        data.setTag("ids", ids);

        NBTTagList aliases = new NBTTagList();
        var aliaseslist = new ObjectArrayList<>(this.aliases.entrySet());
        aliaseslist.sort(Map.Entry.comparingByKey());
        size = aliaseslist.size();
        for (var i = 0; i < size; i++) {
            var e = aliaseslist.get(i);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K", e.getKey().toString());
            tag.setString("V", e.getValue().toString());
            aliases.appendTag(tag);
        }
        data.setTag("aliases", aliases);

        NBTTagList overrides = new NBTTagList();
        var overrideslist = new ObjectArrayList<>(this.overrides.entrySet());
        overrideslist.sort(Map.Entry.comparingByKey());
        size = overrideslist.size();
        for (var i = 0; i < size; i++) {
            var e = overrideslist.get(i);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K", e.getKey().toString());
            tag.setString("V", e.getValue());
            overrides.appendTag(tag);
        }
        data.setTag("overrides", overrides);

        int[] blocked = ((IntSet) this.blocked).toIntArray();
        Arrays.sort(blocked);
        data.setIntArray("blocked", blocked);

        NBTTagList dummied = new NBTTagList();
        var dummiedlist = new ObjectArrayList<>(this.dummied);
        dummiedlist.sort(Comparator.naturalOrder());
        size = dummiedlist.size();
        for (var i = 0; i < size; i++) {
            var e = dummiedlist.get(i);
            dummied.appendTag(new NBTTagString(e.toString()));
        }
        data.setTag("dummied", dummied);
        return data;
    }

}
