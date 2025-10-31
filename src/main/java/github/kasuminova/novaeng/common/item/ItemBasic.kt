package github.kasuminova.novaeng.common.item

import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng
import github.kasuminova.novaeng.common.enchantment.MagicBreaking
import github.kasuminova.novaeng.common.util.Functions
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

open class ItemBasic(name: String) : Item() {

    companion object {
        var NAMES: List<String> = Functions.asList(
            MagicBreaking.MAGICBREAKING.id + "_stone"
        )

        protected var map: MutableMap<String?, ItemBasic?> = Object2ObjectOpenHashMap<String?, ItemBasic?>()

        val allItem: List<Item>
            get() {
                val itemBasics: MutableList<Item> = ObjectArrayList()
                for (name in NAMES) {
                    val item = ItemBasic(name)
                    itemBasics.add(item)
                    map[name] = item
                }
                return itemBasics
            }

        fun getItem(name: String?): ItemBasic? {
            return map[name]
        }
    }

    init {
        this.setMaxStackSize(1)
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE)
        this.registryName = ResourceLocation(NovaEngineeringCore.MOD_ID, name)
        this.translationKey = NovaEngineeringCore.MOD_ID + '.' + name
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, world: World?, lines: MutableList<String?>, flagIn: ITooltipFlag) {
        var i = -1
        while (I18n.hasKey(this.translationKey + ".tooltip." + ++i)) {
            lines.add(I18n.format(this.translationKey + ".tooltip." + i))
        }
    }

}