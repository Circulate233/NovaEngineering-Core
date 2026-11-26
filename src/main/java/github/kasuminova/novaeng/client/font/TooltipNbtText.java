package github.kasuminova.novaeng.client.font;

import github.kasuminova.novaeng.mixin.neofontrender.AccessorTooltipNbtCompound;
import github.kasuminova.novaeng.mixin.neofontrender.AccessorTooltipNbtList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Writes the vanilla SNBT identity into its final builder, without a string for every subtree. */
public final class TooltipNbtText {
    private static final Logger COMPOUND_LOGGER = LogManager.getLogger(NBTTagCompound.class);

    private TooltipNbtText() { }

    public static StringBuilder append(final StringBuilder output, final Object value) {
        final int start = output.length();
        try {
            appendValue(output, value);
            return output;
        } catch (final RuntimeException | LinkageError failure) {
            // StringBuilder.append(Object) evaluates toString before appending anything. Preserve
            // that boundary so NeoFont's registry-name fallback cannot follow a partial NBT value.
            output.setLength(start);
            throw failure;
        }
    }

    private static void appendValue(final StringBuilder output, final Object value) {
        if (value != null && value.getClass() == NBTTagCompound.class) {
            final NBTTagCompound compound = (NBTTagCompound) value;
            if (!(compound instanceof AccessorTooltipNbtCompound accessor)) {
                output.append(value);
                return;
            }
            final Map<String, NBTBase> backing = accessor.nova$tooltipTagMap();
            Collection<String> keys = backing.keySet();
            if (COMPOUND_LOGGER.isDebugEnabled()) {
                final ArrayList<String> sorted = new ArrayList<>(keys);
                Collections.sort(sorted);
                keys = sorted;
            }
            output.append('{');
            boolean first = true;
            for (final String key : keys) {
                if (!first) output.append(',');
                first = false;
                if (simpleKey(key)) output.append(key);
                else quoted(output, key);
                output.append(':');
                appendValue(output, backing.get(key));
            }
            output.append('}');
        } else if (value != null && value.getClass() == NBTTagList.class) {
            final NBTTagList list = (NBTTagList) value;
            if (!(list instanceof AccessorTooltipNbtList accessor)) {
                output.append(value);
                return;
            }
            final List<NBTBase> backing = accessor.nova$tooltipTagList();
            output.append('[');
            for (int i = 0; i < backing.size(); i++) {
                if (i != 0) output.append(',');
                appendValue(output, backing.get(i));
            }
            output.append(']');
        } else if (value != null && value.getClass() == NBTTagString.class) {
            quoted(output, ((NBTTagString) value).getString());
        } else {
            // Retain custom subclasses' toString implementations, including custom NBT types.
            output.append(value);
        }
    }

    private static boolean simpleKey(final String value) {
        if (value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                || c == '.' || c == '_' || c == '+' || c == '-')) return false;
        }
        return true;
    }

    private static void quoted(final StringBuilder output, final String value) {
        output.append('"');
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '\\' || c == '"') output.append('\\');
            output.append(c);
        }
        output.append('"');
    }
}
