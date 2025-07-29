package github.kasuminova.novaeng.common.machine;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IBlockPos;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.novaeng.common.tile.TileDreamEnergyPort;
import github.kasuminova.novaeng.common.util.FixedSizeDeque;
import github.kasuminova.novaeng.common.util.IBlockPosEx;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
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
    public static final String MachineID = "dream_energy_core";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MachineID);
    public static final DreamEnergyCore INSTANCE = new DreamEnergyCore();
    private static final Map<World,Map<BlockPos, FixedSizeDeque<String>>> map = new ConcurrentHashMap<>();
    private static final Map<String, BigInteger> ENERGY_STORED_CACHE = new ConcurrentHashMap<>();

    private static final int MinuteScale = 30;
    //最小传输速度,按倍计。
    private static float minSpeed = 0.01f;
    //最大传输速度,按倍计。
    private static int maxSpeed = 100000;
    //基础输入输出速度。能量输入输出速度计算方法为:defaultTransferAmount * speed,其中 speed 可由玩家控制。
    private static long defaultTransferAmount = 100000000;

    private static IBlockPos facePos;

    @ZenMethod
    public static void setFacePos(IBlockPos facePos) {
        DreamEnergyCore.facePos = facePos;
    }

    @ZenMethod
    public static long setDefaultTransferAmount(long varue){
        defaultTransferAmount = varue;
        return defaultTransferAmount;
    }

    @ZenMethod
    public static float setMinSpeed(float varue){
        minSpeed = varue;
        return minSpeed;
    }

    @ZenMethod
    public static int setMaxSpeed(int varue){
        maxSpeed = varue;
        return maxSpeed;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    @Override
    public void init(DynamicMachine machine) {
        SInit(machine);
        if (isClient) {
            CInit(machine);
        }
    }

    public void SInit(DynamicMachine machine){
        machine.addMachineEventHandler(MachineTickEvent.class,event -> {
            if (event.phase == Phase.START) {
                var ctrl = event.getController();
                var world = ctrl.getWorld();
                if (world.getWorldTime() % (1200 / MinuteScale) == 0) {
                    var data = ctrl.getCustomDataTag();
                    var energyStored = data.getString("energyStored").isEmpty() ? "0" : data.getString("energyStored");
                    getEnergyInfo(world, ctrl.getPos()).addFirst(energyStored);
                    data.setString("chance", change(ctrl,energyStored));
                }
            }
        });
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class, event -> {
            TileMultiblockMachineController ctrl = event.getController();
            ctrl.setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC);
            BlockPos facePos = ctrl.getPos().up();
            if (DreamEnergyCore.facePos != null){
                facePos = IBlockPosEx.createPosByFacing(ctrl.getPos(),ctrl.getControllerRotation(), DreamEnergyCore.facePos.getX(), DreamEnergyCore.facePos.getY(), DreamEnergyCore.facePos.getZ());
            }
            if (ctrl.getWorld().getTileEntity(facePos) instanceof TileDreamEnergyPort tdep){
                tdep.setCtrlPos(ctrl.getPos());
                ctrl.getCustomDataTag().setBoolean("wireless",true);
            } else {
                ctrl.getCustomDataTag().setBoolean("wireless",false);
            }
        });

        var inputThreadName = "梦之收集者";
        machine.addCoreThread(FactoryRecipeThread.createCoreThread(inputThreadName));
        var outputThreadName = "梦之释放者";
        machine.addCoreThread(FactoryRecipeThread.createCoreThread(outputThreadName));

        // 输出配方
        RecipeBuilder.newBuilder("extract", MachineID, 1, 1, true)
                .addEnergyPerTickOutput(defaultTransferAmount)
                .addPreCheckHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    if (data.getBoolean("wireless")){
                        event.setFailed("当前处于通量模式");
                        return;
                    }
                    var speed = Math.max(1.0f,data.getFloat("speed"));
                    if (!canExtract(data, speed)) {
                        event.setFailed("内部能量储量不足！");
                        return;
                    }
                    ctrl.addPermanentModifier("extract", RecipeModifierBuilder.create("modularmachinery:energy", "output", speed, 1, false).build());
                })
                .addFactoryFinishHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var speed = Math.max(1.0f,data.getFloat("speed"));
                    extractEnergy(data, speed, defaultTransferAmount);
                    ctrl.markNoUpdateSync();
                })
                .setParallelized(false)
                .addRecipeTooltip("由梦之收集者运行。", "在智能数据接口处修改速度。")
                .addSmartInterfaceDataInput("speed", minSpeed, maxSpeed)
                .setThreadName(outputThreadName)
                .build();

        // 输入配方
        RecipeBuilder.newBuilder("receive", MachineID, 1, 2, true)
                .addEnergyPerTickInput(defaultTransferAmount)
                .addPreCheckHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    if (data.getBoolean("wireless")){
                        event.setFailed("当前处于通量模式");
                    }
                    var speed = Math.max(1.0f,data.getFloat("speed"));
                    ctrl.addPermanentModifier("receive", RecipeModifierBuilder.create("modularmachinery:energy", "input", speed, 1, false).build());
                })
                .addFactoryPreTickHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var speed = Math.max(1.0f,data.getFloat("speed"));
                    ctrl.addPermanentModifier("receive", RecipeModifierBuilder.create("modularmachinery:energy", "input", speed, 1, false).build());
                })
                .addFactoryPostTickHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var speed = Math.max(1.0f,data.getFloat("speed"));
                    receiveEnergy(data, speed,defaultTransferAmount);
                    ctrl.markNoUpdateSync();
                })
                .setParallelized(false)
                .addRecipeTooltip("由梦之释放者运行。", "在智能数据接口处修改速度。")
                .addSmartInterfaceDataInput("speed", minSpeed, maxSpeed)
                .setThreadName(inputThreadName)
                .build();
    }

    @SideOnly(Side.CLIENT)
    public void CInit(DynamicMachine machine){
        machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> {
            var ctrl = event.getController();
            var data = ctrl.getCustomDataTag();
            var speed = data.hasKey("speed") ? data.getFloat("speed") : 1.0f;
            var energyStored = data.getString("energyStored").isEmpty() ? "0":data.getString("energyStored");
            var chance = data.getString("chance");

            String[] info = {
                    "§b/////////// 梦之管理者 ///////////",
                    "§b能量储存:§a" + formatNumber(getBigInt(energyStored)) + " RF",
                    data.getBoolean("wireless") ? "§b输入输出速度:#FF6347-FFA54F-FFFF00-7FFF00-40E0D0-00BFFFInfinity" : "§b输入输出速度:§a" + formatNumber((long) (defaultTransferAmount * speed),1) + " RF/t",
                    "§b一分钟内平均交互速度:§a" + (chance.isEmpty() ? "0" : chance) + " RF/t",
                    "§b///////////////////////////////////"
            };

            event.setExtraInfo(info);
        });
    }

    /**
     * 能否提取能量。
     */
    private static boolean canExtract(NBTTagCompound nbt,float speed){
        if (nbt.hasKey("energyStored")) {
            var energyStored = getEnergyStored(nbt);
            var sz = (long) (speed * defaultTransferAmount);
            if (energyStored.compareTo(BigLongMax) >= 0) {
                if (ENERGY_STORED_CACHE.size() > 3000) {
                    ENERGY_STORED_CACHE.clear();
                }
                return true;
            }
            return energyStored.longValue() >= sz;
        }
        return false;
    }

    /**
     * 将能量存储进控制器内部。
     */
    public static void receiveEnergy(NBTTagCompound nbt,float speed,long defaultTransferAmount) {
        synchronized (nbt) {
            var energyStored = getEnergyStored(nbt);
            var sz = getBigInt((long) (speed * defaultTransferAmount));
            nbt.setString("energyStored", energyStored.add(sz).toString());
        }

        if (ENERGY_STORED_CACHE.size() > 3000) {
            ENERGY_STORED_CACHE.clear();
        }
    }

    /**
     * 提取控制器内部能量至能量输出仓。
     */
    public static void extractEnergy(NBTTagCompound nbt,float speed,long defaultTransferAmount) {
        synchronized (nbt) {
            var energyStored = getEnergyStored(nbt);
            var sz = getBigInt((long) (speed * defaultTransferAmount));
            nbt.setString("energyStored",energyStored.subtract(sz).toString());
        }

        if (ENERGY_STORED_CACHE.size() > 3000) {
            ENERGY_STORED_CACHE.clear();
        }
    }

    public static BigInteger getEnergyStored(NBTTagCompound nbt){
        return nbt.hasKey("energyStored") ? getBigInt(nbt.getString("energyStored")) : BigInteger.ZERO;
    }

    @ZenMethod
    public static BigInteger getEnergyStored(IMachineController ctrl){
        return getEnergyStored(ctrl.getController().getCustomDataTag());
    }

    /**
     * 额外的crt方法复用方法作为能量输入方法
     * @param ctrl 控制器
     * @param speed 倍率
     * @param amount 每倍率消耗
     */
    @ZenMethod
    public static void receiveEnergy(IMachineController ctrl,float speed,long amount){
        receiveEnergy(ctrl.getController().getCustomDataTag(),speed,amount);
        ctrl.getController().markNoUpdateSync();
    }

    /**
     * 额外的crt方法复用方法作为能量消耗方法
     * @param ctrl 控制器
     * @param speed 倍率
     * @param amount 每倍率消耗
     */
    @ZenMethod
    public static void extractEnergy(IMachineController ctrl,float speed,long amount){
        extractEnergy(ctrl.getController().getCustomDataTag(),speed,amount);
        ctrl.getController().markNoUpdateSync();
    }

    @ZenMethod
    public static BigInteger getBigInt(long num){
        return getBigInt(Long.toString(num));
    }

    @ZenMethod
    public static BigInteger getBigInt(String num){
        return ENERGY_STORED_CACHE.computeIfAbsent(num, BigInteger::new);
    }

    private static final String longmax = Long.toString(Long.MAX_VALUE);

    private String change(TileMultiblockMachineController ctrl){
        return change(ctrl,getEnergyInfo(ctrl.getWorld(),ctrl.getPos()).getFirst());
    }

    private String change(TileMultiblockMachineController ctrl,String newtime){
        FixedSizeDeque<String> energy = getEnergyInfo(ctrl.getWorld(),ctrl.getPos());
        var oldtime = energy.getLast();
        var newbig = newtime == null ? BigInteger.ZERO : getBigInt(newtime);
        var oldbig = oldtime == null ? BigInteger.ZERO : getBigInt(oldtime);
        if (newbig.equals(oldbig)) {
            return "0";
        } else {
            var changel = newbig.subtract(oldbig);
            var denominator = 1200L / MinuteScale * energy.size();
            if (changel.compareTo(BigLongMax) > 0) {
                return formatNumber(changel.divide(getBigInt(denominator)));
            } else {
                return formatNumber(changel.longValue() / denominator);
            }
        }
    }

    private static FixedSizeDeque<String> getEnergyInfo(World world,BlockPos pos) {
        return map.computeIfAbsent(world, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(pos, m -> new FixedSizeDeque<>(MinuteScale));
    }
}