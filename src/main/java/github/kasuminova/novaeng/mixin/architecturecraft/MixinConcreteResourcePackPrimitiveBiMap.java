package github.kasuminova.novaeng.mixin.architecturecraft;

import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.common.ConcreteLog;
import com.elytradev.architecturecraft.repackage.com.elytradev.concrete.resgen.ConcreteResourcePack;
import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.client.resource.ModelMetadataAccess;
import github.kasuminova.novaeng.common.util.collection.ObjectIntPairHashBiMap;

import it.unimi.dsi.fastutil.objects.ObjectIntPair;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.registries.IRegistryDelegate;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;

@Mixin(value = ConcreteResourcePack.class, remap = false, priority = 1000)
public abstract class MixinConcreteResourcePackPrimitiveBiMap implements ModelMetadataAccess {
    @Unique
    private static volatile VarHandle nova$customModels;
    @Shadow
    public String modID;

    /**
     * @author NovaEngineering
     * @reason Keep model metadata primitive; the legacy Pair exists only at ModelLoader's input boundary.
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    public static BiMap<ObjectIntPair<IRegistryDelegate<Item>>, ModelResourceLocation>
    getCustomModels() {
        try {
            VarHandle handle = nova$customModels;
            if (handle == null) {
                handle =
                    MethodHandles.privateLookupIn(ModelLoader.class, MethodHandles.lookup())
                                 .findStaticVarHandle(ModelLoader.class, "customModels", Map.class);
                nova$customModels = handle;
            }
            final Map<Pair<IRegistryDelegate<Item>, Integer>, ModelResourceLocation> source =
                (Map<Pair<IRegistryDelegate<Item>, Integer>, ModelResourceLocation>)
                    handle.get();
            final ObjectIntPairHashBiMap<IRegistryDelegate<Item>, ModelResourceLocation> result =
                new ObjectIntPairHashBiMap<>(source.size());
            for (final var entry : source.entrySet()) {
                final var key = entry.getKey();
                result.forcePut(key.getLeft(), key.getRight(), entry.getValue());
            }
            return result;
        } catch (final Exception failure) {
            ConcreteLog.error(
                "Caught exception getting customModels from the model loader, ", failure);
            return new ObjectIntPairHashBiMap<>();
        }
    }

    @Shadow
    public static ResourceLocation nameToLocation(final String name) {
        throw new AssertionError();
    }

    @Shadow
    private boolean isLocation(final String place, final String validation) {
        throw new AssertionError();
    }

    @Unique
    private ModelResourceLocation nova$modelLocation(
        final String name, final ResourceLocation location) {
        String path = location.getPath();
        path = path.substring(path.lastIndexOf('/') + 1);
        path = path.substring(0, path.lastIndexOf('.'));
        return new ModelResourceLocation(
            location.getNamespace() + ":" + path,
            isLocation(name, "/models/item/") ? "inventory" : "normal");
    }

    /**
     * @author NovaEngineering
     * @reason Consume the fastutil model key directly.
     */
    @Overwrite
    public Item getItem(final String name) {
        final String itemID = name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        final Item item = Item.getByNameOrId(modID + ":" + itemID);
        if (item != null) return item;
        final ResourceLocation location = nameToLocation(name);
        try {
            final var models = getCustomModels();
            final ObjectIntPair<IRegistryDelegate<Item>> key =
                models.inverse().get(nova$modelLocation(name, location));
            if (key != null) return key.left().get();
        } catch (final Exception failure) {
            ConcreteLog.error("Failed to get item from ResourceLocation", failure);
        }
        return Items.AIR;
    }

    /**
     * @author NovaEngineering
     * @reason Read primitive model metadata at the legacy Integer-returning boundary.
     */
    @Overwrite
    public Integer getMetaFromName(final String name) {
        return nova$getModelMetadata(name);
    }

    @Override
    public int nova$getModelMetadata(final String name) {
        final ResourceLocation location = nameToLocation(name);
        try {
            final var models = getCustomModels();
            final ObjectIntPair<IRegistryDelegate<Item>> key =
                models.inverse().get(nova$modelLocation(name, location));
            if (key != null) return key.rightInt();
        } catch (final Exception failure) {
            ConcreteLog.error("Failed to get metadata from name", failure);
        }
        return 0;
    }
}
