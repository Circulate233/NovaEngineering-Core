package github.kasuminova.novaeng.common.item

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng
import github.kasuminova.novaeng.common.enchantment.MagicBreaking
import github.kasuminova.novaeng.common.util.Functions
import github.kasuminova.novaeng.novaeng_core.Tags.MOD_ID
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

open class ItemBasic @JvmOverloads constructor(
    name: String,
    private val maxStack: Int = 1,
    private val glint: Boolean = false,
    private val beaconPayment: Boolean = false
) : Item() {

    companion object {
        var NAMES: List<String> = Functions.asList(
            MagicBreaking.MAGICBREAKING.id + "_stone"
        )

        protected var map: MutableMap<String?, ItemBasic?> = Object2ObjectOpenHashMap<String?, ItemBasic?>()

        private val declared = ObjectArrayList<Declared>()

        private var cachedItems: List<Item>? = null

        /**
         * 声明一个由本类承载的物品，需在 [github.kasuminova.novaeng.common.registry.RegistryItems]
         * 处理注册事件之前调用。
         */
        @JvmStatic
        @JvmOverloads
        fun declare(name: String, maxStack: Int = 64, glint: Boolean = false, beaconPayment: Boolean = false) {
            declared.add(Declared(name, maxStack, glint, beaconPayment))
        }
        val allItem: List<Item>
            get() {
                var items = cachedItems
                if (items == null) {
                    val itemBasics: MutableList<Item> = ObjectArrayList()
                    for (name in NAMES) {
                        val item = ItemBasic(name)
                        itemBasics.add(item)
                        map[name] = item
                    }
                    for (spec in declared) {
                        val item = ItemBasic(spec.name, spec.maxStack, spec.glint, spec.beaconPayment)
                        itemBasics.add(item)
                        map[spec.name] = item
                    }
                    items = itemBasics
                    cachedItems = itemBasics
                }
                return items
            }

        fun getItem(name: String?): ItemBasic? {
            allItem
            return map[name]
        }
    }

    init {
        this.setMaxStackSize(maxStack)
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE)
        this.registryName = ResourceLocation(MOD_ID, name)
        this.translationKey = MOD_ID + '.' + name
    }

    override fun hasEffect(stack: ItemStack): Boolean = glint

    override fun isBeaconPayment(stack: ItemStack): Boolean = beaconPayment

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, world: World?, lines: MutableList<String?>, flagIn: ITooltipFlag) {
        var i = -1
        while (I18n.hasKey(this.translationKey + ".tooltip." + ++i)) {
            lines.add(I18n.format(this.translationKey + ".tooltip." + i))
        }
    }

    private data class Declared(
        val name: String,
        val maxStack: Int,
        val glint: Boolean,
        val beaconPayment: Boolean
    )

}
