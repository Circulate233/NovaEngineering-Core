package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public final class EFabricatorParallelProc extends EFabricatorPart {

    private final List<Modifier> modifiers = new ObjectArrayList<>();
    private final List<Modifier> overclockModifiers = new ObjectArrayList<>();

    public EFabricatorParallelProc() {
    }

    public EFabricatorParallelProc(final List<Modifier> modifiers, final List<Modifier> overclockModifiers) {
        this.modifiers.addAll(modifiers);
        this.overclockModifiers.addAll(overclockModifiers);
    }

    private static void readModifiers(final NBTTagCompound compound, final String name, final List<Modifier> target) {
        target.clear();
        final NBTTagList tags = compound.getTagList(name, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tags.tagCount(); i++) {
            target.add(Modifier.readFromNBT(tags.getCompoundTagAt(i)));
        }
    }

    private static NBTTagList writeModifiers(final List<Modifier> modifiers) {
        final NBTTagList tags = new NBTTagList();
        for (final Modifier modifier : modifiers) {
            tags.appendTag(modifier.writeToNBT());
        }
        return tags;
    }

    public List<Modifier> getModifiers() {
        return this.modifiers;
    }

    public List<Modifier> getOverclockModifiers() {
        return this.overclockModifiers;
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readModifiers(compound, "modifiers", this.modifiers);
        readModifiers(compound, "overclockModifiers", this.overclockModifiers);
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("modifiers", writeModifiers(this.modifiers));
        compound.setTag("overclockModifiers", writeModifiers(this.overclockModifiers));
    }

    public enum Type {
        ADD(0), MULTIPLY(1);

        private final int priority;

        Type(final int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return this.priority;
        }

        public double apply(final double value, final double parallelism) {
            return this == ADD ? value + parallelism : value * parallelism;
        }
    }

    public record Modifier(Type type, double value, boolean debuff) {
        private static final Type[] TYPES = Type.values();

        public static Modifier readFromNBT(final NBTTagCompound tag) {
            return new Modifier(TYPES[tag.getByte("type")], tag.getDouble("value"), tag.getBoolean("debuff"));
        }

        public Type getType() {
            return this.type;
        }

        public double getValue() {
            return this.value;
        }

        public boolean getDebuff() {
            return this.debuff;
        }

        public boolean isBuff() {
            return !this.debuff;
        }

        public double apply(final double parallelism) {
            return this.type.apply(this.value, parallelism);
        }

        public NBTTagCompound writeToNBT() {
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("type", (byte) this.type.ordinal());
            tag.setDouble("value", this.value);
            tag.setBoolean("debuff", this.debuff);
            return tag;
        }

        @SideOnly(Side.CLIENT)
        public String getDesc() {
            return switch (this.type) {
                case ADD -> I18n.format(this.isBuff() ? "novaeng.efabricator_parallel_proc.modifier.add"
                    : "novaeng.efabricator_parallel_proc.modifier.sub", this.isBuff() ? this.value : Math.abs(this.value));
                case MULTIPLY -> I18n.format(this.isBuff() ? "novaeng.efabricator_parallel_proc.modifier.mul"
                        : "novaeng.efabricator_parallel_proc.modifier.mul.debuff",
                    NovaEngUtils.formatDouble((this.isBuff() ? 1.0 - this.value : Math.abs(1.0 - this.value)) * 100, 1));
            };
        }
    }
}
