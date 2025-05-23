package github.kasuminova.novaeng.common.machine;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.novaeng.common.util.FixedSizeDeque;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils.*;

@ZenRegister
@ZenClass("novaeng.DreamEnergyCore")
public class DreamEnergyCore implements MachineSpecial{
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "dream_energy_core");
    public static final DreamEnergyCore INSTANCE = new DreamEnergyCore();
    public static long defaultTransferAmount = 10000000;
    private static final Map<World,Map<BlockPos, FixedSizeDeque<String>>> map = new ConcurrentHashMap<>();
    private static final int MinuteScale = 30;

    @ZenMethod
    public static long setDefaultTransferAmount(long value){
        defaultTransferAmount = value;
        return defaultTransferAmount;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onClientTick(final TileMultiblockMachineController ctrl) {
        var world = ctrl.getWorld();
        if (world.getWorldTime() % (1200 / MinuteScale) == 0){
            var data = ctrl.getCustomDataTag();
            var energyStored = data.getString("energyStored").isEmpty() ? "0":data.getString("energyStored");
            getEnergyInfo(world,ctrl.getPos()).addFirst(energyStored);
        }
    }

    @Override
    public void init(DynamicMachine machine) {
        if (isClient) {
            machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> {
                var ctrl = event.getController();
                var data = ctrl.getCustomDataTag();
                var speed = data.getFloat("speed");
                var energyStored = data.getString("energyStored").isEmpty() ? "0":data.getString("energyStored");

                String[] info = {
                        "§b/////////// 梦之管理者 ///////////",
                        "§b能量储存：§a" + formatNumber(energyStored) + " RF",
                        "§b输入输出值：§a" + formatNumber((long) (defaultTransferAmount * speed)) + " RF/t",
                        "§b一分钟内平均交互速度：§a" + change(ctrl) + " RF/t",
                        "§b///////////////////////////////////"
                };

                event.setExtraInfo(info);
            });
        }
    }

    private String change(TileMultiblockMachineController ctrl){
        FixedSizeDeque<String> energy = getEnergyInfo(ctrl.getWorld(),ctrl.getPos());
        var newtime = energy.getFirst();
        var oldtime = energy.getLast();
        var newbig = new BigInteger(newtime);
        var oldbig = new BigInteger(oldtime);
        var changel = newbig.subtract(oldbig);

        return formatNumber(changel.compareTo(BigLongMax) >= 0 ? Long.MAX_VALUE : changel.longValue() / (1200L / MinuteScale * energy.size()));
    }

    private static FixedSizeDeque<String> getEnergyInfo(World world,BlockPos pos) {
        return map.computeIfAbsent(world, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(pos, m -> new FixedSizeDeque<>(MinuteScale));
    }
}
