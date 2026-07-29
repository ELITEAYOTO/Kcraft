package me.krunsh.kcraft.utils;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Utilitaire NBT pour items custom style SparrowMC
 * Utilise NBT-API pour compatibilité 1.8.8
 */
public class NBTUtil {
    
    /**
     * Crée un item avec NBT custom et enchantement visuel optionnel
     */
    public static ItemStack createItemWithNBT(Material material, int amount, String name, List<String> lore, Map<String, Object> nbtData) {
        return createItemWithNBT(material, amount, name, lore, nbtData, false);
    }
    
    /**
     * Crée un item avec NBT custom et enchantement visuel optionnel
     */
    public static ItemStack createItemWithNBT(Material material, int amount, String name, List<String> lore, Map<String, Object> nbtData, boolean enchanted) {
        ItemStack item = new ItemStack(material, amount);
        
        // Meta basique
        if (name != null || (lore != null && !lore.isEmpty())) {
            ItemMeta meta = item.getItemMeta();
            if (name != null) {
                meta.setDisplayName(MessageUtil.colorize(name));
            }
            if (lore != null) {
                meta.setLore(MessageUtil.colorizeList(lore));
            }
            item.setItemMeta(meta);
        }
        
        // NBT Data et enchantement visuel
        if (((nbtData != null && !nbtData.isEmpty()) || enchanted)
                && ItemStackUtil.isPresent(item)) {
            NBTItem nbtItem = new NBTItem(item);
            
            // Ajouter les données NBT
            if (nbtData != null && !nbtData.isEmpty()) {
                for (Map.Entry<String, Object> entry : nbtData.entrySet()) {
                    setNBTValue(nbtItem, entry.getKey(), entry.getValue());
                }
            }
            
            // Ajouter enchantement visuel (glow effect)
            if (enchanted) {
                addVisualEnchantment(nbtItem);
            }
            
            item = nbtItem.getItem();
        }
        
        return item;
    }
    
    /**
     * Ajoute des données NBT à un item existant
     */
    public static ItemStack addNBTData(ItemStack item, Map<String, Object> nbtData) {
        if (!ItemStackUtil.isPresent(item) || nbtData == null || nbtData.isEmpty()) {
            return item;
        }
        
        NBTItem nbtItem = new NBTItem(item);
        
        for (Map.Entry<String, Object> entry : nbtData.entrySet()) {
            setNBTValue(nbtItem, entry.getKey(), entry.getValue());
        }
        
        return nbtItem.getItem();
    }
    
    /**
     * Obtient une valeur NBT d'un item
     */
    public static Object getNBTValue(ItemStack item, String key) {
        if (!ItemStackUtil.isPresent(item)) return null;
        
        NBTItem nbtItem = new NBTItem(item);
        
        if (!nbtItem.hasKey(key)) {
            return null;
        }
        
        // Déterminer le type et retourner la valeur appropriée
        switch (nbtItem.getType(key)) {
            case NBTTagString:
                return nbtItem.getString(key);
            case NBTTagInt:
                return nbtItem.getInteger(key);
            case NBTTagLong:
                return nbtItem.getLong(key);
            case NBTTagFloat:
                return nbtItem.getFloat(key);
            case NBTTagDouble:
                return nbtItem.getDouble(key);
            case NBTTagByte:
                return nbtItem.getByte(key);
            case NBTTagShort:
                return nbtItem.getShort(key);
            case NBTTagByteArray:
                return nbtItem.getByteArray(key);
            case NBTTagIntArray:
                return nbtItem.getIntArray(key);
            case NBTTagLongArray:
                return nbtItem.getLongArray(key);
            default:
                return nbtItem.getString(key);
        }
    }
    
    /**
     * Vérifie si un item a une clé NBT
     */
    public static boolean hasNBTKey(ItemStack item, String key) {
        if (!ItemStackUtil.isPresent(item)) return false;
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(key);
    }
    
