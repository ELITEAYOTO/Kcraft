package me.krunsh.kcraft.compiled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Snapshot immutable d'un ingredient.
 *
 * Toutes les normalisations statiques sont faites au reload :
 * - nom colorise ;
 * - lore colorise ;
 * - copie NBT immutable ;
 * - specificite ;
 * - presence d'une identite custom.
 *
 * V2.3 utilisera cette representation sans relire CraftIngredient.
 */
public final class CompiledIngredient {

    private final Material material;
    private final Short dataValue;
    private final int amount;

    private final String expectedName;
    private final List<String> expectedLore;
    private final Map<String, Object> requiredNbt;

    private final boolean strictMaterial;
    private final boolean allowCustomItems;
    private final boolean identityRequired;
    private final int identitySpecificity;

    private final String materialKey;

    private CompiledIngredient(
            Material material,
            Short dataValue,
            int amount,
            String expectedName,
            List<String> expectedLore,
            Map<String, Object> requiredNbt,
            boolean strictMaterial,
            boolean allowCustomItems,
            boolean identityRequired,
            int identitySpecificity) {

        this.material = material;
        this.dataValue = dataValue;
        this.amount = Math.max(1, amount);
        this.expectedName = expectedName;

        this.expectedLore = expectedLore == null
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(
                new ArrayList<String>(expectedLore)
            );

        this.requiredNbt = requiredNbt == null
            ? Collections.<String, Object>emptyMap()
            : Collections.unmodifiableMap(
                new LinkedHashMap<String, Object>(requiredNbt)
            );

        this.strictMaterial = strictMaterial;
        this.allowCustomItems = allowCustomItems;
        this.identityRequired = identityRequired;
        this.identitySpecificity = identitySpecificity;

        this.materialKey =
            material == null ? "<null>" : material.name();
    }

    public static CompiledIngredient compile(
            CraftIngredient ingredient) {

        if (ingredient == null) {
            throw new IllegalArgumentException(
                "ingredient ne peut pas etre null."
            );
        }

        String coloredName =
            ingredient.getName() == null
                ? null
                : MessageUtil.colorize(
                    ingredient.getName()
                );

        List<String> coloredLore =
            ingredient.getLore() == null
                || ingredient.getLore().isEmpty()
                ? Collections.<String>emptyList()
                : MessageUtil.colorizeList(
                    ingredient.getLore()
                );

        return new CompiledIngredient(
            ingredient.getMaterial(),
            ingredient.getDataValue(),
            ingredient.getAmount(),
            coloredName,
            coloredLore,
            ingredient.getRequiredNBT(),
            ingredient.isStrictMaterial(),
            ingredient.isAllowCustomItems(),
            ingredient.hasIdentityRequirement(),
            ingredient.getIdentitySpecificity()
        );
    }

    public Material getMaterial() {
        return material;
    }

    public Short getDataValue() {
        return dataValue;
    }

    public int getAmount() {
        return amount;
    }

    public String getExpectedName() {
        return expectedName;
    }

    public List<String> getExpectedLore() {
        return expectedLore;
    }

    public Map<String, Object> getRequiredNbt() {
        return requiredNbt;
    }

    public boolean isStrictMaterial() {
        return strictMaterial;
    }

    public boolean isAllowCustomItems() {
        return allowCustomItems;
    }

    public boolean isIdentityRequired() {
        return identityRequired;
    }

    public int getIdentitySpecificity() {
        return identitySpecificity;
    }

    public boolean requiresNbt() {
        return !requiredNbt.isEmpty();
    }

    public boolean requiresName() {
        return expectedName != null;
    }

    public boolean requiresLore() {
        return !expectedLore.isEmpty();
    }

    /**
     * Cle tres grossiere volontairement sans NBT.
     * Utilisee plus tard pour l'index primaire des candidats.
     */
    public String getMaterialKey() {
        return materialKey;
    }
}
