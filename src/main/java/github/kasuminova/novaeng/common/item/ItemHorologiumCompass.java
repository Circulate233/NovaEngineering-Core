package github.kasuminova.novaeng.common.item;

import hellfirepvp.astralsorcery.common.constellation.distribution.ConstellationSkyHandler;
import hellfirepvp.astralsorcery.common.data.config.Config;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;

public class ItemHorologiumCompass extends ItemBasic {

    public static ItemHorologiumCompass INSTANCE = new ItemHorologiumCompass();

    private ItemHorologiumCompass(){
        super("horologium_compass");
    }

    private static final long dayTime = Config.dayLength;
    private static final int cycle = 36;

    @Override
    @NotNull
    public ActionResult<ItemStack> onItemRightClick(@NotNull World world, @NotNull EntityPlayer player, @NotNull EnumHand hand) {
        if (!world.isRemote) return super.onItemRightClick(world, player, hand);

        Optional<Long> testSeed = ConstellationSkyHandler.getInstance().getSeedIfPresent(world);
        if (testSeed.isPresent()) {
            int actualDay = getActualDay(world, testSeed.get());
            player.sendMessage(new TextComponentTranslation("tile.horologium_compass.success", actualDay));
            player.getCooldownTracker().setCooldown(player.getHeldItem(hand).getItem(), 1200);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    private static int getActualDay(@NotNull World world, long seed) {
        Random rand = new Random(seed);
        for (int i = 0; i < 10 + rand.nextInt(10); i++) rand.nextLong(); // 随机扰动

        int r = rand.nextInt(cycle);

        if (r >= 18) {
            r -= cycle;
        }

        long day = world.getWorldTime() / dayTime;
        long elapsedDay = day / cycle;
        int OffsetDay = (cycle - r) % cycle;
        int actualDay = (int) (elapsedDay * cycle + OffsetDay - day);
        if (actualDay < 0) {
            actualDay += 36;
        }
        return actualDay;
    }

}
