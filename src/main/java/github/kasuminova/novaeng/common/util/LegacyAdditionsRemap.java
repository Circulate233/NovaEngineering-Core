package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 原 Additions 模组移除后，旧存档中记录的是 `additions:novaextended-*` 形式的注册名。
 *
 * <p>物品栈在磁盘上以注册名字符串保存，因此只需在反序列化时把旧名换成新名即可让
 * 既有存档中的物品继续存在。未命中映射的名称原样返回，不影响其他模组的数据。
 */
public final class LegacyAdditionsRemap {

    private static final Map<String, String> ITEMS = new Object2ObjectOpenHashMap<>();

    static {
        // 材料与电路
        map("novaextended-star_ingot", "ingot_ark");
        map("novaextended-blue_alloy_ingot", "ingot_blue_alloy");
        map("novaextended-terraalloy", "ingot_terra_alloy");
        map("novaextended-psi_alloy", "ingot_psi_alloy");
        map("novaextended-ingot7", "ingot_machalloy");
        map("novaextended-ingot8", "ingot_willowalloy");
        map("novaextended-fallen_star_alloy", "ingot_fallen_star_alloy");
        map("novaextended-ark_circuit", "circuit_ark");
        map("novaextended-extremecircuit", "circuit_extreme");
        map("novaextended-crystal4", "gem_crystal_rgp");
        map("novaextended-lifeessence_crystal", "crystal_life_essence");
        map("novaextended-phocore_2", "photon_core_variant");
        map("novaextended-omniseed", "seed_omnipotent");

        // 勋章
        map("novaextended-novaextended_medal", "medal_radiant");
        map("novaextended-novaextended_medal1", "medal_radiant_auriga");
        map("novaextended-novaextended_medal2", "medal_radiant_horologium");
        map("novaextended-novaextended_medal3", "medal_radiant_chamaeleon");
        map("novaextended-novaextended_medal4", "medal_radiant_bootes");
        map("novaextended-novaextended_medal5", "medal_radiant_vivid");
        map("novaextended-chaotic_medal", "medal_chaotic_devour");
        map("novaextended-chaotic_medal1", "medal_chaotic_invert");

        // 工具、食物与弹射物
        map("novaextended-expert_tools", "expert_tools");
        map("novaextended-fallen_star_arrow", "fallen_star_arrow");
        map("pie-apple_pie", "pie_apple");
        map("pie-chocolate_pie", "pie_chocolate");
        map("pie-shepherds_pie", "pie_shepherds");
        map("pie-sweet_berry_cheesecake", "pie_sweet_berry_cheesecake");

        // 方块物品
        map("novaextended-ore8", "willowalloy_ore");
        map("novaextended-infinity_ore", "infinity_ore");
        map("novaextended-potato_server", "potato_server");
        map("novaextended-server_tier_1", "server_tier_1");
        map("novaextended-willowalloy_substrate", "willowalloy_substrate");
    }

    private LegacyAdditionsRemap() {
    }

    private static void map(final String legacyPath, final String newPath) {
        final String legacy = "additions:" + legacyPath;
        ITEMS.put(legacy, "novaeng_core:" + newPath);
    }

    /**
     * 把旧注册名转换为新注册名，未命中时原样返回。
     *
     * @param name 反序列化得到的注册名
     * @return 可用于当前注册表的注册名
     */
    @Nullable
    public static String apply(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        final String replacement = ITEMS.get(name);
        return replacement == null ? name : replacement;
    }

    /**
     * 把旧注册名转换为新注册名，未命中返回null
     *
     * @param name 反序列化得到的注册名
     * @return 可用于当前注册表的注册名
     */
    @Nullable
    public static String get(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return ITEMS.get(name);
    }

    /**
     * 是否为待重定向的旧注册名。
     */
    public static boolean isLegacy(final String name) {
        return name != null && ITEMS.containsKey(name);
    }

}
