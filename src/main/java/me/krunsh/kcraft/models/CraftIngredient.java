package me.krunsh.kcraft.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.krunsh.kcraft.utils.MessageUtil;
import me.krunsh.kcraft.utils.NBTUtil;
import me.krunsh.kcraft.utils.NbtValueComparator;
import me.krunsh.kcraft.utils.ItemStackUtil;

/**
 * Modèle pour un ingrédient de craft
 */
public class CraftIngredient {
    
    private Material material;
    private Short dataValue; // null = n'importe quelle data value
    private int amount;
    private String name;
    private List<String> lore;
    private Map<String, Object> requiredNBT;
    // Si true: refuse tout item portant des cles NBT custom (n'accepte que vanilla
    // pur). Si false (defaut): accepte tout item du materiau, sous reserve des
    // checks name/lore/nbt requis.
    private boolean strictMaterial = false;
    // Autorisation explicite pour qu'un ingredient defini seulement par son
    // materiau accepte un item portant une identite custom dans la table vanilla.
    private boolean allowCustomItems = false;
    
    public CraftIngredient(Material material) {
        this.material = material;
        this.dataValue = null;
        this.amount = 1;
        this.requiredNBT = new HashMap<>();
    }
    
    public CraftIngredient(Material material, int amount) {
        this.material = material;
        this.dataValue = null;
        this.amount = amount;
        this.requiredNBT = new HashMap<>();
    }
    
    /**
     * Vérifie si un ItemStack correspond à cet ingrédient
     */
    public boolean matches(ItemStack item) {
        if (!ItemStackUtil.isPresent(item) || item.getType() != material) {
            return false;
        }

        // Data value (1.8.8) si configurée
        if (dataValue != null && item.getDurability() != dataValue.shortValue()) {
            return false;
        }
        
        if (item.getAmount() < amount) {
            return false;
        }
        
        // Vérifier le nom si spécifié
        if (name != null) {
            ItemMeta meta = item.getItemMeta();
            String expectedName = MessageUtil.colorize(name);
            if (meta == null || !meta.hasDisplayName() || !expectedName.equals(meta.getDisplayName())) {
                return false;
            }
        }

        // Vérifier le lore si spécifié
        if (lore != null && !lore.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            List<String> expectedLore = MessageUtil.colorizeList(lore);
            if (meta == null || !meta.hasLore() || !expectedLore.equals(meta.getLore())) {
                return false;
            }
        }
        
        // Vérifier NBT si spécifié
        if (!requiredNBT.isEmpty()) {
            return NBTUtil.hasMatchingNBT(item, requiredNBT);
        }

        // Mode strict: refuse tout item portant des NBT custom (items modes)
        if (strictMaterial && NBTUtil.hasCustomNBT(item)) {
            return false;
        }

        return true;
    }

    /**
     * Variante sure pour la table vanilla. Un ingredient avec une identite
     * configuree (NBT/CIT, nom ou lore) doit la faire correspondre exactement.
     * Un ingredient materiau seul refuse les items custom, sauf opt-in explicite.
     */
    public boolean matchesForVanilla(ItemStack item, boolean recipeAllowsLooseCustomItems) {
        if (!matches(item)) return false;
        if (!hasIdentityRequirement()
                && !allowCustomItems && !recipeAllowsLooseCustomItems
                && me.krunsh.kcraft.utils.NBTUtil.hasCustomNBT(item)) {
            return false;
        }
        return true;
    }

