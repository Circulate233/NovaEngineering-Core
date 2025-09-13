package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerNull;
import appeng.crafting.MECraftingInventory;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import co.neeve.nae2.common.crafting.patterntransform.PatternTransformWrapper;
import co.neeve.nae2.common.helpers.VirtualPatternDetails;
import co.neeve.nae2.common.interfaces.ICancellingCraftingMedium;
import com.glodblock.github.coremod.CoreModHooks;
import com.glodblock.github.util.FluidCraftingPatternDetails;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.novaeng.common.tile.MEPatternProviderNova;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorMEChannel;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker;
import github.kasuminova.novaeng.common.util.MediumType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.val;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

@Mixin(value = CraftingCPUCluster.class, remap = false ,priority = 0)
public abstract class MixinCraftingCPUClusterTwo {

    @Shadow
    protected abstract void postChange(IAEItemStack diff, IActionSource src);

    @Shadow
    protected abstract void postCraftingStatusChange(IAEItemStack diff);

    @Shadow
    private int remainingOperations;
    @Shadow
    private MachineSource machineSrc;
    @Shadow
    private MECraftingInventory inventory;

    @Shadow
    @Final
    private Map<ICraftingPatternDetails, ?> tasks;

    @Shadow
    protected abstract boolean canCraft(ICraftingPatternDetails details, IAEItemStack[] condensedInputs);

    @Shadow
    @Final
    private Map<ICraftingPatternDetails, Queue<ICraftingMedium>> visitedMediums;

    @Shadow
    protected abstract World getWorld();

    @Shadow
    private boolean somethingChanged;

    @Shadow
    private IItemList<IAEItemStack> waitingFor;

    @Shadow
    protected abstract void markDirty();

    @Shadow
    public abstract void cancel();

    @Shadow
    protected abstract void completeJob();

    @Shadow
    public abstract IAEItemStack injectItems(IAEItemStack input, Actionable type, IActionSource src);

    @Unique
    private boolean r$IgnoreParallel = false;

    @Unique
    private long r$craftingFrequency = 0;

