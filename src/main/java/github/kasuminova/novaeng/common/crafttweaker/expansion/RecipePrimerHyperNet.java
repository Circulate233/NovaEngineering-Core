package github.kasuminova.novaeng.common.crafttweaker.expansion;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.hypernet.old.NetNodeCache;
import github.kasuminova.novaeng.common.hypernet.old.NetNodeImpl;
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchCognitionData;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class RecipePrimerHyperNet {

    private RecipePrimerHyperNet() {
    }

    public static RecipePrimer requireComputationPoint(final RecipePrimer recipe, final float required) {
        return requireComputationPoint(recipe, required, false);
    }

    @ZenMethod
    public static RecipePrimer requireComputationPoint(final RecipePrimer recipe, final float required,
                                                       @Optional final boolean triggerFailure) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            recipe.addRecipeTooltip(I18n.format("novaeng.hypernet.computation_point_required.tip",
                NovaEngUtils.formatFLOPS(required)).intern());
        }
        return recipe.addPreCheckHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.checkComputationPoint(event, required);
            }
        }).addStartHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.onRecipeStart(event, required);
            }
        }).addFactoryStartHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.onRecipeStart(event, required);
            }
        }).addPreTickHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.onRecipePreTick(event, required, triggerFailure);
            }
        }).addFactoryPreTickHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.onRecipePreTick(event, required, triggerFailure);
            }
        }).addFactoryFinishHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.onRecipeFinished(event.getRecipeThread());
            }
        }).addFinishHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.onRecipeFinished(event.getRecipeThread());
            }
        });
    }

    @ZenMethod
    public static RecipePrimer requireResearch(final RecipePrimer recipe, final String... names) {
        final List<ResearchCognitionData> research = new ObjectArrayList<>();
        for (final String name : names) {
            final ResearchCognitionData data = RegistryHyperNet.getResearchCognitionData(name);
            if (data != null) {
                research.add(data);
            }
        }
        return requireResearch(recipe, research.toArray(ResearchCognitionData[]::new));
    }

    @ZenMethod
    public static RecipePrimer requireResearch(final RecipePrimer recipe, final ResearchCognitionData... required) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            final String tip = Arrays.stream(required).map(ResearchCognitionData::getTranslatedName)
                                     .collect(Collectors.joining(TextFormatting.RESET + ", "));
            recipe.addRecipeTooltip(I18n.format("novaeng.hypernet.research_required.tip", tip).intern());
        }
        return recipe.addPostCheckHandler(event -> {
            final var node = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (node != null) {
                node.checkResearch(event, required);
            }
        });
    }
}
