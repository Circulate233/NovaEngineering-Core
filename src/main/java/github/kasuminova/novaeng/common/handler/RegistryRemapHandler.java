package github.kasuminova.novaeng.common.handler;

import github.kasuminova.novaeng.common.registry.RegistryExtended;
import github.kasuminova.novaeng.common.util.LegacyAdditionsRemap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 处理旧存档中属于已移除的 Additions 模组的方块记录。
 *
 * <p>区块以数字 ID 存储方块，注册表按名解析。为 {@code additions:novaextended-ore8}
 * 调用 {@code remap} 可保留其原有数字 ID 并指向新方块，世界中已生成的柳钢矿石不会消失。
 */
public class RegistryRemapHandler {

    public static final RegistryRemapHandler INSTANCE = new RegistryRemapHandler();

    private static final String NS_LEGACY = "additions";

    private static final String LEGACY_ORE = "novaextended-ore8";
    private static final String LEGACY_INFINITY_ORE = "novaextended-infinity_ore";
    private static final String LEGACY_POTATO_SERVER = "novaextended-potato_server";
    private static final String LEGACY_SERVER_TIER_1 = "novaextended-server_tier_1";
    private static final String LEGACY_SUBSTRATE = "novaextended-willowalloy_substrate";

    @SubscribeEvent
    public void onMissingBlocks(final RegistryEvent.MissingMappings<Block> event) {
        for (final RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            if (!NS_LEGACY.equals(mapping.key.getNamespace())) {
                continue;
            }
            final Block replacement = blockFor(mapping.key.getPath());
            if (replacement != null) {
                mapping.remap(replacement);
            }
        }
    }

    @SubscribeEvent
    public void onMissingItems(final RegistryEvent.MissingMappings<Item> event) {
        for (final RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (!NS_LEGACY.equals(mapping.key.getNamespace())) {
                continue;
            }
            final String name = LegacyAdditionsRemap.get(mapping.key.toString());
            if (name == null) {
                continue;
            }
            final Item item = Item.getByNameOrId(name);
            if (item != null) {
                mapping.remap(item);
            }
        }
    }

    private static Block blockFor(final String path) {
        return switch (path) {
            case LEGACY_ORE -> RegistryExtended.WILLOWALLOY_ORE;
            case LEGACY_INFINITY_ORE -> RegistryExtended.INFINITY_ORE;
            case LEGACY_POTATO_SERVER -> RegistryExtended.POTATO_SERVER;
            case LEGACY_SERVER_TIER_1 -> RegistryExtended.SERVER_TIER_1;
            case LEGACY_SUBSTRATE -> RegistryExtended.WILLOWALLOY_SUBSTRATE;
            default -> null;
        };
    }

}
