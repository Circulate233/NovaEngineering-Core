package github.kasuminova.novaeng.common.item.estorage;

import ae2.api.storage.cells.IBasicCellItem;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import ae2.api.config.FuzzyMode;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.items.AEBaseItem;
import ae2.items.contents.CellConfig;
import ae2.util.ConfigInventory;
import ae2.util.Platform;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import lombok.Getter;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public abstract class EStorageCell extends AEBaseItem implements IBasicCellItem {
    @Getter
    protected final DriveStorageLevel level;
    protected final int totalBytes;
    @Getter
    protected final int byteMultiplier;

    public EStorageCell(DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        this.level = level;
        this.totalBytes = (millionBytes * 1000) * 1024;
        this.byteMultiplier = byteMultiplier;
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addCheckedInformation(final ItemStack stack, final World world, final List<String> lines, final ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);
        addCellInformationToTooltip(stack, lines);
        lines.add(I18n.format("novaeng.estorage_cell.insert.tip"));
        lines.add(I18n.format("novaeng.estorage_cell.extract.tip"));
        if (level == DriveStorageLevel.B) {
            lines.add(I18n.format("novaeng.estorage_cell.l6.tip"));
        }
        if (level == DriveStorageLevel.C) {
            lines.add(I18n.format("novaeng.estorage_cell.l9.tip"));
        }
    }

    @Override
    public double getIdleDrain() {
        return (double) totalBytes / 1024 / 1024;
    }

    @Override
    public int getBytes(@Nonnull final ItemStack cellItem) {
        return totalBytes;
    }

    @Override
    public boolean isBlackListed(@Nonnull final ItemStack cellItem, @Nonnull final AEKey requestedAddition) {
        return false;
    }

    @Nonnull
    @Override
    public abstract AEKeyType getKeyType();

    @Override
    public IUpgradeInventory getUpgrades(final ItemStack is) {
        return UpgradeInventories.forItem(is, getKeyType() == AEKeyType.items() ? 4 : 3);
    }

    @Override
    public ConfigInventory getConfigInventory(final ItemStack is) {
        return CellConfig.create(Set.of(getKeyType()), is, getTotalTypes(is));
    }

    @Override
    public FuzzyMode getFuzzyMode(final ItemStack is) {
        final NBTTagCompound tag = is.getTagCompound();
        if (tag == null || !tag.hasKey("FuzzyMode", 8)) {
            return FuzzyMode.IGNORE_ALL;
        }
        return FuzzyMode.valueOf(tag.getString("FuzzyMode"));
    }

    @Override
    public void setFuzzyMode(final ItemStack is, final FuzzyMode fzMode) {
        Platform.openNbtData(is).setString("FuzzyMode", fzMode.name());
    }
}