    /** Raison exploitable par le debug, sans modifier la décision de match. */
    public String describeVanillaMismatch(ItemStack item, boolean recipeAllowsLooseCustomItems) {
        if (!ItemStackUtil.isPresent(item)) return "ITEM_ABSENT";
        if (item.getType() != material) return "MATERIAU attendu=" + material + " actuel=" + item.getType();
        if (dataValue != null && item.getDurability() != dataValue.shortValue()) {
            return "DATA attendue=" + dataValue + " actuelle=" + item.getDurability();
        }
        if (item.getAmount() < amount) return "QUANTITE attendue>=" + amount + " actuelle=" + item.getAmount();
        ItemMeta meta = item.getItemMeta();
        if (name != null) {
            String actual = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "<absent>";
            String expected = MessageUtil.colorize(name);
            if (!expected.equals(actual)) return "NOM attendu=" + expected + " actuel=" + actual;
        }
        if (lore != null && !lore.isEmpty()) {
            List<String> actual = meta != null && meta.hasLore() ? meta.getLore() : null;
            List<String> expected = MessageUtil.colorizeList(lore);
            if (!expected.equals(actual)) return "LORE attendu=" + expected + " actuel=" + actual;
        }
        for (Map.Entry<String, Object> requirement : requiredNBT.entrySet()) {
            String key = requirement.getKey();
            if (!NBTUtil.hasNBTKey(item, key)) return "NBT_CLE_ABSENTE cle=" + key;
            Object expected = requirement.getValue();
            Object actual = NBTUtil.getNBTValue(item, key);
            if (!NbtValueComparator.equivalent(expected, actual)) {
                boolean incompatibleType = expected != null && actual != null
                    && !(expected instanceof Number && actual instanceof Number)
                    && !expected.getClass().equals(actual.getClass());
                return (incompatibleType ? "NBT_TYPE" : "NBT_VALEUR") + " cle=" + key
                    + " attendue=" + typed(expected) + " actuelle=" + typed(actual);
            }
        }
        if (strictMaterial && NBTUtil.hasCustomNBT(item)) return "NBT_CUSTOM_INTERDIT_STRICT";
        if (!hasIdentityRequirement() && !allowCustomItems && !recipeAllowsLooseCustomItems
                && NBTUtil.hasCustomNBT(item)) {
            return "ITEM_CUSTOM_INTERDIT_POUR_INGREDIENT_MATERIAU_SEUL";
        }
        return matchesForVanilla(item, recipeAllowsLooseCustomItems) ? "MATCH" : "ECHEC_INCONNU";
    }

    private static String typed(Object value) {
        return value == null ? "null" : value + "(" + value.getClass().getSimpleName() + ")";
    }

    public boolean hasIdentityRequirement() {
        return (name != null && !name.isEmpty())
                || (lore != null && !lore.isEmpty())
                || (requiredNBT != null && !requiredNBT.isEmpty());
    }

    /** Score de priorite: NBT/CIT avant nom/lore, puis data. */
    public int getIdentitySpecificity() {
        int score = 0;
        if (dataValue != null) score += 1;
        if (name != null && !name.isEmpty()) score += 20;
        if (lore != null && !lore.isEmpty()) score += 20;
        if (requiredNBT != null) score += requiredNBT.size() * 100;
        if (strictMaterial) score += 2;
        return score;
    }

    public boolean isStrictMaterial() {
        return strictMaterial;
    }

    public void setStrictMaterial(boolean strictMaterial) {
        this.strictMaterial = strictMaterial;
    }

    public boolean isAllowCustomItems() {
        return allowCustomItems;
    }

    public void setAllowCustomItems(boolean allowCustomItems) {
        this.allowCustomItems = allowCustomItems;
    }
    
    /**
     * Consomme les items nécessaires du slot
     */
    public ItemStack consume(ItemStack item) {
        if (!matches(item)) {
            return item;
        }
        
        int newAmount = item.getAmount() - amount;
        if (newAmount <= 0) {
            return null;
        }
        
        ItemStack result = item.clone();
        result.setAmount(newAmount);
        return result;
    }
    
    // === GETTERS & SETTERS ===
    
    public Material getMaterial() {
        return material;
    }
    
    public void setMaterial(Material material) {
        this.material = material;
    }

    public Short getDataValue() {
        return dataValue;
    }

    public void setDataValue(Short dataValue) {
        this.dataValue = dataValue;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public void setLore(List<String> lore) {
        this.lore = lore;
    }
    
    public Map<String, Object> getRequiredNBT() {
        return requiredNBT;
    }
    
    public void setRequiredNBT(Map<String, Object> requiredNBT) {
        this.requiredNBT = requiredNBT != null ? requiredNBT : new HashMap<>();
    }
    
    public void addNBTRequirement(String key, Object value) {
        this.requiredNBT.put(key, value);
    }
    
    @Override
    public String toString() {
        return "CraftIngredient{" +
                "material=" + material +
                ", amount=" + amount +
                ", name='" + name + '\'' +
                ", nbt=" + requiredNBT.size() + " keys" +
                '}';
    }
}
