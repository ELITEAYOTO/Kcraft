package me.krunsh.kcraft.matching;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Snapshot leger d'un slot de craft.
 *
 * V2.3.1 :
 * - Material/data/amount sont captures immediatement ;
 * - ItemMeta est lazy ;
 * - NBT est lazy ;
 * - aucun appel Bukkit ItemFactory n'est effectue tant qu'une recette
 *   candidate ne demande pas reellement name/lore ;
 * - un NBTItem n'est cree qu'une seule fois par slot/snapshot.
 *
 * Ce comportement est aussi plus propre pour les tests JUnit purs :
 * Bukkit.getItemFactory() n'existe pas hors serveur, mais un matching purement
 * materiau/data/amount n'en a de toute facon pas besoin.
 */
public final class ItemSnapshot {

    private static final Set<String> VANILLA_NBT_KEYS =
        vanillaNbtKeys();

    private final ItemStack source;
    private final boolean present;
    private final Material material;
    private final short data;
    private final int amount;

    private boolean metaInitialized;
    private String displayName;
    private List<String> lore;

    private NBTItem nbtItem;
    private boolean nbtInitialized;

    private Set<String> nbtKeys;

    private final Map<String, Object> nbtValueCache =
        new HashMap<String, Object>();

    private Boolean customNbt;

    private ItemSnapshot(
            ItemStack source) {

        this.source = source;

        this.present =
            source != null
                && source.getType() != Material.AIR
                && source.getAmount() > 0;

        if (!present) {
            this.material = Material.AIR;
            this.data = 0;
            this.amount = 0;
            return;
        }

        this.material =
            source.getType();

        this.data =
            source.getDurability();

        this.amount =
            source.getAmount();
    }

    public static ItemSnapshot of(
            ItemStack item) {

        return new ItemSnapshot(item);
    }

    public boolean isPresent() {
        return present;
    }

    public Material getMaterial() {
        return material;
    }

    public short getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public String getDisplayName() {
        ensureMeta();
        return displayName;
    }

    public List<String> getLore() {
        ensureMeta();
        return lore;
    }

    public boolean hasInitializedMeta() {
        return metaInitialized;
    }

    public boolean hasNbtKey(
            String key) {

        if (!present
                || key == null) {

            return false;
        }

        return nbt().hasKey(key);
    }

    public Object getNbtValue(
            String key) {

        if (!present
                || key == null) {

            return null;
        }

        if (nbtValueCache.containsKey(key)) {
            return nbtValueCache.get(key);
        }

        NBTItem item =
            nbt();

        Object value = null;

        if (item.hasKey(key)) {
            switch (item.getType(key)) {
                case NBTTagString:
                    value = item.getString(key);
                    break;

                case NBTTagInt:
                    value = item.getInteger(key);
                    break;

                case NBTTagLong:
                    value = item.getLong(key);
                    break;

                case NBTTagFloat:
                    value = item.getFloat(key);
                    break;

                case NBTTagDouble:
                    value = item.getDouble(key);
                    break;

                case NBTTagByte:
                    value = item.getByte(key);
                    break;

                case NBTTagShort:
                    value = item.getShort(key);
                    break;

                case NBTTagByteArray:
                    value = item.getByteArray(key);
                    break;

                case NBTTagIntArray:
                    value = item.getIntArray(key);
                    break;

                case NBTTagLongArray:
                    value = item.getLongArray(key);
                    break;

                default:
                    value = item.getString(key);
                    break;
            }
        }

        nbtValueCache.put(
            key,
            value
        );

        return value;
    }

    public boolean hasCustomNbt() {

        if (!present) {
            return false;
        }

        if (customNbt != null) {
            return customNbt.booleanValue();
        }

        boolean found = false;

        for (String key : nbtKeys()) {
            if (key != null
                    && !VANILLA_NBT_KEYS.contains(key)) {

                found = true;
                break;
            }
        }

        customNbt =
            Boolean.valueOf(found);

        return found;
    }

    public boolean hasInitializedNbt() {
        return nbtInitialized;
    }

    /**
     * Charge ItemMeta seulement si un matcher demande name/lore.
     *
     * En production Bukkit, getItemMeta() est disponible normalement.
     * En test JUnit pur, un ItemStack simple n'entre jamais ici si la recette
     * ne demande aucune contrainte meta.
     */
    private void ensureMeta() {

        if (metaInitialized) {
            return;
        }

        metaInitialized = true;

        if (!present) {
            return;
        }

        ItemMeta meta =
            source.getItemMeta();

        if (meta == null) {
            return;
        }

        if (meta.hasDisplayName()) {
            displayName =
                meta.getDisplayName();
        }

        if (meta.hasLore()) {
            lore =
                meta.getLore();
        }
    }

    private NBTItem nbt() {

        if (!nbtInitialized) {
            nbtItem =
                new NBTItem(source);

            nbtInitialized = true;
        }

        return nbtItem;
    }

    private Set<String> nbtKeys() {

        if (nbtKeys == null) {
            nbtKeys =
                new HashSet<String>(
                    nbt().getKeys()
                );
        }

        return nbtKeys;
    }

    private static Set<String> vanillaNbtKeys() {

        Set<String> keys =
            new HashSet<String>();

        keys.add("display");
        keys.add("ench");
        keys.add("StoredEnchantments");
        keys.add("AttributeModifiers");
        keys.add("Unbreakable");
        keys.add("HideFlags");
        keys.add("Damage");
        keys.add("RepairCost");
        keys.add("CanDestroy");
        keys.add("CanPlaceOn");
        keys.add("BlockEntityTag");
        keys.add("SkullOwner");

        return keys;
    }
}
