package me.krunsh.kcraft.models;

/**
 * Types de crafts supportés
 */
public enum CraftType {
    
    /**
     * Craft avec pattern fixe (comme vanilla)
     */
    SHAPED,
    
    /**
     * Craft sans pattern (ingrédients dans n'importe quel ordre)
     */
    SHAPELESS,
    
    /**
     * Craft de compactage (optimisé pour performance)
     */
    COMPACTING,
    
    /**
     * Craft spécial avec logique custom
     */
    SPECIAL;
    
    /**
     * Parse depuis string de config
     */
    public static CraftType fromString(String str) {
        if (str == null) return SHAPED;
        
        try {
            return CraftType.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SHAPED;
        }
    }
}