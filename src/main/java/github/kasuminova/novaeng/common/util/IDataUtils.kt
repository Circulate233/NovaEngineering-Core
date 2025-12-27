package github.kasuminova.novaeng.common.util

import crafttweaker.api.data.DataMap
import crafttweaker.api.data.IData
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import stanhebben.zenscript.annotations.ZenMethod

object IDataUtils {

    @JvmStatic
    private fun IData.getNBTMap(): MutableMap<String?, IData?> {
        if (this is DataMap) {
            return this.asMap()
        }
        return Object2ObjectMaps.emptyMap<String?, IData?>()
    }

    @ZenMethod
    @JvmStatic
    fun IData.get(path: String, defaultValue: IData?): IData? {
        if (this.check(path)) {
            return this.getNBTMap()[path]
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getBool(path: String, defaultValue: Boolean): Boolean {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asBool() ?: false
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getByte(path: String, defaultValue: Byte): Byte {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asByte() ?: 0
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getDouble(path: String, defaultValue: Double): Double {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asDouble() ?: 0.0
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getFloat(path: String, defaultValue: Float): Float {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asFloat() ?: 0.0f
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getInt(path: String, defaultValue: Int): Int {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asInt() ?: 0
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getList(path: String, defaultValue: MutableList<IData?>?): MutableList<IData?>? {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asList()
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getLong(path: String, defaultValue: Long): Long {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asLong() ?: 0
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getMap(
        path: String,
        defaultValue: MutableMap<String?, IData?>?
    ): MutableMap<String?, IData?>? {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asMap()
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getShort(path: String, defaultValue: Short): Short {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asShort() ?: 0
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getString(path: String, defaultValue: String): String {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asString() ?: ""
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getByteArray(path: String, defaultValue: ByteArray?): ByteArray? {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asByteArray()
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.getIntArray(path: String, defaultValue: IntArray?): IntArray? {
        if (this.check(path)) {
            return this.getNBTMap()[path]?.asIntArray()
        }
        return defaultValue
    }

    @ZenMethod
    @JvmStatic
    fun IData.check(path: String?): Boolean {
        val nbt = this.getNBTMap()
        return nbt.containsKey(path)
    }

    @ZenMethod
    @JvmStatic
    fun IData.check(vararg path: String?): Boolean {
        val nbt = this.getNBTMap()
        for (key in path) {
            if (!nbt.containsKey(key)) {
                return false
            }
        }
        return true
    }
}