    /**
     * @author circulation
     * @reason 完全覆写样板发配方法，已完成合并其余mod的mixin与asm TODO:待优化
     */
    @Overwrite
    private void executeCrafting(IEnergyGrid eg, CraftingGridCache cc) {
        val voidSet = new ObjectArrayList<IAEItemStack>();
        Iterator<?> i = this.tasks.entrySet().iterator();

        while (i.hasNext()) {
            //noinspection unchecked
            var e = (Map.Entry<ICraftingPatternDetails, AccessorTaskProgress>) i.next();
            var value = ((AccessorTaskProgress) e.getValue());
            if (value.getValue() <= 0L) {
                i.remove();
            } else {
                ICraftingPatternDetails details = r$getKey(e);
                if (this.canCraft(details, details.getCondensedInputs())) {
                    if (details instanceof VirtualPatternDetails) {
                        this.completeJob();
                        this.cancel();
                        return;
                    }
                    InventoryCrafting ic = null;
                    if (!this.visitedMediums.containsKey(details) || this.visitedMediums.get(details).isEmpty()) {
                        this.visitedMediums.put(details, new ArrayDeque<>(cc.getMediums(details).stream().filter(Objects::nonNull).collect(Collectors.toList())));
                    }
                    while (!this.visitedMediums.get(details).isEmpty()) {
                        ICraftingMedium m = this.visitedMediums.get(details).poll();
                        if (value.getValue() > 0L && m != null && !m.isBusy()) {
                            MediumType mediumType = r$specialMediumTreatment(m, details);
                            if (ic == null) {
                                IAEItemStack[] input = details.getInputs();
                                double sum = (double) 0.0F;

                                for (IAEItemStack anInput : input) {
                                    if (anInput != null) {
                                        sum += (double) CoreModHooks.getFluidSize(anInput);
                                    }
                                }

                                if (switch (mediumType) {
                                    case MEPatternProvider, EF -> {
                                        var sum1 = sum * this.r$craftingFrequency;
                                        var o = eg.extractAEPower(sum1, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                                        if (o < sum1 - 0.01) {
                                            long s = (long) (o / sum1 * this.r$craftingFrequency);
                                            this.r$craftingFrequency = s;
                                            if (s < 1) {
                                                yield eg.extractAEPower(sum, Actionable.MODULATE, PowerMultiplier.CONFIG);
                                            } else {
                                                yield eg.extractAEPower(sum * s, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                                            }
                                        }
                                        yield o;
                                    }
                                    default -> eg.extractAEPower(sum, Actionable.MODULATE, PowerMultiplier.CONFIG);
                                } < sum - 0.01) {
                                    continue;
                                }

                                if (details.isCraftable()) {
                                    ic = CoreModHooks.wrapCraftingBuffer(new InventoryCrafting(new ContainerNull(), 3, 3));
                                } else {
                                    ic = CoreModHooks.wrapCraftingBuffer(new InventoryCrafting(new ContainerNull(), 4, 4));
                                }

                                boolean found = false;

                                for (int x = 0; x < input.length; ++x) {
                                    if (input[x] != null) {
                                        found = false;
                                        if (details.isCraftable()) {
                                            Collection<IAEItemStack> itemList;
                                            if (details.canSubstitute()) {
                                                List<IAEItemStack> substitutes = details.getSubstituteInputs(x);
                                                itemList = new ObjectArrayList<>(substitutes.size());

                                                for (IAEItemStack stack : substitutes) {
                                                    itemList.addAll(this.inventory.getItemList().findFuzzy(stack, FuzzyMode.IGNORE_ALL));
                                                }
                                            } else {
                                                itemList = new ObjectArrayList<>(1);
                                                IAEItemStack item = this.inventory.getItemList().findPrecise(input[x]);
                                                if (item != null) {
                                                    itemList.add(item);
                                                } else if (input[x].getDefinition().getItem().isDamageable() || Platform.isGTDamageableItem(input[x].getDefinition().getItem())) {
                                                    itemList.addAll(this.inventory.getItemList().findFuzzy(input[x], FuzzyMode.IGNORE_ALL));
                                                }
                                            }

                                            for (IAEItemStack fuzz : itemList) {
                                                fuzz = fuzz.copy();
                                                fuzz.setStackSize(input[x].getStackSize());
                                                if (details.isValidItemForSlot(x, fuzz.createItemStack(), this.getWorld())) {
                                                    IAEItemStack ais = r$extractItemsR(this.inventory, fuzz, Actionable.MODULATE, this.machineSrc, mediumType);
                                                    ItemStack is = ais == null ? ItemStack.EMPTY : ais.createItemStack();
                                                    if (!is.isEmpty()) {
                                                        r$postChange2((CraftingCPUCluster) (Object) this, AEItemStack.fromItemStack(is), this.machineSrc, mediumType);
                                                        ic.setInventorySlotContents(x, is);
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            IAEItemStack ais = r$extractItemsR(this.inventory, input[x].copy(), Actionable.MODULATE, this.machineSrc, mediumType);
                                            ItemStack is = ais == null ? ItemStack.EMPTY : ais.createItemStack();
                                            if (!is.isEmpty()) {
                                                r$postChange1((CraftingCPUCluster) (Object) this, input[x], this.machineSrc, mediumType);
                                                ic.setInventorySlotContents(x, is);
                                                var count = is.getCount();
                                                if (mediumType != MediumType.NULL)
                                                    count /= (int) this.r$craftingFrequency;
                                                if (count == input[x].getStackSize()) {
                                                    found = true;
                                                    continue;
                                                }
                                            }
                                        }

                                        if (!found) {
                                            break;
                                        }
                                    }
                                }

                                if (!found) {
                                    for (int x = 0; x < ic.getSizeInventory(); ++x) {
                                        ItemStack is = CoreModHooks.removeFluidPackets(ic, x);
                                        if (!is.isEmpty()) {
                                            this.inventory.injectItems(CoreModHooks.wrapFluidPacketStack(AEItemStack.fromItemStack(is)), Actionable.MODULATE, this.machineSrc);
                                        }
                                    }

                                    ic = null;
                                    break;
                                }
                            }

                            ICraftingPatternDetails newDetails;
                            if (details instanceof PatternTransformWrapper wrapper) {
                                newDetails = wrapper.getDelegate();
                            } else {
                                newDetails = details;
                            }
                            if (m.pushPattern(newDetails, ic)) {
                                this.somethingChanged = true;
                                --this.remainingOperations;
                                if (mediumType != MediumType.NULL && !this.r$IgnoreParallel) {
                                    this.remainingOperations -= (int) this.r$craftingFrequency;
                                }

                                val outputs = details.getCondensedOutputs();
                                if (m instanceof ICancellingCraftingMedium medium) {
                                    if (medium.shouldAutoComplete()) {
                                        Collections.addAll(voidSet, outputs);
                                    }
                                }

                                for (IAEItemStack out : outputs) {
                                    r$postChange1((CraftingCPUCluster) (Object) this, out, this.machineSrc, mediumType);
                                    var iaeStack0 = out.copy();
                                    var iaeStack1 = out.copy();
                                    if (mediumType != MediumType.NULL) {
                                        iaeStack0.setStackSize(iaeStack0.getStackSize() * this.r$craftingFrequency);
                                        iaeStack1.setStackSize(iaeStack1.getStackSize() * this.r$craftingFrequency);
                                    }
                                    this.waitingFor.add(iaeStack0);
                                    this.postCraftingStatusChange(iaeStack1);
                                }

                                if (details.isCraftable()) {
                                    for (int x = 0; x < ic.getSizeInventory(); ++x) {
                                        ItemStack output = Platform.getContainerItem(CoreModHooks.removeFluidPackets(ic, x));
                                        if (!output.isEmpty()) {
                                            IAEItemStack cItem = AEItemStack.fromItemStack(output);
                                            r$postChange2((CraftingCPUCluster) (Object) this, cItem, this.machineSrc, mediumType);
                                            if (mediumType == MediumType.EF) {
                                                cItem.setStackSize(cItem.getStackSize() * this.r$craftingFrequency);
                                            }
                                            this.waitingFor.add(cItem);
                                            this.postCraftingStatusChange(cItem);
                                        }
                                    }
                                }

                                ic = null;
                                this.markDirty();
                                if (mediumType == MediumType.NULL) {
                                    value.setValue(value.getValue() - 1);
                                } else {
                                    value.setValue(value.getValue() - (this.r$craftingFrequency));
                                }
                                if (value.getValue() > 0L && this.remainingOperations == 0) {
                                    for (IAEItemStack output : voidSet) {
                                        try {
                                            this.nae2$ghostInject(output);
                                        } catch (NoSuchFieldException | IllegalAccessException ignored) {
                                        }
                                    }
                                    return;
                                }
                            }
                        }
                    }

                    if (ic != null) {
                        for (int x = 0; x < ic.getSizeInventory(); ++x) {
                            ItemStack is = CoreModHooks.removeFluidPackets(ic, x);
                            if (!is.isEmpty()) {
                                this.inventory.injectItems(CoreModHooks.wrapFluidPacketStack(AEItemStack.fromItemStack(is)), Actionable.MODULATE, this.machineSrc);
                            }
                        }
                    }
                }
            }
        }
        for (IAEItemStack output : voidSet) {
            try {
                this.nae2$ghostInject(output);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
    }

    @Unique
    private @NotNull MediumType r$specialMediumTreatment(ICraftingMedium m, ICraftingPatternDetails details) {
        MediumType mediumType;
        if (m instanceof MEPatternProviderNova mep) {
            if (mep.getWorkMode() == MEPatternProvider.WorkModeSetting.DEFAULT
                    || mep.getWorkMode() == MEPatternProvider.WorkModeSetting.ENHANCED_BLOCKING_MODE) {

                for (IAEItemStack input : details.getCondensedInputs()) {
                    long size = input.getStackSize() * this.r$craftingFrequency;
                    var item = this.inventory.extractItems(input.copy().setStackSize(size), Actionable.SIMULATE, this.machineSrc);
                    if (item == null) continue;
                    if (item.getStackSize() < size) {
                        long size0 = item.getStackSize() / input.getStackSize();
                        if (size0 < 2) {
                            this.r$craftingFrequency = 1;
                        } else {
                            this.r$craftingFrequency = size0;
                        }
                    }
                }

                mediumType = MediumType.MEPatternProvider;
                if (mep.r$isIgnoreParallel()) {
                    this.r$IgnoreParallel = true;
                } else {
                    this.r$IgnoreParallel = false;
                    this.r$craftingFrequency = Math.min(this.remainingOperations, this.r$craftingFrequency);
                }
            } else mediumType = MediumType.NULL;
        } else if (m instanceof EFabricatorMEChannel ef) {
            var max = 0;
            for (EFabricatorWorker worker : ef.getController().getWorkers()) {
                max += worker.getRemainingSpace();
            }
            for (IAEItemStack input : details.getInputs()) {
                if (input == null) continue;
                long size = this.r$craftingFrequency;
                var item = this.inventory.extractItems(input.copy().setStackSize(size), Actionable.SIMULATE, this.machineSrc);
                if (item == null) continue;
                if (item.getStackSize() < size) {
                    long size0 = item.getStackSize() / input.getStackSize();
                    if (size0 < 2) {
                        this.r$craftingFrequency = 1;
                    } else {
                        this.r$craftingFrequency = size0;
                    }
                }
            }
            mediumType = MediumType.EF;
            this.r$craftingFrequency = Math.min(max, this.r$craftingFrequency);
        } else mediumType = MediumType.NULL;
        return mediumType;
    }

    @Unique
    private ICraftingPatternDetails r$getKey(Map.Entry<ICraftingPatternDetails, AccessorTaskProgress> instance) {
        var key = instance.getKey();

        long max = 0;
        var list = (key.isCraftable() || key instanceof FluidCraftingPatternDetails) ? key.getCondensedOutputs() : key.getCondensedInputs();
        for (IAEItemStack stack : list) {
            long size = stack.getStackSize();
            if (size > max) max = size;
        }

        this.r$craftingFrequency = instance.getValue().getValue();
        if (max * this.r$craftingFrequency > Integer.MAX_VALUE) {
            this.r$craftingFrequency = Integer.MAX_VALUE / max;
        }

        return key;
    }

    @Unique
    private IAEItemStack r$extractItemsR(MECraftingInventory instance, IAEItemStack request, Actionable mode, IActionSource src, MediumType mediumType) {
        return switch (mediumType) {
            case MEPatternProvider, EF -> {
                var i = request.copy().setStackSize(request.getStackSize() * this.r$craftingFrequency);
                yield instance.extractItems(i, mode, src);
            }
            default -> instance.extractItems(request, mode, src);
        };
    }

    @Unique
    private void r$postChange1(CraftingCPUCluster instance, IAEItemStack receiver, IActionSource single, MediumType mediumType) {
        switch (mediumType) {
            case MEPatternProvider, EF -> {
                var i = receiver.copy().setStackSize(receiver.getStackSize() * this.r$craftingFrequency);
                this.postChange(i, single);
            }
            default -> this.postChange(receiver, single);
        }
    }

    @Unique
    private void r$postChange2(CraftingCPUCluster instance, IAEItemStack receiver, IActionSource single, MediumType mediumType) {
        switch (mediumType) {
            case EF -> {
                var i = receiver.copy().setStackSize(receiver.getStackSize() * this.r$craftingFrequency);
                this.postChange(i, single);
            }
            default -> this.postChange(receiver, single);
        }
    }

    @Unique
    private static Field nea2$field;

    @Unique
    protected void nae2$ghostInject(IAEItemStack output) throws IllegalAccessException, NoSuchFieldException {
        if (nea2$field == null) {
            nea2$field = this.getClass().getField("nae2$ghostInjecting");
        }
        nea2$field.set(this, true);

        try {
            this.injectItems(output, Actionable.MODULATE, this.machineSrc);
        } finally {
            nea2$field.set(this, false);
        }

    }

    @Mixin(targets = "appeng.me.cluster.implementations.CraftingCPUCluster$TaskProgress", remap = false)
    public interface AccessorTaskProgress {
        @Accessor
        long getValue();

        @Accessor
        void setValue(long value);
    }
}