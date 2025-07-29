package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketRecipeBook;
import net.minecraft.stats.RecipeBookServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(value = RecipeBookServer.class)
public class MixinRecipeBookServer {

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Overwrite
    public void add(List<IRecipe> recipesIn, EntityPlayerMP player) {}

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Overwrite
    public void remove(List<IRecipe> recipesIn, EntityPlayerMP player) {}

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Overwrite
    private void sendPacket(SPacketRecipeBook.State state, EntityPlayerMP player, List<IRecipe> recipesIn) {}

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Overwrite
    public NBTTagCompound write() {
        return new NBTTagCompound();
    }

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Overwrite
    public void read(NBTTagCompound tag) {}

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Inject(method = "getRecipes",at = @At("HEAD"), cancellable = true)
    public void getRecipes(CallbackInfoReturnable<List<IRecipe>> cir) {
        cir.setReturnValue(Collections.emptyList());
    }

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Overwrite
    private List<IRecipe> getDisplayedRecipes() {
        return Collections.emptyList();
    }

    /**
     * @author circulation
     * @reason 废弃原版配方书
     */
    @Overwrite
    public void init(EntityPlayerMP player) {}

}
