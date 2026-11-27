package github.kasuminova.novaeng.common.profiler;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SPacketProfiler {

    private static final Map<GameProfile, Map<Class<?>, AtomicLong>> PLAYER_CLIENT_PACKETS = new ConcurrentHashMap<>();

    public static void onPacketReceived(final NetworkManager networkManager, final Object packet) {
        INetHandler netHandler = networkManager.getNetHandler();
        if (!(netHandler instanceof NetHandlerPlayServer handlerServer)) {
            return;
        }

        GameProfile profile = handlerServer.player.getGameProfile();
        Map<Class<?>, AtomicLong> packetCounter = PLAYER_CLIENT_PACKETS.computeIfAbsent(profile, v -> new ConcurrentHashMap<>());

        packetCounter.computeIfAbsent(packet.getClass(), v -> new AtomicLong(0)).getAndIncrement();
    }

    public static void onPacketReceived(final EntityPlayer player, final Object packet) {
        GameProfile profile = player.getGameProfile();
        Map<Class<?>, AtomicLong> packetCounter = PLAYER_CLIENT_PACKETS.computeIfAbsent(profile, v -> new ConcurrentHashMap<>());

        packetCounter.computeIfAbsent(packet.getClass(), v -> new AtomicLong(0)).getAndIncrement();
    }

    public static List<String> getProfilerMessages() {
        final Object2LongOpenHashMap<Class<?>> packetCounter = new Object2LongOpenHashMap<>();
        PLAYER_CLIENT_PACKETS.forEach((profile, map) ->
            map.forEach((pClass, counter) ->
                packetCounter.addTo(pClass, counter.get())));

        final List<Object2LongMap.Entry<Class<?>>> largest = new ObjectArrayList<>(packetCounter.object2LongEntrySet());
        largest.sort((o1, o2) -> Long.compare(o2.getLongValue(), o1.getLongValue()));
        int largestLimit = Math.min(50, packetCounter.size() / 2 + packetCounter.size() % 2);
        if (largest.size() > largestLimit) largest.subList(largestLimit, largest.size()).clear();

        final List<Object2LongMap.Entry<Class<?>>> smallest = new ObjectArrayList<>(packetCounter.object2LongEntrySet());
        smallest.sort((o1, o2) -> Long.compare(o1.getLongValue(), o2.getLongValue()));
        int smallestLimit = Math.min(50, packetCounter.size() / 2);
        if (smallest.size() > smallestLimit) smallest.subList(smallestLimit, smallest.size()).clear();

        final Object2ObjectOpenHashMap<Class<?>, Object2LongOpenHashMap<GameProfile>> pClassCounter = new Object2ObjectOpenHashMap<>();
        PLAYER_CLIENT_PACKETS.forEach((profile, map) -> map.forEach((pClass, counter) ->
            pClassCounter.computeIfAbsent(pClass, ignored -> new Object2LongOpenHashMap<>()).addTo(profile, counter.get())));

        List<String> messages = new ObjectArrayList<>();
        messages.add(TextFormatting.BLUE + "已接收的网络包（最大排序，至多 50）：");
        for (int index = 0; index < largest.size(); index++) {
            final Object2LongMap.Entry<Class<?>> entry = largest.get(index);
            Class<?> pClass = entry.getKey();
            long counter = entry.getLongValue();
            messages.add(TextFormatting.BLUE + "  " + getPacketClassName(pClass) + ": " + TextFormatting.GOLD + counter);
        }

        messages.add(TextFormatting.BLUE + "已接收的网络包（最小排序，至多 50）：");
        for (int index = 0; index < smallest.size(); index++) {
            final Object2LongMap.Entry<Class<?>> entry = smallest.get(index);
            Class<?> pClass = entry.getKey();
            long counter = entry.getLongValue();
            messages.add(TextFormatting.BLUE + "  " + getPacketClassName(pClass) + ": " + TextFormatting.GOLD + counter);
        }

        messages.add(TextFormatting.RED + "统计中的可疑网络包：");
        pClassCounter.forEach((pClass, map) -> {
            if (map.size() == 1) {
                final Object2LongMap.Entry<GameProfile> entry = map.object2LongEntrySet().iterator().next();
                final GameProfile profile = entry.getKey();
                messages.add(TextFormatting.RED + profile.getName() + " (" + profile.getId() + ")");
                messages.add(TextFormatting.BLUE + "  " + getPacketClassName(pClass) + ": " + TextFormatting.GOLD + entry.getLongValue());
            }
        });

        return messages;
    }

    public static List<String> getFullProfilerMessages() {
        List<String> messages = new ObjectArrayList<>();

        PLAYER_CLIENT_PACKETS.forEach((profile, map) -> {
            messages.add(TextFormatting.BLUE + profile.getName() + " (" + profile.getId() + ")");
            List<Map.Entry<Class<?>, AtomicLong>> sorted = new ObjectArrayList<>(map.entrySet());
            sorted.sort((o1, o2) -> Long.compare(o2.getValue().get(), o1.getValue().get()));
            for (int index = 0; index < sorted.size(); index++) {
                final Map.Entry<Class<?>, AtomicLong> entry = sorted.get(index);
                Class<?> pClass = entry.getKey();
                AtomicLong counter = entry.getValue();
                messages.add(TextFormatting.BLUE + "  " + getPacketClassName(pClass) + ": " + TextFormatting.GOLD + counter.get());
            }
        });

        return messages;
    }

    private static String getPacketClassName(final Class<?> pClass) {
        if (pClass.getName().startsWith("net.minecraft")) {
            return pClass.getSimpleName();
        }
        return pClass.getName();
    }

}
