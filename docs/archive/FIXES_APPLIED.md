# CORRECTIFS APPLIQUÉS - Kcraft Plugin

## 🔧 PROBLÈMES RÉSOLUS

### 1. ❌ OutilsEvolutif - Outils non donnés correctement
**Symptôme**: Log `[OutilsEvolutif] Usage depuis la console : /giveoutil <type> <niveau> [joueur]`
- La commande était exécutée depuis la console sans le paramètre `[joueur]`
- OutilsEvolutif nécessite que la commande soit exécutée PAR le joueur, pas depuis la console

**Solution**:
```yaml
# AVANT (console-command: true)
command: "giveoutil hoe 1 {player_name}"
console-command: true

# APRÈS (console-command: false - joueur exécute)
command: "giveoutil hoe 1"
console-command: false
```

**Fichiers modifiés**:
- `evolutive_tools.yml` - 4 crafts (hoe, axe, pickaxe, fishing_rod)
- `hunter.yml` - 1 craft (hunter_legendary_sword)

---

### 2. ❌ Kharvester Collectors - Items invisibles
**Symptôme**: Craft détecté, barrière disparaît, mais aucun item reçu
- Utilisait `command-on-success: []` qui n'est PAS implémenté dans le code
- Le système reconnaît seulement `command` dans `on-success`

**Solution**:
```yaml
# AVANT (clé non reconnue)
result:
  material: "AIR"
command-on-success:
  - "khgive {player} collector_stone 1"

# APRÈS (clé correcte)
result:
  material: "AIR"
on-success:
  command: "khgive {player_name} collector_stone 1"
  console-command: true
```

**Fichiers modifiés**:
- `collectors.yml` - 3 crafts (stone, iron, diamond)

---

### 3. ❌ Décraft impossible - Items NBT sans recette inverse
**Symptôme**: 
```
DEBUG: NBT key 'sparrowmc-item' not found in item
DEBUG NBT - Item: STRING
DEBUG NBT - Required: {sparrowmc-item=string_t1}
```
- Les items T1/T2/T3 créés via crafts ont le NBT `sparrowmc-item`
- Aucune recette de décraft n'existait pour revenir en arrière

**Solution**: Ajout de recettes SHAPELESS de décompactage pour TOUS les items:

#### Hunter.yml (Mob Drops)
- `string_t3_to_t2` → 9x T2
- `string_t2_to_t1` → 9x T1
- `string_t1_to_vanilla` → 9x Vanilla
- `rotten_flesh_t3_to_vanilla` → 16x Vanilla
- `blaze_rod_t3_to_vanilla` → 16x Vanilla
- `bone_t3_to_vanilla` → 16x Vanilla
- `ender_pearl_t3_to_vanilla` → 16x Vanilla
- `leather_t3_to_vanilla` → 16x Vanilla

#### Compacting.yml (Cultures)
- **Blé**: `wheat_t3_to_t2` (9x T2), `wheat_t2_to_t1` (9x T1), `wheat_t1_to_vanilla` (9x)
- **Carotte**: `carrot_t3_to_t2` (16x T2), `carrot_t2_to_t1` (9x T1), `carrot_t1_to_vanilla` (9x)
- **Pomme de terre**: `potato_t3_to_t2` (16x T2), `potato_t2_to_t1` (16x T1), `potato_t1_to_vanilla` (16x)
- **Verrue du Nether**: `nether_wart_t3_to_t2` (16x T2), `nether_wart_t2_to_t1` (16x T1), `nether_wart_t1_to_vanilla` (16x)
- **Pastèque**: `watermelon_t3_to_t2` (9x T2), `watermelon_t2_to_t1` (9x T1), `watermelon_t1_to_vanilla` (9x)
- **Citrouille**: `pumpkin_t3_to_t2` (9x T2), `pumpkin_t2_to_t1` (9x T1), `pumpkin_t1_to_vanilla` (9x)
- **Cacao**: `cocoa_t3_to_t2` (9x T2), `cocoa_t2_to_t1` (9x T1), `cocoa_t1_to_vanilla` (9x)

