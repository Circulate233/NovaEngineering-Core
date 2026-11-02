package github.kasuminova.novaeng.common.performance;

import java.util.List;

public final class IC2ConfigPaths {

    private static final List<String> DISABLE_ENDER_CHEST = List.of("balance", "disableEnderChest");

    private IC2ConfigPaths() {
    }

    public static List<String> tickPath(final String key, final char delimiter) {
        return delimiter == '/' && "balance/disableEnderChest".equals(key) ? DISABLE_ENDER_CHEST : null;
    }
}
