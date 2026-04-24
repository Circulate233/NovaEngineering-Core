package github.kasuminova.novaeng.common.profiler;

import com.mojang.authlib.GameProfile;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.handler.HyperNetEventHandler;
import github.kasuminova.novaeng.common.network.packetprofiler.PktCProfilerRequest;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CPacketProfilerDataProcessor {

    public static final CPacketProfilerDataProcessor INSTANCE = new CPacketProfilerDataProcessor();

    private final Map<GameProfile, CPacketProfilerData> receivedData = new ConcurrentHashMap<>();

    private ICommandSender sender = null;
    private UUID currentEvent = null;
    private GameProfile target = null;

    private long startTime = 0;

    private int players = 0;
    private int receivedPlayers = 0;

    private CPacketProfilerDataProcessor() {
    }

    private static void generateDefaultMessage(final List<ITextComponent> messages, final long maxBandwidthPerSecond, @Nullable final GameProfile maxPlayer, final Map<String, CPacketProfilerData.PacketData> mergedPackets, final Map<String, CPacketProfilerData.PacketData> mergedTileEntityPackets) {
        String maxPlayerName = maxPlayer == null ? "N/A" : maxPlayer.getName();
        messages.add(new TextComponentString(TextFormatting.GREEN + "最大带宽使用: ~" + TextFormatting.AQUA + MiscUtils.formatNumber(maxBandwidthPerSecond) + "B/s" + TextFormatting.GREEN + "，来自: " + TextFormatting.YELLOW + maxPlayerName));
        messages.add(new TextComponentString(TextFormatting.GREEN + "合并后数据: "));
        messages.add(new TextComponentString(TextFormatting.GREEN + "普通数据包: "));
        generatePktMessage(messages, mergedPackets);
        messages.add(new TextComponentString(TextFormatting.GREEN + "TileEntity 数据包: "));
        generatePktMessage(messages, mergedTileEntityPackets);
    }

    private static void generatePktMessage(final List<ITextComponent> messages, final Map<String, CPacketProfilerData.PacketData> mergedTileEntityPackets) {
        for (Map.Entry<String, CPacketProfilerData.PacketData> entry : mergedTileEntityPackets.entrySet()) {
            messages.add(new TextComponentString(
                "PktClass: " +
                    TextFormatting.GOLD + entry.getKey() + TextFormatting.RESET + ": " +
                    TextFormatting.RED + MiscUtils.formatNumber(entry.getValue().totalSize()) + 'B' +
                    TextFormatting.WHITE + ", PktCnt: " + TextFormatting.AQUA + entry.getValue().count() +
                    TextFormatting.WHITE + ", SizeAvg: " + TextFormatting.YELLOW + MiscUtils.formatNumber(entry.getValue().totalSize() / entry.getValue().count()) + 'B'
            ));
        }
    }

    private static void generateTargetMessage(final List<ITextComponent> messages, final GameProfile target, final Map<String, CPacketProfilerData.PacketData> mergedPackets, final Map<String, CPacketProfilerData.PacketData> mergedTileEntityPackets) {
        messages.add(new TextComponentString(TextFormatting.GREEN + "目标玩家: " + TextFormatting.YELLOW + target.getName()));
        messages.add(new TextComponentString(TextFormatting.GREEN + "合并后数据: "));
        messages.add(new TextComponentString(TextFormatting.GREEN + "普通数据包: "));
        generatePktMessage(messages, mergedPackets);
        messages.add(new TextComponentString(TextFormatting.GREEN + "TileEntity 数据包: "));
        generatePktMessage(messages, mergedTileEntityPackets);
    }

    public void create(final ICommandSender sender, final int limit, @Nullable GameProfile target) {
        if (this.currentEvent != null) {
            NovaEngineeringCore.log.warn("Profiler collect task is already running, event ID: {}", currentEvent);
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "已存在一个收集任务！事件 ID: " + TextFormatting.YELLOW + currentEvent));
            return;
        }
        List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "无玩家在线，无法创建收集任务！"));
            return;
        }

        this.sender = sender;
        this.currentEvent = UUID.randomUUID();
        this.target = target;
        this.startTime = System.currentTimeMillis();
        requestPlayers(players, limit);
        createTask();
        this.sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "收集任务已创建，事件 ID: " + TextFormatting.YELLOW + currentEvent));
    }

    private void requestPlayers(List<EntityPlayerMP> players, final int limit) {
        this.players = players.size();
        this.receivedPlayers = 0;
        players.forEach(player -> NovaEngineeringCore.NET_CHANNEL.sendTo(new PktCProfilerRequest(currentEvent, limit), player));
    }

    private void createTask() {
        Thread.ofVirtual().name("NovaEng CPacket Profiler Collector").start(() -> {
            ProcessedData result;
            synchronized (receivedData) {
                long remaining = 5000L - (System.currentTimeMillis() - startTime);
                while (receivedPlayers < players && remaining > 0L) {
                    try {
                        receivedData.wait(remaining);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    remaining = 5000L - (System.currentTimeMillis() - startTime);
                }
                result = getProcessedData(System.currentTimeMillis() - startTime, receivedPlayers, players);
            }
            if (result.receivedPlayers() < result.players()) {
                NovaEngineeringCore.log.warn("Profiler collect task timeout ({}ms), received players: {}, players: {}", result.elapsedTime(), result.receivedPlayers(), result.players());
            } else {
                NovaEngineeringCore.log.info("Profiler collect task completed ({}ms), received players: {}, players: {}", result.elapsedTime(), result.receivedPlayers(), result.players());
            }
            HyperNetEventHandler.addTickEndAction(() -> finish(result));
        });
    }

    private void finish(ProcessedData result) {
        Map<String, CPacketProfilerData.PacketData> mergedPackets;
        Map<String, CPacketProfilerData.PacketData> mergedTileEntityPackets;

        mergedPackets = result.mergedPackets().entrySet().stream()
                              .sorted(Map.Entry.comparingByValue())
                              .collect(Object2ObjectLinkedOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
        mergedTileEntityPackets = result.mergedTileEntityPackets().entrySet().stream()
                                        .sorted(Map.Entry.comparingByValue())
                                        .collect(Object2ObjectLinkedOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);

        List<ITextComponent> messages = new ObjectArrayList<>();

        messages.add(new TextComponentString(TextFormatting.GREEN + "收集任务完成，事件 ID: " + TextFormatting.YELLOW + currentEvent));
        messages.add(new TextComponentString(TextFormatting.GREEN + "已收集数据: " + TextFormatting.YELLOW + result.receivedPlayers() + "/" + result.players()));
        messages.add(new TextComponentString(TextFormatting.GREEN + "总带宽使用: ~" + TextFormatting.AQUA + MiscUtils.formatNumber((long) result.totalBandwidthPerSecond()) + "B/s"));
        if (target == null) {
            generateDefaultMessage(messages, (long) result.maxBandwidthPerSecond(), result.maxPlayer(), mergedPackets, mergedTileEntityPackets);
        } else {
            generateTargetMessage(messages, target, mergedPackets, mergedTileEntityPackets);
        }

        messages.forEach(message -> sender.sendMessage(message));
        reset();
    }

    private void reset() {
        receivedData.clear();

        sender = null;
        currentEvent = null;
        target = null;

        startTime = 0;

        receivedPlayers = 0;
        players = 0;
    }

    @Nonnull
    private ProcessedData getProcessedData(final long elapsedTime, final int receivedPlayers, final int players) {
        Map<GameProfile, CPacketProfilerData> sorted = receivedData.entrySet().stream()
                                                                   .sorted(Map.Entry.comparingByValue())
                                                                   .collect(Object2ObjectLinkedOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);

        double totalBandwidthPerSecond = sorted.values().stream()
                                               .mapToDouble(CPacketProfilerData::getNetworkBandwidthPerSecond)
                                               .sum();

        GameProfile maxPlayer = sorted.entrySet().stream()
                                      .max(Comparator.comparingDouble(entry -> entry.getValue().getNetworkBandwidthPerSecond()))
                                      .map(Map.Entry::getKey)
                                      .orElse(null);
        double maxBandwidthPerSecond = sorted.values().stream()
                                             .mapToDouble(CPacketProfilerData::getNetworkBandwidthPerSecond)
                                             .max()
                                             .orElse(0);

        Map<String, CPacketProfilerData.PacketData> mergedPackets = new Object2ObjectOpenHashMap<>();
        Map<String, CPacketProfilerData.PacketData> mergedTileEntityPackets = new Object2ObjectOpenHashMap<>();

        for (CPacketProfilerData data : sorted.values()) {
            final Map<String, CPacketProfilerData.PacketData> finalMergedPackets = mergedPackets;
            data.getPackets().forEach((packetName, packetData) -> {
                CPacketProfilerData.PacketData mergedPacketData = finalMergedPackets.get(packetName);
                if (mergedPacketData == null) {
                    finalMergedPackets.put(packetName, packetData);
                } else {
                    mergedPacketData.merge(packetData);
                }
            });
            final Map<String, CPacketProfilerData.PacketData> finalMergedTileEntityPackets = mergedTileEntityPackets;
            data.getTileEntityPackets().forEach((packetName, packetData) -> {
                CPacketProfilerData.PacketData mergedPacketData = finalMergedTileEntityPackets.get(packetName);
                if (mergedPacketData == null) {
                    finalMergedTileEntityPackets.put(packetName, packetData);
                } else {
                    mergedPacketData.merge(packetData);
                }
            });
        }
        return new ProcessedData(elapsedTime, receivedPlayers, players, totalBandwidthPerSecond, maxPlayer, maxBandwidthPerSecond, mergedPackets, mergedTileEntityPackets);
    }

    public void receive(final UUID eventId, final GameProfile player, final CPacketProfilerData data) {
        if (currentEvent == null) {
            return;
        }
        if (!currentEvent.equals(eventId)) {
            NovaEngineeringCore.log.warn("Received profiler data from {} with wrong eventId!", player.getName());
            return;
        }
        synchronized (receivedData) {
            receivedPlayers++;
            receivedData.put(player, data);
            receivedData.notifyAll();
        }
        NovaEngineeringCore.log.info("Received profiler data from {}", player.getName());
    }

    private record ProcessedData(long elapsedTime,
                                 int receivedPlayers,
                                 int players,
                                 double totalBandwidthPerSecond,
                                 GameProfile maxPlayer,
                                 double maxBandwidthPerSecond,
                                 Map<String, CPacketProfilerData.PacketData> mergedPackets,
                                 Map<String, CPacketProfilerData.PacketData> mergedTileEntityPackets) {
    }

}
