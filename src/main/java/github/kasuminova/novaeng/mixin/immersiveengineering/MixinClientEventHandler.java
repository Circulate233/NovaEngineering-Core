package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.client.ClientEventHandler;
import blusunrize.immersiveengineering.client.fx.ParticleFractal;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDevice1;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntitySampleDrill;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientEventHandler.class, remap = false)
public class MixinClientEventHandler {

    @Shadow
    private static ItemStack sampleDrill;

    @Shadow
    private static ItemStack coreSample;

    @Inject(method = "onRenderWorldLastEvent", at = @At("HEAD"), cancellable = true)
    private void nova$skipWhenIdle(final RenderWorldLastEvent event, final CallbackInfo ci) {
        final EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        nova$ensureSampleStacks();
        if (nova$matchesSampleItems(player.getHeldItemMainhand())
            || nova$matchesSampleItems(player.getHeldItemOffhand())) {
            return;
        }
        if (!ParticleFractal.PARTICLE_FRACTAL_DEQUE.isEmpty()) {
            return;
        }
        if (!ClientEventHandler.FAILED_CONNECTIONS.isEmpty()) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK
            && mc.world.getTileEntity(mc.objectMouseOver.getBlockPos()) instanceof TileEntitySampleDrill) {
            return;
        }
        ci.cancel();
    }

    @Unique
    private static void nova$ensureSampleStacks() {
        if (sampleDrill.isEmpty()) {
            sampleDrill = new ItemStack(IEContent.blockMetalDevice1, 1, BlockTypes_MetalDevice1.SAMPLE_DRILL.getMeta());
        }
        if (coreSample.isEmpty()) {
            coreSample = new ItemStack(IEContent.itemCoresample);
        }
    }

    @Unique
    private static boolean nova$matchesSampleItems(final ItemStack held) {
        return OreDictionary.itemMatches(sampleDrill, held, true) || OreDictionary.itemMatches(coreSample, held, true);
    }
}
