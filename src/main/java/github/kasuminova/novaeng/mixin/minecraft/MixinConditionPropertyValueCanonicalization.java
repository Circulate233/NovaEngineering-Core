package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import github.kasuminova.novaeng.client.model.BlockStatePredicateCache;
import github.kasuminova.novaeng.client.model.PredicateCache;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.multipart.ConditionPropertyValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;
import java.util.List;

@Mixin(ConditionPropertyValue.class)
public abstract class MixinConditionPropertyValueCanonicalization {
    @Shadow
    @Final
    private static Splitter SPLITTER;
    @Shadow
    @Final
    private String key;
    @Shadow
    @Final
    private String value;

    @Unique
    private final BlockStatePredicateCache nova$predicateCache = new BlockStatePredicateCache();

    /**
     * @author circulation
     * @reason Unwrap the Optional before the predicate lambda is allocated and cache equivalent predicates.
     */
    @Overwrite
    public Predicate<IBlockState> getPredicate(final BlockStateContainer blockState) {
        return nova$predicateCache.getOrCompute(blockState, () -> nova$createPredicate(blockState));
    }

    @Unique
    private Predicate<IBlockState> nova$createPredicate(final BlockStateContainer blockState) {
        final IProperty<?> property = blockState.getProperty(key);
        if (property == null) {
            throw new RuntimeException(this + ": Definition: " + blockState + " has no property: " + key);
        }
        String parsed = value;
        final boolean inverted = !parsed.isEmpty() && parsed.charAt(0) == '!';
        if (inverted) {
            parsed = parsed.substring(1);
        }
        final Iterator<String> values = SPLITTER.split(parsed).iterator();
        if (!values.hasNext()) {
            throw new RuntimeException(this + ": has an empty value: " + value);
        }
        final String firstValue = values.next();
        final Optional<?> firstParsedValue = property.parseValue(firstValue);
        if (!firstParsedValue.isPresent()) {
            throw new RuntimeException(this + ": has an unknown value: " + value);
        }
        Predicate<IBlockState> predicate = PredicateCache.property(property,
            (Comparable<?>) firstParsedValue.get());
        if (values.hasNext()) {
            final List<Predicate<IBlockState>> predicates = new ObjectArrayList<>();
            predicates.add(predicate);
            do {
                final Optional<?> parsedValue = property.parseValue(values.next());
                if (!parsedValue.isPresent()) {
                    throw new RuntimeException(this + ": has an unknown value: " + value);
                }
                predicates.add(PredicateCache.property(property, (Comparable<?>) parsedValue.get()));
            } while (values.hasNext());
            predicate = PredicateCache.or(predicates);
        }
        return inverted ? Predicates.not(predicate) : predicate;
    }
}
