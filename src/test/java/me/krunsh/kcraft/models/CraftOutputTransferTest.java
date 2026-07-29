package me.krunsh.kcraft.models;

import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CraftOutputTransferTest {

    @Test
    public void normalClickPlacesExactOutputOnCursor() {
        CraftRecipe recipe = singleStoneRecipe();
        CraftOutputTransfer.Plan plan = CraftOutputTransfer.planOnce(recipe,
            matrix(1), new ItemStack(Material.DIAMOND_HELMET),
            CraftOutputTransfer.Destination.CURSOR, null, new ItemStack[36], -1);
        assertNotNull(plan);
        assertNull(plan.getMatrix()[0]);
        assertEquals(Material.DIAMOND_HELMET, plan.getCursor().getType());
    }

    @Test
    public void shiftClickWithAlmostFullInventoryConsumesOnlyWhenOutputFits() {
        ItemStack[] storage = fullStorage();
        storage[35] = new ItemStack(Material.DIAMOND, 63);
        CraftOutputTransfer.Plan plan = CraftOutputTransfer.planOnce(singleStoneRecipe(),
            matrix(1), new ItemStack(Material.DIAMOND),
            CraftOutputTransfer.Destination.INVENTORY, null, storage, -1);
        assertNotNull(plan);
        assertEquals(64, plan.getStorage()[35].getAmount());

        assertNull(CraftOutputTransfer.planOnce(singleStoneRecipe(), matrix(1),
            new ItemStack(Material.EMERALD), CraftOutputTransfer.Destination.INVENTORY,
            null, fullStorage(), -1));
    }

    @Test
    public void hotbarNumberKeyAndSuccessiveCraftsRemainAtomic() {
        CraftRecipe recipe = singleStoneRecipe();
        ItemStack[] storage = new ItemStack[36];
        CraftOutputTransfer.Plan first = CraftOutputTransfer.planOnce(recipe, matrix(2),
            new ItemStack(Material.DIAMOND), CraftOutputTransfer.Destination.HOTBAR,
            null, storage, 4);
        assertNotNull(first);
        assertEquals(1, first.getMatrix()[0].getAmount());
        assertEquals(1, first.getStorage()[4].getAmount());

        CraftOutputTransfer.Plan second = CraftOutputTransfer.planOnce(recipe, first.getMatrix(),
            new ItemStack(Material.DIAMOND), CraftOutputTransfer.Destination.HOTBAR,
            first.getCursor(), first.getStorage(), 4);
        assertNotNull(second);
        assertNull(second.getMatrix()[0]);
        assertEquals(2, second.getStorage()[4].getAmount());
    }

    @Test
    public void hotbarKeyMovesDisplacedStackWithoutLosingOrDuplicatingIt() {
        ItemStack[] storage = new ItemStack[36];
        storage[4] = new ItemStack(Material.EMERALD, 32);

        CraftOutputTransfer.Plan plan = CraftOutputTransfer.planOnce(singleStoneRecipe(),
            matrix(1), new ItemStack(Material.DIAMOND),
            CraftOutputTransfer.Destination.HOTBAR, null, storage, 4);

        assertNotNull(plan);
        assertNull(plan.getMatrix()[0]);
        assertEquals(Material.DIAMOND, plan.getStorage()[4].getType());
        assertEquals(Material.EMERALD, plan.getStorage()[0].getType());
        assertEquals(32, plan.getStorage()[0].getAmount());

        ItemStack[] full = fullStorage();
        full[4] = new ItemStack(Material.EMERALD, 32);
        assertNull(CraftOutputTransfer.planOnce(singleStoneRecipe(), matrix(1),
            new ItemStack(Material.DIAMOND), CraftOutputTransfer.Destination.HOTBAR,
            null, full, 4));
    }

    @Test
    public void mirroredRecipeMatchesOnlyWhenEnabled() {
        CraftRecipe recipe = new CraftRecipe("mirror");
        recipe.setPattern(Arrays.asList("AB"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('A', vanilla(Material.STONE));
        ingredients.put('B', vanilla(Material.DIRT));
        recipe.setIngredients(ingredients);

        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = new ItemStack(Material.DIRT);
        matrix[1] = new ItemStack(Material.STONE);
        assertNull(CraftMatrixTransaction.consumeOnceVanilla(recipe, matrix));
        recipe.setAllowMirror(true);
        assertNotNull(CraftMatrixTransaction.consumeOnceVanilla(recipe, matrix));
    }

    @Test
    public void craftResultAlwaysReturnsIndependentFullClone() {
        CraftResult result = new CraftResult(Material.WOOL, 2);
        result.setDataValue((short) 14);
        ItemStack preview = result.createItem();
        ItemStack granted = result.createItem();
        preview.setAmount(1);
        assertEquals(2, granted.getAmount());
        assertEquals(14, granted.getDurability());
    }

    private static CraftRecipe singleStoneRecipe() {
        CraftRecipe recipe = new CraftRecipe("stone");
        recipe.setPattern(Arrays.asList("A"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('A', vanilla(Material.STONE));
        recipe.setIngredients(ingredients);
        return recipe;
    }

    private static ItemStack[] matrix(int amount) {
        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = new ItemStack(Material.STONE, amount);
        return matrix;
    }

    private static ItemStack[] fullStorage() {
        ItemStack[] storage = new ItemStack[36];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = new ItemStack(Material.COBBLESTONE, 64);
        }
        return storage;
    }

    private static CraftIngredient vanilla(Material material) {
        return new CraftIngredient(material) {
            @Override public boolean matchesForVanilla(ItemStack item, boolean loose) {
                return matches(item);
            }
        };
    }
}
