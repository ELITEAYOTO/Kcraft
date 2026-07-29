package me.krunsh.kcraft.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.utils.NBTUtil;

/**
 * Modèle pour le résultat d'un craft
 */
public class CraftResult {
    
    private Material material;
    private short dataValue; // Data value pour blocs (ex: STONE:2 = Polished Granite)
    private int amount;
    private String name;
    private List<String> lore;
    private Map<String, Object> nbtData;
    private double chance; // Probabilité si multiples résultats
    private boolean enchanted; // Effet glow sans nom d'enchantement
    
    // Nouveaux attributs custom Kcraft
    private Map<String, Double> attributes; // attack_damage, armor, max_health, etc.
    private double durabilityModifier; // Multiplicateur de durabilité (1.0 = normal)
    // Item complet enregistre une seule fois, puis toujours clone pour la preview
    // et la remise au joueur.
    private transient ItemStack itemTemplate;
    
    public CraftResult(Material material) {
        this.material = material;
        this.dataValue = 0;
        this.amount = 1;
        this.chance = 100.0;
        this.nbtData = new HashMap<>();
        this.enchanted = false;
        this.attributes = new HashMap<>();
        this.durabilityModifier = 1.0;
    }
    
    public CraftResult(Material material, int amount) {
        this.material = material;
        this.dataValue = 0;
        this.amount = amount;
        this.chance = 100.0;
        this.nbtData = new HashMap<>();
        this.enchanted = false;
        this.attributes = new HashMap<>();
        this.durabilityModifier = 1.0;
    }
    
    /**
     * Crée l'ItemStack résultat avec tous les attributs appliqués
     */
    @SuppressWarnings("deprecation")
    public synchronized ItemStack createItem() {
        if (itemTemplate == null) {
            itemTemplate = buildItem();
        }
        return itemTemplate == null ? null : itemTemplate.clone();
    }

    private ItemStack buildItem() {
        // Créer l'item de base avec data value si spécifiée
        ItemStack item;
        if (dataValue != 0) {
            item = new ItemStack(material, amount, dataValue);
            // Appliquer nom et lore manuellement
            if (name != null || (lore != null && !lore.isEmpty())) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (name != null) {
                    meta.setDisplayName(me.krunsh.kcraft.utils.MessageUtil.colorize(name));
                }
                if (lore != null) {
                    meta.setLore(me.krunsh.kcraft.utils.MessageUtil.colorizeList(lore));
                }
                item.setItemMeta(meta);
            }
            // Appliquer NBT si présent
            if (nbtData != null && !nbtData.isEmpty()) {
                item = NBTUtil.addNBTData(item, nbtData);
            }
            // Appliquer enchantement visuel si demandé
            if (enchanted) {
                item = NBTUtil.addVisualGlow(item);
            }
        } else {
            // Créer l'item via NBTUtil (méthode standard)
            item = NBTUtil.createItemWithNBT(
                material, 
                amount, 
                name, 
                lore, 
                nbtData.isEmpty() ? null : nbtData,
                enchanted
            );
        }
        
        // Appliquer les AttributeModifiers (dégâts, armure, etc.)
        if (attributes != null && !attributes.isEmpty()) {
            item = NBTUtil.applyAttributes(item, attributes);
        }
        
        // Appliquer le modificateur de durabilité
        if (durabilityModifier != 1.0) {
            item = NBTUtil.applyDurabilityModifier(item, durabilityModifier);
        }
        
        return item;
    }
    
    /**
     * Vérifie si ce résultat doit être donné selon sa chance
     */
    public boolean shouldGive() {
        if (chance >= 100.0) {
            return true;
        }
        
        double roll = Math.random() * 100.0;
        return roll <= chance;
    }
    
    // === GETTERS & SETTERS ===
    
    public Material getMaterial() {
        return material;
    }
    
    public void setMaterial(Material material) {
        this.material = material;
        invalidateTemplate();
    }
    
    public short getDataValue() {
        return dataValue;
    }
    
    public void setDataValue(short dataValue) {
        this.dataValue = dataValue;
        invalidateTemplate();
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
        invalidateTemplate();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        invalidateTemplate();
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public void setLore(List<String> lore) {
        this.lore = lore;
        invalidateTemplate();
    }
    
    public Map<String, Object> getNbtData() {
        return nbtData;
    }
    
    public void setNbtData(Map<String, Object> nbtData) {
        this.nbtData = nbtData != null ? nbtData : new HashMap<>();
        invalidateTemplate();
    }
    
    public void addNBTData(String key, Object value) {
        this.nbtData.put(key, value);
        invalidateTemplate();
    }
    
    public double getChance() {
        return chance;
    }
    
    public void setChance(double chance) {
        this.chance = Math.max(0, Math.min(100, chance));
    }
    
    public boolean isEnchanted() {
        return enchanted;
    }
    
    public void setEnchanted(boolean enchanted) {
        this.enchanted = enchanted;
        invalidateTemplate();
    }
    
    // === NOUVEAUX GETTERS/SETTERS POUR ATTRIBUTS ===
    
    public Map<String, Double> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, Double> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
        invalidateTemplate();
    }
    
    public void addAttribute(String name, double value) {
        this.attributes.put(name, value);
        invalidateTemplate();
    }
    
    public double getDurabilityModifier() {
        return durabilityModifier;
    }
    
    public void setDurabilityModifier(double durabilityModifier) {
        this.durabilityModifier = Math.max(0.1, durabilityModifier); // Min 0.1x
        invalidateTemplate();
    }

    private void invalidateTemplate() {
        itemTemplate = null;
    }
    
    @Override
    public String toString() {
        return "CraftResult{" +
                "material=" + material +
                ", amount=" + amount +
                ", name='" + name + '\'' +
                ", chance=" + chance + "%" +
                ", nbt=" + nbtData.size() + " keys" +
                ", attributes=" + attributes.size() +
                ", durabilityMod=" + durabilityModifier +
                '}';
    }
}
