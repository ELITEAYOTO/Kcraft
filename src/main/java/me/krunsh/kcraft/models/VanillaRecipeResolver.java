package me.krunsh.kcraft.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.utils.NBTUtil;
import me.krunsh.kcraft.utils.ItemStackUtil;

/** Source de verite de la priorite KCraft -> vanilla. */
public final class VanillaRecipeResolver {

    public enum Kind {
        CUSTOM,
        VANILLA,
        BLOCKED_CUSTOM_INPUT,
        AMBIGUOUS
    }

    public static final class Resolution {
        private final Kind kind;
        private final CraftRecipe recipe;
        private final List<String> ambiguousRecipeIds;

        private Resolution(Kind kind, CraftRecipe recipe, List<String> ambiguousRecipeIds) {
            this.kind = kind;
            this.recipe = recipe;
            this.ambiguousRecipeIds = ambiguousRecipeIds == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(ambiguousRecipeIds));
        }

        public Kind getKind() { return kind; }
        public CraftRecipe getRecipe() { return recipe; }
        public List<String> getAmbiguousRecipeIds() { return ambiguousRecipeIds; }
    }

    private VanillaRecipeResolver() {
    }

    public static Resolution resolve(Collection<CraftRecipe> recipes, ItemStack[] matrix) {
        return resolve(recipes, matrix, new Predicate<ItemStack>() {
            @Override public boolean test(ItemStack item) {
                return ItemStackUtil.isPresent(item) && NBTUtil.hasCustomNBT(item);
            }
        });
    }

    /** Surcharge injectable pour les tests purs sans serveur NMS. */
    static Resolution resolve(Collection<CraftRecipe> recipes, ItemStack[] matrix,
                              Predicate<ItemStack> customItemPredicate) {
        List<Candidate> matches = new ArrayList<Candidate>();
        if (recipes != null && matrix != null) {
            for (CraftRecipe recipe : recipes) {
                if (!RecipeAccessPolicy.fitsGrid(recipe, 3)) continue;
                if (CraftMatrixTransaction.matchesVanilla(recipe, matrix)) {
                    matches.add(new Candidate(recipe, specificity(recipe)));
                }
            }
        }

        if (!matches.isEmpty()) {
            Collections.sort(matches, new Comparator<Candidate>() {
                @Override public int compare(Candidate left, Candidate right) {
                    int score = Integer.compare(right.specificity, left.specificity);
                    return score != 0 ? score : left.recipe.getId().compareTo(right.recipe.getId());
                }
            });

            Candidate first = matches.get(0);
            List<String> top = new ArrayList<String>();
            for (Candidate candidate : matches) {
                if (candidate.specificity != first.specificity) break;
                top.add(candidate.recipe.getId());
            }
            if (top.size() > 1) {
                return new Resolution(Kind.AMBIGUOUS, null, top);
            }
            return new Resolution(Kind.CUSTOM, first.recipe, null);
        }

        if (matrix != null) {
            for (ItemStack item : matrix) {
                if (ItemStackUtil.isPresent(item) && customItemPredicate.test(item)) {
                    return new Resolution(Kind.BLOCKED_CUSTOM_INPUT, null, null);
                }
            }
        }
        return new Resolution(Kind.VANILLA, null, null);
    }

    public static int specificity(CraftRecipe recipe) {
        int score = 0;
        if (recipe == null) return score;
        if (recipe.getType() == CraftType.SHAPELESS) {
            for (CraftIngredient ingredient : recipe.getShapelessIngredients()) {
                score += ingredient.getIdentitySpecificity();
            }
            return score;
        }
        for (String row : recipe.getPattern()) {
            if (row == null) continue;
            for (int i = 0; i < row.length(); i++) {
                CraftIngredient ingredient = recipe.getIngredients().get(row.charAt(i));
                if (ingredient != null) score += ingredient.getIdentitySpecificity();
            }
        }
        return score;
    }

    /** Signature stable utilisee pour signaler les doublons au chargement. */
    public static String signature(CraftRecipe recipe) {
        if (recipe == null) return "null";
        StringBuilder out = new StringBuilder(recipe.getType().name()).append('|');
        if (recipe.getType() == CraftType.SHAPELESS) {
            List<String> ingredients = new ArrayList<String>();
            for (CraftIngredient ingredient : recipe.getShapelessIngredients()) {
                ingredients.add(ingredientSignature(ingredient));
            }
            Collections.sort(ingredients);
            out.append(ingredients);
        } else {
            out.append(recipe.getPattern()).append('|');
            Map<String, String> ingredients = new TreeMap<String, String>();
            for (Map.Entry<Character, CraftIngredient> entry : recipe.getIngredients().entrySet()) {
                ingredients.put(String.valueOf(entry.getKey()), ingredientSignature(entry.getValue()));
            }
            out.append(ingredients).append("|mirror=").append(recipe.isAllowMirror());
        }
        return out.toString();
    }

    private static String ingredientSignature(CraftIngredient ingredient) {
        if (ingredient == null) return "null";
        Map<String, Object> nbt = ingredient.getRequiredNBT() == null
            ? Collections.<String, Object>emptyMap()
            : new TreeMap<String, Object>(ingredient.getRequiredNBT());
        return ingredient.getMaterial() + ":" + ingredient.getDataValue()
            + ":" + ingredient.getAmount() + ":" + ingredient.getName()
            + ":" + ingredient.getLore() + ":" + nbt
            + ":strict=" + ingredient.isStrictMaterial()
            + ":allowCustom=" + ingredient.isAllowCustomItems();
    }

    private static final class Candidate {
        private final CraftRecipe recipe;
        private final int specificity;

        private Candidate(CraftRecipe recipe, int specificity) {
            this.recipe = recipe;
            this.specificity = specificity;
        }
    }
}