    /**
     * Vérifie si un item a des données NBT spécifiques
     */
    public static boolean hasNBTData(ItemStack item, Map<String, Object> requiredData) {
        if (!ItemStackUtil.isPresent(item) || requiredData == null) return false;
        
        NBTItem nbtItem = new NBTItem(item);
        
        for (Map.Entry<String, Object> entry : requiredData.entrySet()) {
            String key = entry.getKey();
            Object requiredValue = entry.getValue();
            
            if (!nbtItem.hasKey(key)) {
                MessageUtil.debug("NBT key '" + key + "' non trouvée dans l'item", 3);
                return false;
            }
            
            Object actualValue = getNBTValue(item, key);
            boolean matches = NbtValueComparator.equivalent(requiredValue, actualValue);
            
            if (!matches) {
                MessageUtil.debug(
                    "NBT mismatch pour '" + key + "': requis='" + requiredValue +
                    "' (" + (requiredValue != null ? requiredValue.getClass().getSimpleName() : "null") +
                    "), actuel='" + actualValue + "' (" + (actualValue != null ? actualValue.getClass().getSimpleName() : "null") + ")",
                    3
                );
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Comparaison NBT typée. Tolère les écarts de type numérique
     * (Integer vs Long vs Byte vs Short) en ramenant les nombres a long,
     * et les decimaux a double. Les autres types comparent via equals avec
     * fallback toString pour les structures non triviales.
     */
    /**
     * Renvoie true si l'item possede au moins une cle NBT custom hors balises
     * vanilla connues (display, ench, AttributeModifiers, etc.). Sert au mode
     * strict-material qui doit refuser tout item non-vanilla.
     */
    public static boolean hasCustomNBT(ItemStack item) {
        if (!ItemStackUtil.isPresent(item)) return false;
        NBTItem nbtItem = new NBTItem(item);
        for (String key : nbtItem.getKeys()) {
            if (key == null) continue;
            switch (key) {
                case "display":
                case "ench":
                case "StoredEnchantments":
                case "AttributeModifiers":
                case "Unbreakable":
                case "HideFlags":
                case "Damage":
                case "RepairCost":
                case "CanDestroy":
                case "CanPlaceOn":
                case "BlockEntityTag":
                case "SkullOwner":
                    continue;
                default:
                    return true;
            }
        }
        return false;
    }

    /**
     * Alias pour hasNBTData - Vérifie si un item correspond aux NBT requis pour un craft
     */
    public static boolean hasMatchingNBT(ItemStack item, Map<String, Object> requiredNBT) {
        boolean result = hasNBTData(item, requiredNBT);
        
        // Debug pour voir ce qui se passe
        if (!result && requiredNBT != null && !requiredNBT.isEmpty()) {
            MessageUtil.debug("NBT debug - Item: " + (item != null ? item.getType() : "null"), 3);
            MessageUtil.debug("NBT debug - Requis: " + requiredNBT, 3);
            if (ItemStackUtil.isPresent(item)) {
                NBTItem nbtItem = new NBTItem(item);
                MessageUtil.debug("NBT debug - Keys item: " + nbtItem.getKeys(), 3);
                for (String key : requiredNBT.keySet()) {
                    MessageUtil.debug(
                        "NBT debug - Key '" + key + "': has=" + nbtItem.hasKey(key) +
                        ", value=" + getNBTValue(item, key) +
                        ", required=" + requiredNBT.get(key),
                        3
                    );
                }
            }
        }
        
        return result;
    }
    
    /**
     * Vérifie si un item est un item SparrowMC custom
     */
    public static boolean isSparrowMCItem(ItemStack item) {
        return hasNBTKey(item, "sparrowmc-item");
    }
    
    /**
     * Obtient l'ID d'un item SparrowMC
     */
    public static String getSparrowMCItemId(ItemStack item) {
        if (!isSparrowMCItem(item)) {
            return null;
        }
        return (String) getNBTValue(item, "sparrowmc-item");
    }
    
    /**
     * Crée un item SparrowMC avec ID
     */
    public static ItemStack createSparrowMCItem(Material material, int amount, String name, List<String> lore, String sparrowId, Map<String, Object> additionalNBT) {
        Map<String, Object> nbtData = new java.util.HashMap<>();
        nbtData.put("sparrowmc-item", sparrowId);
        
        if (additionalNBT != null) {
            nbtData.putAll(additionalNBT);
        }
        
        return createItemWithNBT(material, amount, name, lore, nbtData);
    }
    
    /**
     * Compare deux items par leur NBT (utile pour crafts)
     */
    public static boolean compareNBT(ItemStack item1, ItemStack item2) {
        boolean firstPresent = ItemStackUtil.isPresent(item1);
        boolean secondPresent = ItemStackUtil.isPresent(item2);
        if (!firstPresent && !secondPresent) return true;
        if (!firstPresent || !secondPresent) return false;
        
        NBTItem nbt1 = new NBTItem(item1);
        NBTItem nbt2 = new NBTItem(item2);
        
        // NBTCompound#equals effectue une comparaison structurelle, y compris
        // pour les compounds et listes imbriques, sans dependre de l'ordre de
        // serialisation de toString().
        return nbt1.equals(nbt2);
    }
    
    /**
     * Copie les données NBT d'un item vers un autre
     */
    public static ItemStack copyNBT(ItemStack source, ItemStack target) {
        if (!ItemStackUtil.isPresent(source) || !ItemStackUtil.isPresent(target)) return target;
        
        NBTItem sourceNBT = new NBTItem(source);
        NBTItem targetNBT = new NBTItem(target);
        
        // Copier toutes les clés NBT
        for (String key : sourceNBT.getKeys()) {
            Object value = getNBTValue(source, key);
            setNBTValue(targetNBT, key, value);
        }
        
        return targetNBT.getItem();
    }
    
    /**
     * Supprime une clé NBT d'un item
     */
    public static ItemStack removeNBTKey(ItemStack item, String key) {
        if (!ItemStackUtil.isPresent(item)) return item;
        
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.removeKey(key);
        
        return nbtItem.getItem();
    }
    
    /**
     * Obtient toutes les clés NBT d'un item
     */
    public static java.util.Set<String> getNBTKeys(ItemStack item) {
        if (!ItemStackUtil.isPresent(item)) return new java.util.HashSet<>();
        
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.getKeys();
    }
    
    // === MÉTHODES PRIVÉES ===
    
    private static void setNBTValue(NBTItem nbtItem, String key, Object value) {
        if (value instanceof String) {
            nbtItem.setString(key, (String) value);
        } else if (value instanceof Integer) {
            nbtItem.setInteger(key, (Integer) value);
        } else if (value instanceof Long) {
            nbtItem.setLong(key, (Long) value);
        } else if (value instanceof Float) {
            nbtItem.setFloat(key, (Float) value);
        } else if (value instanceof Double) {
            nbtItem.setDouble(key, (Double) value);
        } else if (value instanceof Byte) {
            nbtItem.setByte(key, (Byte) value);
        } else if (value instanceof Short) {
            nbtItem.setShort(key, (Short) value);
        } else if (value instanceof Boolean) {
            nbtItem.setBoolean(key, (Boolean) value);
        } else if (value instanceof byte[]) {
            nbtItem.setByteArray(key, (byte[]) value);
        } else if (value instanceof int[]) {
            nbtItem.setIntArray(key, (int[]) value);
        } else if (value instanceof long[]) {
            nbtItem.setLongArray(key, (long[]) value);
        } else {
            // Fallback vers string
            nbtItem.setString(key, value.toString());
        }
    }
    
    /**
     * Crée une table de craft custom avec NBT unique
     * @param tableId L'ID de la table
     * @param blockType Le type de bloc (ex: STONE pour Polished Granite)
     * @param dataValue La data value du bloc (ex: 2 pour Polished Granite)
     */
    @SuppressWarnings("deprecation")
    public static ItemStack createCustomCraftTable(String tableId, Material blockType, byte dataValue) {
        ItemStack table = new ItemStack(blockType, 1, dataValue);
        
        if (!ItemStackUtil.isPresent(table)) return table;
        try {
            NBTItem nbtItem = new NBTItem(table);
            nbtItem.setString("kcraft-table-id", tableId);
            nbtItem.setString("kcraft-table-type", "custom");
            nbtItem.setString("kcraft-block-type", blockType.name());
            nbtItem.setByte("kcraft-block-data", dataValue);
            nbtItem.setLong("kcraft-placed-time", System.currentTimeMillis());
            
            return nbtItem.getItem();
        } catch (Exception e) {
            return table; // Retourne table normale si erreur NBT
        }
    }
    
    /**
     * Crée une table de craft custom avec le type par défaut (WORKBENCH)
     * @deprecated Utiliser createCustomCraftTable(String, Material, byte) à la place
     */
    @Deprecated
    public static ItemStack createCustomCraftTable(String tableId) {
        return createCustomCraftTable(tableId, Material.WORKBENCH, (byte) 0);
    }
    
    /**
     * Vérifie si un item est une table de craft custom du plugin
     */
    public static boolean isCustomCraftTable(ItemStack item) {
        if (!ItemStackUtil.isPresent(item)) {
            return false;
        }
        
        try {
            NBTItem nbtItem = new NBTItem(item);
            return nbtItem.hasKey("kcraft-table-id") && 
                   "custom".equals(nbtItem.getString("kcraft-table-type"));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Récupère l'ID de table custom
     */
    public static String getCustomTableId(ItemStack item) {
        if (!isCustomCraftTable(item)) return null;
        
        try {
            NBTItem nbtItem = new NBTItem(item);
            return nbtItem.getString("kcraft-table-id");
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Vérifie si un bloc placé est une table custom (via TileEntity NBT)
     * Note: Cette méthode est obsolète pour le système actuel.
     * Le TableManager gère les blocs custom via un système de cache.
     */
    public static boolean isCustomCraftTableBlock(org.bukkit.block.Block block) {
        if (block == null) {
            return false;
        }
        
        // Pour 1.8.8, on utilise un système de cache avec les coordonnées dans TableManager
        // Car les TileEntity NBT ne sont pas disponibles sur tous les types de blocs
        return false; // Sera géré par le système de cache dans TableManager
    }
    
    /**
     * Ajoute un enchantement visuel (glow effect) sans nom d'enchantement
     */
    private static void addVisualEnchantment(NBTItem nbtItem) {
        try {
            // Ajouter un enchantement invisible pour l'effet glow
            // Utiliser un enchantement valide mais cacher les infos
            nbtItem.addCompound("ench");
            nbtItem.getCompound("ench").setShort("id", (short) 71);
            nbtItem.getCompound("ench").setShort("lvl", (short) 1);
            
            // Cacher les enchantements dans le tooltip
            nbtItem.setInteger("HideFlags", 1);
        } catch (Exception e) {
            // Fallback simple: juste cacher les enchantements
            nbtItem.setInteger("HideFlags", 63); // Cache tout
        }
    }
    
    /**
     * Ajoute un enchantement visuel à un item existant
     */
    public static ItemStack addGlowEffect(ItemStack item) {
        if (!ItemStackUtil.isPresent(item)) return item;
        
        NBTItem nbtItem = new NBTItem(item);
        addVisualEnchantment(nbtItem);
        return nbtItem.getItem();
    }
    
    /**
     * Alias pour addGlowEffect - ajoute l'effet glow visuel
     */
    public static ItemStack addVisualGlow(ItemStack item) {
        return addGlowEffect(item);
    }
    
    // ============================================================
    //          SYSTÈME D'ATTRIBUTS CUSTOM (Kcraft)
    // ============================================================
    
    /**
     * Ajoute des AttributeModifiers à un item (dégâts, armure, etc.)
     * Compatible 1.8.8/1.8.9
     * 
     * NOTE: En 1.8.8, les AttributeModifiers n'ont PAS de slot - ils s'appliquent
     * toujours quand l'item est dans l'inventaire/main. Le champ "Slot" est 1.9+.
     * 
     * @param item L'item à modifier
     * @param attributes Map des attributs (attack_damage, armor, max_health, etc.)
     * @return L'item avec les attributs appliqués
     */
    public static ItemStack applyAttributes(ItemStack item, Map<String, Double> attributes) {
        if (!ItemStackUtil.isPresent(item) || attributes == null || attributes.isEmpty()) {
            return item;
        }
        
        NBTItem nbtItem = new NBTItem(item);
        
        try {
            // Créer la liste des AttributeModifiers
            de.tr7zw.changeme.nbtapi.NBTCompoundList modifiers = nbtItem.getCompoundList("AttributeModifiers");
            
            for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                String attrName = entry.getKey();
                double value = entry.getValue();
                
                // Convertir le nom court en nom Minecraft complet
                String minecraftAttr = getMinecraftAttributeName(attrName);
                if (minecraftAttr == null) continue;
                
                // Créer le modifier
                de.tr7zw.changeme.nbtapi.NBTListCompound modifier = modifiers.addCompound();
                modifier.setString("AttributeName", minecraftAttr);
                modifier.setString("Name", "kcraft_" + attrName);
                modifier.setDouble("Amount", getAttributeAmount(attrName, value, item.getType()));
                modifier.setInteger("Operation", 0); // 0 = add, 1 = multiply base, 2 = multiply total
                
                // UUID unique et STABLE pour éviter les conflits et le stacking
                // Utiliser un UUID fixe basé sur le type d'attribut pour que ce soit reproductible
                long uuidBase = attrName.hashCode() * 31L + item.getType().hashCode();
                modifier.setLong("UUIDMost", uuidBase);
                modifier.setLong("UUIDLeast", uuidBase * 17L);
                
                // PAS de "Slot" en 1.8.8 ! Le champ Slot n'existe qu'en 1.9+
                // En 1.8, l'attribut s'applique quand l'item est tenu en main
            }
            
            return nbtItem.getItem();
            
        } catch (Exception e) {
            System.out.println("[Kcraft] Erreur lors de l'application des attributs: " + e.getMessage());
            e.printStackTrace();
            return item;
        }
    }
    
    /**
     * Convertit un nom d'attribut court en nom Minecraft complet
     */
    private static String getMinecraftAttributeName(String shortName) {
        switch (shortName.toLowerCase()) {
            case "attack_damage":
            case "damage":
                return "generic.attackDamage";
            case "armor":
            case "protection":
                return "generic.armor";
            case "armor_toughness":
            case "toughness":
                return "generic.armorToughness"; // 1.9+ mais on le garde
            case "max_health":
            case "health":
                return "generic.maxHealth";
            case "movement_speed":
            case "speed":
                return "generic.movementSpeed";
            case "knockback_resistance":
            case "knockback":
                return "generic.knockbackResistance";
            case "attack_speed":
                return "generic.attackSpeed"; // 1.9+ ignoré en 1.8
            default:
                return null;
        }
    }
    
    /**
     * Calcule la valeur correcte de l'attribut
     * Pour attack_damage en 1.8.8: la valeur donnée EST le total de dégâts voulu
     * L'AttributeModifier REMPLACE les dégâts vanilla de l'arme
     */
    private static double getAttributeAmount(String attrName, double value, Material material) {
        // En 1.8.8, l'AttributeModifier avec Operation 0 AJOUTE à la base du joueur (1.0)
        // Donc pour avoir X dégâts total, on doit mettre X - 1.0
        // PAS besoin de soustraire les dégâts vanilla car on les REMPLACE
        if (attrName.equalsIgnoreCase("attack_damage") || attrName.equalsIgnoreCase("damage")) {
            // La valeur configurée est le TOTAL voulu
            // Base du joueur = 1.0, donc on soustrait 1
            return value - 1.0;
        }
        return value;
    }
    
    /**
     * Obtient les dégâts de base d'une arme (vanilla)
     */
    private static double getBaseAttackDamage(Material material) {
        switch (material) {
            case DIAMOND_SWORD: return 7.0;
            case IRON_SWORD: return 6.0;
            case STONE_SWORD: return 5.0;
            case GOLD_SWORD: return 4.0;
            case WOOD_SWORD: return 4.0;
            case DIAMOND_AXE: return 6.0;
            case IRON_AXE: return 5.0;
            case STONE_AXE: return 4.0;
            case GOLD_AXE: return 3.0;
            case WOOD_AXE: return 3.0;
            default: return 1.0;
        }
    }
    
    /**
     * Détermine le slot approprié pour un item
     */
    private static String getSlotForItem(Material material, String attrName) {
        String name = material.name();
        
        // Armes = mainhand
        if (name.contains("SWORD") || name.contains("AXE") || name.contains("BOW")) {
            return "mainhand";
        }
        
        // Armures = slot spécifique
        if (name.contains("HELMET")) return "head";
        if (name.contains("CHESTPLATE")) return "chest";
        if (name.contains("LEGGINGS")) return "legs";
        if (name.contains("BOOTS")) return "feet";
        
        // Autres = pas de slot (toujours actif quand tenu)
        return null;
    }
    
    // ============================================================
    //          SYSTÈME DE DURABILITÉ CUSTOM (Kcraft)
    // ============================================================
    
    /** Clé NBT pour le modificateur de durabilité */
    public static final String DURABILITY_MODIFIER_KEY = "kcraft-durability-mod";
    
    /**
     * Applique un modificateur de durabilité à un item
     * 
     * @param item L'item à modifier
     * @param modifier Le multiplicateur (2.0 = 2x plus durable, 0.5 = 2x moins durable)
     * @return L'item modifié
     */
    public static ItemStack applyDurabilityModifier(ItemStack item, double modifier) {
        if (!ItemStackUtil.isPresent(item) || modifier <= 0) return item;
        
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setDouble(DURABILITY_MODIFIER_KEY, modifier);
        return nbtItem.getItem();
    }
    
    /**
     * Obtient le modificateur de durabilité d'un item
     * 
     * @param item L'item à vérifier
     * @return Le modificateur (1.0 si aucun)
     */
    public static double getDurabilityModifier(ItemStack item) {
        if (!ItemStackUtil.isPresent(item)) return 1.0;
        
        NBTItem nbtItem = new NBTItem(item);
        if (!nbtItem.hasKey(DURABILITY_MODIFIER_KEY)) {
            return 1.0;
        }
        
        return nbtItem.getDouble(DURABILITY_MODIFIER_KEY);
    }
    
    /**
     * Vérifie si un item a un modificateur de durabilité custom
     */
    public static boolean hasDurabilityModifier(ItemStack item) {
        if (!ItemStackUtil.isPresent(item)) return false;
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(DURABILITY_MODIFIER_KEY);
    }
}
