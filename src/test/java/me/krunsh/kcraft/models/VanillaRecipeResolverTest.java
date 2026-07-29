package me.krunsh.kcraft.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class VanillaRecipeResolverTest {

    @Test
    public void exactCitRecipeBeatsMaterialOnlyRecipe() {
        CraftRecipe loose = recipe("loose", "A", generic(Material.DIAMOND));
        CraftRecipe azurite = recipe("azurite", "A", custom(Material.DIAMOND, (short) 7));
        ItemStack[] matrix = matrix(new ItemStack(Material.DIAMOND, 1, (short) 7));

        VanillaRecipeResolver.Resolution result = resolve(Arrays.asList(loose, azurite), matrix);
        assertEquals(VanillaRecipeResolver.Kind.CUSTOM, result.getKind());
        assertEquals("azurite", result.getRecipe().getId());
    }

    @Test
    public void vanillaMaterialWithoutCitRemainsVanilla() {
        CraftRecipe azurite = recipe("azurite", "A", custom(Material.DIAMOND, (short) 7));
        VanillaRecipeResolver.Resolution result = resolve(
            Arrays.asList(azurite), matrix(new ItemStack(Material.DIAMOND)));
        assertEquals(VanillaRecipeResolver.Kind.VANILLA, result.getKind());
    }

    @Test
    public void wrongCitAndUnmatchedCustomItemBlockVanillaFallback() {
        CraftRecipe azurite = recipe("azurite", "A", custom(Material.DIAMOND, (short) 7));
        VanillaRecipeResolver.Resolution result = resolve(
            Arrays.asList(azurite), matrix(new ItemStack(Material.DIAMOND, 1, (short) 8)));
        assertEquals(VanillaRecipeResolver.Kind.BLOCKED_CUSTOM_INPUT, result.getKind());
    }

    @Test
    public void explicitMixedRecipeAcceptsCustomAndVanillaSlots() {
        CraftRecipe recipe = new CraftRecipe("mixed");
        recipe.setPattern(Arrays.asList("AB"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('A', custom(Material.DIAMOND, (short) 7));
        ingredients.put('B', generic(Material.STICK));
        recipe.setIngredients(ingredients);

        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = new ItemStack(Material.DIAMOND, 1, (short) 7);
        matrix[1] = new ItemStack(Material.STICK);
        assertEquals(VanillaRecipeResolver.Kind.CUSTOM,
            resolve(Arrays.asList(recipe), matrix).getKind());

        matrix[1] = new ItemStack(Material.STICK, 1, (short) 9);
        assertEquals(VanillaRecipeResolver.Kind.BLOCKED_CUSTOM_INPUT,
            resolve(Arrays.asList(recipe), matrix).getKind());
    }

    @Test
    public void twoCitRecipesWithSameMaterialSelectOnlyExactIdentity() {
        CraftRecipe first = recipe("azurite", "A", custom(Material.DIAMOND, (short) 7));
        CraftRecipe second = recipe("jaspe", "A", custom(Material.DIAMOND, (short) 8));
        VanillaRecipeResolver.Resolution result = resolve(Arrays.asList(first, second),
            matrix(new ItemStack(Material.DIAMOND, 1, (short) 8)));
        assertEquals("jaspe", result.getRecipe().getId());
    }

    @Test
    public void equalPriorityMatchesAreRefusedAsAmbiguous() {
        CraftRecipe first = recipe("first", "A", custom(Material.DIAMOND, (short) 7));
        CraftRecipe second = recipe("second", "A", custom(Material.DIAMOND, (short) 7));
        VanillaRecipeResolver.Resolution result = resolve(Arrays.asList(first, second),
            matrix(new ItemStack(Material.DIAMOND, 1, (short) 7)));
        assertEquals(VanillaRecipeResolver.Kind.AMBIGUOUS, result.getKind());
        assertEquals(Arrays.asList("first", "second"), result.getAmbiguousRecipeIds());
    }

    @Test
    public void fullAzuriteChestplatePatternWinsOverVanillaArmorShape() {
        CraftRecipe chestplate = new CraftRecipe("azurite_chestplate");
        chestplate.setPattern(Arrays.asList("D D", "DDD", "DDD"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('D', custom(Material.DIAMOND, (short) 7));
        chestplate.setIngredients(ingredients);

        ItemStack[] matrix = new ItemStack[9];
        int[] occupied = {0, 2, 3, 4, 5, 6, 7, 8};
        for (int slot : occupied) matrix[slot] = new ItemStack(Material.DIAMOND, 1, (short) 7);

        VanillaRecipeResolver.Resolution resolution = resolve(Arrays.asList(chestplate), matrix);
        assertEquals(VanillaRecipeResolver.Kind.CUSTOM, resolution.getKind());
        assertEquals("azurite_chestplate", resolution.getRecipe().getId());

        ItemStack[] consumed = CraftMatrixTransaction.consumeOnceVanilla(chestplate, matrix);
        assertNotNull(consumed);
        for (int slot : occupied) assertNull(consumed[slot]);
        assertNull(consumed[1]);
    }

    @Test
    public void consecutiveCustomCraftsReResolveEveryRemainingIngredient() {
        CraftRecipe chestplate = new CraftRecipe("azurite_chestplate");
        chestplate.setPattern(Arrays.asList("D D", "DDD", "DDD"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('D', custom(Material.DIAMOND, (short) 7));
        chestplate.setIngredients(ingredients);

        ItemStack[] matrix = new ItemStack[9];
        int[] occupied = {0, 2, 3, 4, 5, 6, 7, 8};
        for (int slot : occupied) {
            matrix[slot] = new ItemStack(Material.DIAMOND, 2, (short) 7);
        }

        VanillaRecipeResolver.Resolution first = resolve(Arrays.asList(chestplate), matrix);
        assertEquals(VanillaRecipeResolver.Kind.CUSTOM, first.getKind());
        ItemStack[] afterFirst = CraftMatrixTransaction.consumeOnceVanilla(
            first.getRecipe(), matrix);
        assertNotNull(afterFirst);
        for (int slot : occupied) {
            assertEquals(2, matrix[slot].getAmount());
            assertEquals(1, afterFirst[slot].getAmount());
            assertEquals((short) 7, afterFirst[slot].getDurability());
        }

        VanillaRecipeResolver.Resolution second = resolve(
            Arrays.asList(chestplate), afterFirst);
        assertEquals(VanillaRecipeResolver.Kind.CUSTOM, second.getKind());
        ItemStack[] afterSecond = CraftMatrixTransaction.consumeOnceVanilla(
            second.getRecipe(), afterFirst);
        assertNotNull(afterSecond);
        for (int slot : occupied) assertNull(afterSecond[slot]);

        assertEquals(VanillaRecipeResolver.Kind.VANILLA,
            resolve(Arrays.asList(chestplate), afterSecond).getKind());
    }

    @Test
    public void everyEmptySlotRepresentationRemainsVanillaWithoutNbtAccess() {
        ItemStack[] matrix = new ItemStack[9];
        matrix[1] = new ItemStack(Material.AIR, 1);
        matrix[5] = new ItemStack(Material.DIAMOND, 0);

        VanillaRecipeResolver.Resolution resolution = VanillaRecipeResolver.resolve(
            java.util.Collections.<CraftRecipe>emptyList(), matrix);

        assertEquals(VanillaRecipeResolver.Kind.VANILLA, resolution.getKind());
    }

    private static VanillaRecipeResolver.Resolution resolve(
            java.util.Collection<CraftRecipe> recipes, ItemStack[] matrix) {
        return VanillaRecipeResolver.resolve(recipes, matrix, new Predicate<ItemStack>() {
            @Override public boolean test(ItemStack item) {
                return item != null && item.getDurability() > 0;
            }
        });
    }

    private static CraftRecipe recipe(String id, String pattern, CraftIngredient ingredient) {
        CraftRecipe recipe = new CraftRecipe(id);
        recipe.setPattern(Arrays.asList(pattern));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('A', ingredient);
        recipe.setIngredients(ingredients);
        return recipe;
    }

    private static CraftIngredient generic(Material material) {
        return new FakeIdentityIngredient(material, null);
    }

    private static CraftIngredient custom(Material material, short identity) {
        return new FakeIdentityIngredient(material, identity);
    }

    private static ItemStack[] matrix(ItemStack item) {
        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = item;
        return matrix;
    }

    private static final class FakeIdentityIngredient extends CraftIngredient {
        private final Short identity;

        private FakeIdentityIngredient(Material material, Short identity) {
            super(material, 1);
            this.identity = identity;
        }

        @Override public boolean matches(ItemStack item) {
            return item != null && item.getType() == getMaterial()
                && (identity == null || item.getDurability() == identity.shortValue());
        }

        @Override public boolean matchesForVanilla(ItemStack item, boolean loose) {
            if (!matches(item)) return false;
            return identity != null || item.getDurability() == 0 || loose;
        }

        @Override public int getIdentitySpecificity() {
            return identity == null ? 0 : 100;
        }
    }
}
