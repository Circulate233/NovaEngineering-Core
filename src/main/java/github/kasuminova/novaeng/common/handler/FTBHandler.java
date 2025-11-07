package github.kasuminova.novaeng.common.handler;

import com.feed_the_beast.ftbquests.events.CustomRewardEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.val;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FTBHandler {

    private static final Int2ObjectMap<TextComponentTranslation> ids = new Int2ObjectOpenHashMap<>() {{
        put(-697329152,
            new TextComponentTranslation("new.ftb.as.tooltip",
                new TextComponentTranslation("new.ftb.as.tooltip.0")));
        put(1304181469,
            new TextComponentTranslation("new.ftb.as.tooltip",
                new TextComponentTranslation("new.ftb.as.tooltip.1")));
        put(-845382584,
            new TextComponentTranslation("new.ftb.as.tooltip",
                new TextComponentTranslation("new.ftb.as.tooltip.2")));
        put(-739358833,
            new TextComponentTranslation("new.ftb.as.tooltip",
                new TextComponentTranslation("new.ftb.as.tooltip.3")));
    }};
    public static FTBHandler INSTANCE = new FTBHandler();

    private FTBHandler() {

    }

    @SubscribeEvent
    public void onCustomReward(CustomRewardEvent event) {
        val id = event.getReward().quest.id;
        if (ids.containsKey(id)) {
            val player = event.getPlayer();
            if (!player.world.isRemote) {
                player.sendMessage(ids.get(id));
            }
        }
    }
}