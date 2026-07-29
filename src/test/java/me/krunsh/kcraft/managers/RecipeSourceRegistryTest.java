package me.krunsh.kcraft.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class RecipeSourceRegistryTest {
    @Test public void recordsSourceForUniqueRecipe() {
        RecipeSourceRegistry<String> registry = new RecipeSourceRegistry<String>();
        assertEquals(RecipeSourceRegistry.Registration.ACCEPTED,
            registry.register("azurite_helmet", "recipe", "armor.yml"));
        assertEquals("recipe", registry.get("azurite_helmet"));
        assertEquals("armor.yml", registry.getSource("azurite_helmet"));
        assertFalse(registry.isDuplicate("azurite_helmet"));
    }

    @Test public void duplicateIdRejectsEveryDefinitionAndReportsBothSources() {
        RecipeSourceRegistry<String> registry = new RecipeSourceRegistry<String>();
        registry.register("same", "first", "armor.yml");
        assertEquals(RecipeSourceRegistry.Registration.DUPLICATE_REJECTED,
            registry.register("same", "second", "azurite.yml"));
        assertNull(registry.get("same"));
        assertTrue(registry.isDuplicate("same"));
        assertEquals(Arrays.asList("armor.yml", "azurite.yml"), registry.getDuplicateSources("same"));
    }

    @Test public void laterDuplicatesStayRejected() {
        RecipeSourceRegistry<String> registry = new RecipeSourceRegistry<String>();
        registry.register("same", "one", "one.yml");
        registry.register("same", "two", "two.yml");
        registry.register("same", "three", "three.yml");
        assertNull(registry.get("same"));
        assertEquals(Arrays.asList("one.yml", "two.yml", "three.yml"), registry.getDuplicateSources("same"));
    }
}
