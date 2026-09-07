package me.krunsh.kcraft.gui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import me.krunsh.kcraft.models.CraftRecipe;

public class PreviewSelectionTest {

    @Test
    public void selectionMatchesOnlySameRecipeId() {
        PreviewSelection selection =
            new PreviewSelection(
                "random_reward",
                null
            );

        CraftRecipe same =
            new CraftRecipe(
                "random_reward"
            );

        CraftRecipe other =
            new CraftRecipe(
                "other"
            );

        assertTrue(
            selection.matches(same)
        );

        assertFalse(
            selection.matches(other)
        );

        assertNull(
            selection.getResult()
        );
    }
}
