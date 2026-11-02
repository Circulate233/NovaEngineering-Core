package github.kasuminova.novaeng.common.profiler;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Comparator;
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
        Map<Class<?>, AtomicLong> packetCounter = new ConcurrentHashMap<>();
        PLAYER_CLIENT_PACKETS.forEach((profile, map) ->
            map.forEach((pClass, counter) ->
                packetCounter.computeIfAbsent(pClass, v -> new AtomicLong(0)).addAndGet(counter.get())));

        List<Map.Entry<Class<?>, AtomicLong>> largest = new ArrayList<>(packetCounter.entrySet());
        largest.sort((o1, o2) -> Long.compare(o2.getValue().get(), o1.getValue().get()));
        int largestLimit = Math.min(50, packetCounter.size() / 2 + packetCounter.size() % 2);
        if (largest.size() > largestLimit) largest.subList(largestLimit, largest.size()).clear();

        List<Map.Entry<Class<?>, AtomicLong>> smallest = new ArrayList<>(packetCounter.entrySet());
        smallest.sort(Comparator.comparingLong(o -> o.getValue().get()));
        int smallestLimit = Math.min(50, packetCounter.size() / 2);
        if (smallest.size() > smallestLimit) smallest.subList(smallestLimit, smallest.size()).clear();

        Map<Class<?>, Map<GameProfile, AtomicLong>> pClassCounter = new ConcurrentHashMap<>();
        PLAYER_CLIENT_PACKETS.forEach((profile, map) -> map.forEach((pClass, counter) -> pClassCounter
            .computeIfAbsent(pClass, v -> new ConcurrentHashMap<>())
            .computeIfAbsent(profile, v -> new AtomicLong(0))
            .addAndGet(counter.get()))
        );

        List<String> messages = new ArrayList<>();
        messages.add(TextFormatting.BLUE + "已接收的网络包（最大排序，至多 50）：");
        largest.forEach(entry -> {
            Class<?> pClass = entry.getKey();
            AtomicLong counter = entry.getValue();
            messages.add(TextFormatting.BLUE + "  " + getPacketClassName(pClass) + ": " + TextFormatting.GOLD + counter.get());
        });

        messages.add(TextFormatting.BLUE + "已接收的网络包（最小排序，至多 50）：");
        smallest.forEach(entry -> {
            Class<?> pClass = entry.getKey();
            AtomicLong counter = entry.getValue();
            messages.add(TextFormatting.BLUE + "  " + getPacketClassName(pClass) + ": " + TextFormatting.GOLD + counter.get());
        });

        messages.add(TextFormatting.RED + "统计中的可疑网络包：");
        pClassCounter.forEach((pClass, map) -> {
            if (map.size() == 1) {
                map.forEach((profile, counter) -> {
                    messages.add(TextFormatting.RED + profile.getName() + " (" + profile.getId() + ")");
                    messages.add(TextFormatting.BLUE + "  " + getPacketClassName(pClass) + ": " + TextFormatting.GOLD + counter.get());
                });
            }
        });

        return messages;
    }

    public static List<String> getFullProfilerMessages() {
        List<String> messages = new ArrayList<>();

        PLAYER_CLIENT_PACKETS.forEach((profile, map) -> {
            messages.add(TextFormatting.BLUE + profile.getName() + " (" + profile.getId() + ")");
            List<Map.Entry<Class<?>, AtomicLong>> sorted = new ArrayList<>(map.entrySet());
            sorted.sort((o1, o2) -> Long.compare(o2.getValue().get(), o1.getValue().get()));
            for (Map.Entry<Class<?>, AtomicLong> entry : sorted) {
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
