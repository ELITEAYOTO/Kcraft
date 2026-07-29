# Guide Debug NBT - Kcraft
# ========================

## System de Debug Implementé

### 1. NBTUtil.java - Debug amélioré
- `hasNBTData()` avec log détaillé des comparaisons NBT
- Affiche les NBT exacts des items et patterns attendus
- Compare en mode string pour éviter les problèmes de type

### 2. CraftManager.java - Debug matching
- `hasMatchingNBT()` avec logs détaillés de correspondance
- Affiche chaque comparaison item vs pattern
- Identifie précisément où le matching échoue

## Architecture du Système

### Table Unique: tier1 (16 slots / 4x4)
- **Tous** les crafts custom utilisent cette table
- Patterns 3x3 placés flexiblement dans grille 4x4
- Position flexible via l'algorithme de matching

### Catégories d'Items NBT

#### OutilsEvolutif (/ge commands)
- pioche_bois_t1/t2/t3, hache_bois_t1/t2/t3, etc.
- NBT: sparrowmc-item pour identification
- Craft avec commande d'évolution automatique

#### Cultures T1-T3 (compacting.yml)
- wheat/carrot/potato/nether_wart/melon_slice/pumpkin/cocoa_beans
- Progression: T1 → T2 → T3 (9x compacting)
- NBT: sparrowmc-item pour différencier les tiers

#### Mob Drops T1-T3 (mob_drops.yml)
- string/rotten_flesh/bone/ender_pearl/leather/blaze_rod
- Même système de progression T1→T2→T3
- NBT: sparrowmc-item pour identification

#### Azurite Equipment
- Lingots, armes, armures azurite
- NBT: sparrowmc-item pour items custom
- Table tier1 pour tous les crafts

## Test Procédure

1. **Vérifier les logs de debug**:
   - `/craftdebug on` pour activer debug détaillé
   - Regarder console pour comparaisons NBT

2. **Tester craft simple**:
   - Obtenir 9x wheat_t1 via `/ge wheat_t1 9`
   - Placer dans pattern 3x3 sur table tier1
   - Vérifier logs NBT dans console

3. **Identifier problèmes**:
   - Si "NBT comparison failed" → NBT ne match pas
   - Si "Pattern doesn't match" → Problème de pattern
   - Si "Wrong table" → Vérifier table tier1

## Files Modifiés
- `compacting.yml`: Patterns 3x3, melon/pumpkin/cocoa T1-T3
- `mob_drops.yml`: Restructuré hunter.yml avec T1-T3 complet  
- `azurite.yml`: Tous crafts sur tier1
- `NBTUtil.java`: Debug comparaison amélioré
- `CraftManager.java`: Debug matching détaillé

## Next Steps
1. Tester avec items `/ge` in-game
2. Analyser logs de debug console
3. Ajuster NBT matching si nécessaire
4. Valider tous les crafts custom fonctionnent