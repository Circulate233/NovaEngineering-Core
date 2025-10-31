package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorParallelProc.Type.ADD
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorParallelProc.Type.MULTIPLY
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.util.function.Consumer
import net.minecraft.client.resources.I18n
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import kotlin.math.abs

open class EFabricatorParallelProc : EFabricatorPart {
    val modifiers: MutableList<Modifier> = ObjectArrayList<Modifier>()
    val overclockModifiers: MutableList<Modifier> = ObjectArrayList<Modifier>()

    constructor()

    constructor(modifiers: MutableList<Modifier>, overclockModifiers: MutableList<Modifier>) {
        this.modifiers.addAll(modifiers)
        this.overclockModifiers.addAll(overclockModifiers)
    }

    override fun readCustomNBT(compound: NBTTagCompound) {
        super.readCustomNBT(compound)
        modifiers.clear()
        compound.getTagList("modifiers", Constants.NBT.TAG_COMPOUND)
            .forEach(Consumer { tag: NBTBase? -> modifiers.add(Modifier.readFromNBT((tag as NBTTagCompound?)!!)) })

        overclockModifiers.clear()
        compound.getTagList("overclockModifiers", Constants.NBT.TAG_COMPOUND)
            .forEach(Consumer { tag: NBTBase? -> overclockModifiers.add(Modifier.readFromNBT((tag as NBTTagCompound?)!!)) })
    }

    override fun writeCustomNBT(compound: NBTTagCompound) {
        super.writeCustomNBT(compound)
        val modifiersTag = NBTTagList()
        modifiers.forEach(Consumer { modifier: Modifier? -> modifiersTag.appendTag(modifier!!.writeToNBT()) })
        compound.setTag("modifiers", modifiersTag)

        val overclockModifiersTag = NBTTagList()
        overclockModifiers.forEach(Consumer { modifier: Modifier? -> overclockModifiersTag.appendTag(modifier!!.writeToNBT()) })
        compound.setTag("overclockModifiers", overclockModifiersTag)
    }

    enum class Type(val priority: Int) {
        ADD(0),
        MULTIPLY(1);

        fun apply(value: Double, parallelism: Double): Double {
            return when (this) {
                ADD -> value + parallelism
                MULTIPLY -> value * parallelism
            }
        }
    }

    data class Modifier(val type: Type, val value: Double, val debuff: Boolean) {

        companion object {
            fun readFromNBT(tag: NBTTagCompound): Modifier {
                return Modifier(
                    Type.entries[tag.getByte("type").toInt()],
                    tag.getDouble("value"),
                    tag.getBoolean("debuff")
                )
            }
        }

        fun apply(parallelism: Double): Double {
            return type.apply(value, parallelism)
        }

        val isBuff: Boolean
            get() = !debuff

        fun writeToNBT(): NBTTagCompound {
            val tag = NBTTagCompound()
            tag.setByte("type", type.ordinal.toByte())
            tag.setDouble("value", value)
            tag.setBoolean("debuff", debuff)
            return tag
        }

        @get:SideOnly(Side.CLIENT)
        val desc: String
            get() {
                when (type) {
                    ADD -> {
                        return if (this.isBuff)
                            I18n.format(
                                "novaeng.efabricator_parallel_proc.modifier.add",
                                value
                            )
                        else
                            I18n.format(
                                "novaeng.efabricator_parallel_proc.modifier.sub",
                                abs(value)
                            )
                    }

                    MULTIPLY -> {
                        return if (this.isBuff)
                            I18n.format(
                                "novaeng.efabricator_parallel_proc.modifier.mul",
                                NovaEngUtils.formatDouble((1.0 - value) * 100, 1)
                            )
                        else
                            I18n.format(
                                "novaeng.efabricator_parallel_proc.modifier.mul.debuff",
                                NovaEngUtils.formatDouble(abs(1.0 - value) * 100, 1)
                            )
                    }
                }
            }
    }
}