**Total**: 30 nouvelles recettes de décraft ajoutées

---

## 📊 RÉSUMÉ DES MODIFICATIONS

| Fichier | Lignes modifiées | Nouvelles recettes |
|---------|------------------|-------------------|
| `evolutive_tools.yml` | ~16 lignes | 0 (corrections) |
| `hunter.yml` | ~120 lignes | 8 décrafts |
| `collectors.yml` | ~21 lignes | 0 (corrections) |
| `compacting.yml` | ~280 lignes | 22 décrafts |

**Total**: 30 nouvelles recettes, ~437 lignes modifiées

---

## ✅ VALIDATION

### Tests à effectuer:

1. **OutilsEvolutif**:
   ```
   /kcraft give Krunsh_ evolutive_hoe 1
   → Doit recevoir houe OutilsEvolutif niveau 1
   ```

2. **Kharvester Collectors**:
   ```
   Craft: 8x Iron Ingot + 1x Hopper
   → Doit recevoir vrai collector Kharvester
   ```

3. **Décraft String**:
   ```
   1x String T3 → 9x String T2
   9x String T2 → 81x String T1
   81x String T1 → 729x String Vanilla
   ```

4. **Décraft Cultures**:
   ```
   1x Wheat T3 → 9x Wheat T2
   9x Wheat T2 → 81x Wheat T1
   81x Wheat T1 → 729x Wheat Vanilla
   ```

---

## 🚀 DÉPLOIEMENT

1. **Arrêter le serveur**
2. **Copier** `target/Kcraft-1.0.0.jar` → `plugins/Kcraft.jar`
3. **Redémarrer le serveur**
4. **Vérifier** logs au démarrage:
   ```
   [Kcraft] Chargé X crafts depuis hunter.yml (devrait être ~23 au lieu de 15)
   [Kcraft] Chargé X crafts depuis compacting.yml (devrait être ~46 au lieu de 24)
   [Kcraft] Total crafts chargés: ~XX (augmentation de 30)
   ```

---

## 📝 NOTES TECHNIQUES

### Système de commandes (CraftManager.java)
```java
// Console (console-command: true)
if (console-command == true) {
    Bukkit.dispatchCommand(ConsoleSender, command);
}

// Joueur (console-command: false)
else {
    Bukkit.dispatchCommand(player, command);
}
```

### Variables disponibles dans les commandes:
- `{player_name}` - Nom du joueur (console commands)
- Aucune variable nécessaire pour player commands (contexte automatique)

### Recettes SHAPELESS:
- Ne nécessitent pas de pattern
- Acceptent 1 item en input
- Retournent X items en output
- Parfait pour le décraft/décompactage

---

## ⚠️ POINTS D'ATTENTION

1. **OutilsEvolutif**: Si `/giveoutil` ne fonctionne toujours pas, vérifier:
   - Le plugin OutilsEvolutif est bien installé
   - Le joueur a la permission d'exécuter `/giveoutil`
   - La syntaxe exacte de la commande dans OutilsEvolutif

2. **Kharvester**: Si collector toujours invisible:
   - Vérifier que Kharvester est installé
   - Tester manuellement `/khgive Krunsh_ collector_stone 1`
   - Ajuster la syntaxe dans `collectors.yml` si nécessaire

3. **Décraft**: Les items DOIVENT avoir le NBT `sparrowmc-item: "xxx_t1"` pour être décraftables
   - Items créés via `/kcraft give` ont le NBT
   - Items vanilla n'ont PAS le NBT → ne peuvent pas être décraftés

---

**Date**: 2025-11-01  
**Version**: Kcraft-1.0.0  
**Status**: ✅ Compilé et prêt à déployer
