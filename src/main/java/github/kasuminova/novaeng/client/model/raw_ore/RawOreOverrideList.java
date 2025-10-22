package github.kasuminova.novaeng.client.model.raw_ore;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.common.model.TRSRTransformation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import java.util.Map;

/**
 * <a href="https://github.com/SmileycorpMC/dynaores/blob/main/src/main/java/net/smileycorp/dynaores/client/OreModelOverrides.java">...</a>
 * class incorporates code from this location and retains the original license terms.
 */
public class RawOreOverrideList extends ItemOverrideList {

    private static final TRSRTransformation flipX = new TRSRTransformation(null, null, new Vector3f(-1, 1, 1), null);
    private final Map<Item, IBakedModel> cache = new Reference2ObjectOpenHashMap<>();

    public RawOreOverrideList() {
        super(ObjectLists.emptyList());
    }

    private static ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> itemTransforms() {
        TRSRTransformation thirdperson = get(0, 3, 1, 0, 0, 0, 0.55f);
        TRSRTransformation firstperson = get(1.13f, 3.2f, 1.13f, 0, -90, 25, 0.68f);
        ImmutableMap.Builder<ItemCameraTransforms.TransformType, TRSRTransformation> builder = ImmutableMap.builder();
        builder.put(ItemCameraTransforms.TransformType.GROUND, get(0, 2, 0, 0, 0, 0, 0.5f));
        builder.put(ItemCameraTransforms.TransformType.HEAD, get(0, 13, 7, 0, 180, 0, 1));
        builder.put(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, thirdperson);
        builder.put(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, leftify(thirdperson));
        builder.put(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, firstperson);
        builder.put(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, leftify(firstperson));
        return builder.build();
    }

    private static TRSRTransformation leftify(TRSRTransformation transform) {
        return TRSRTransformation.blockCenterToCorner(
                flipX.compose(TRSRTransformation.blockCornerToCenter(transform)).compose(flipX));
    }

    private static TRSRTransformation get(float tx, float ty, float tz, float ax, float ay, float az, float s) {
        return TRSRTransformation.blockCenterToCorner(new TRSRTransformation(
                new Vector3f(tx / 16, ty / 16, tz / 16),
                TRSRTransformation.quatFromXYZDegrees(new Vector3f(ax, ay, az)), new Vector3f(s, s, s), null));
    }

    @NotNull
    @Override
    public IBakedModel handleItemState(@NotNull IBakedModel base, @NotNull ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
        var out = cache.get(stack.getItem());
        if (out != null) {
            return out;
        }
        out = new BakedItemModel(
                (ImmutableList<BakedQuad>) base.getQuads(null, null, 0),
                base.getParticleTexture(),
                itemTransforms(),
                ItemOverrideList.NONE,
                true
        );
        synchronized (cache) {
            cache.put(stack.getItem(), out);
        }
        return out;
    }

}