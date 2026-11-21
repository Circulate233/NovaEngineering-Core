package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.NNList;
import crazypants.enderio.base.config.recipes.xml.AbstractConditional;
import crazypants.enderio.base.recipe.RecipeLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RecipesRegistrationTest {
    @Test
    void registrationKeepsOrderLevelAndDynamicNameCallbacks() {
        List<String> calls = new ArrayList<>();
        class Recipe extends AbstractConditional {
            private final String first;
            private final String second;
            private int reads;

            Recipe(String first, String second) {
                this.first = first;
                this.second = second;
            }

            @Override
            public String getName() {
                calls.add("name:" + first);
                return reads++ % 2 == 0 ? first : second;
            }

            @Override
            public void enforceValidity() {
            }

            @Override
            public void register(String name, RecipeLevel level) {
                assertSame(RecipeLevel.IGNORE, level);
                calls.add("register:" + name);
            }
        }
        MixinRecipesRegistration registration = new MixinRecipesRegistration() {
        };
        registration.recipes = new NNList<>();
        registration.recipes.add(new Recipe("stable", "stable"));
        registration.recipes.add(new Recipe("before", "after"));
        registration.register("addon", RecipeLevel.IGNORE);
        assertEquals(List.of("name:stable", "name:stable", "register:addon:stable",
            "name:before", "name:before", "register:addon:after"), calls);
        calls.clear();
        registration.register("", RecipeLevel.IGNORE);
        assertEquals("register:stable", calls.get(2));
        assertEquals("register:after", calls.get(5));
    }
}
