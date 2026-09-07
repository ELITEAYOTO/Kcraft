package me.krunsh.kcraft.compiled;

import java.util.Locale;

/** Resume des metadonnees precompilees, utile pour logs/diagnostics. */
public final class CompilationStats {

    private final int recipes;
    private final int shaped;
    private final int shapeless;
    private final int nbtRecipes;
    private final int identityRecipes;
    private final int occupiedSlots;
    private final int requiredItems;

    private CompilationStats(
            int recipes,
            int shaped,
            int shapeless,
            int nbtRecipes,
            int identityRecipes,
            int occupiedSlots,
            int requiredItems) {

        this.recipes = recipes;
        this.shaped = shaped;
        this.shapeless = shapeless;
        this.nbtRecipes = nbtRecipes;
        this.identityRecipes = identityRecipes;
        this.occupiedSlots = occupiedSlots;
        this.requiredItems = requiredItems;
    }

    public static CompilationStats from(
            CompiledRecipeCatalog catalog) {

        int recipes = 0;
        int shaped = 0;
        int shapeless = 0;
        int nbt = 0;
        int identity = 0;
        int slots = 0;
        int items = 0;

        for (CompiledRecipe recipe
                : catalog.recipes()) {

            recipes++;

            if (recipe.getType().name().equals("SHAPELESS")) {
                shapeless++;
            } else {
                shaped++;
            }

            if (recipe.isRequiresNbt()) {
                nbt++;
            }

            if (recipe.isRequiresCustomIdentity()) {
                identity++;
            }

            slots +=
                recipe.getOccupiedSlots();

            items +=
                recipe.getTotalRequiredItems();
        }

        return new CompilationStats(
            recipes,
            shaped,
            shapeless,
            nbt,
            identity,
            slots,
            items
        );
    }

    public String summary() {
        return "recipes="
            + recipes
            + ", shaped="
            + shaped
            + ", shapeless="
            + shapeless
            + ", nbt="
            + nbtRecipes
            + ", identity="
            + identityRecipes
            + ", occupied="
            + occupiedSlots
            + ", units="
            + requiredItems;
    }

    public int getRecipes() {
        return recipes;
    }

    public int getShaped() {
        return shaped;
    }

    public int getShapeless() {
        return shapeless;
    }

    public int getNbtRecipes() {
        return nbtRecipes;
    }

    public int getIdentityRecipes() {
        return identityRecipes;
    }

    public int getOccupiedSlots() {
        return occupiedSlots;
    }

    public int getRequiredItems() {
        return requiredItems;
    }
}
