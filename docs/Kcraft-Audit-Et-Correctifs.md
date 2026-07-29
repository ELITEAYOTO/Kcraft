# Kcraft - Audit complet et correctifs

> Statut : audit realise sur la base de code actuelle, correctifs appliques sur les bugs critiques NBT, GUI dynamique multi-tailles (3x3 / 4x4 / 5x5) operationnelle, API d'ouverture de table cablee, sons branches.

---

## 1. Vue d'ensemble

Kcraft est un systeme de crafts custom pour Minecraft 1.8.8 (PandaSpigot / Spigot) qui :

- intercepte les crafts vanilla 3x3 via `VanillaCraftListener` (`PrepareItemCraftEvent` + `CraftItemEvent`),
- expose des tables de craft custom via `CraftTableGUI` (inventaire 54 slots, layouts 3x3 / 4x4 / 5x5 selectionnes selon `CraftTable.getSize()`),
- charge les recettes depuis `src/main/resources/crafts/*.yml` (`hunter`, `compacting`, `azurite`, `collectors`, `evolutive_tools`, `tables`),
- supporte les ingredients avec matching `material + data + amount + displayName + lore + NBT`.

Packages cles :

| Package | Role |
|--------|------|
| `me.krunsh.kcraft.managers` | `CraftManager` (parsing + matching), `TableManager`, `CacheManager`, `LoggingManager` |
| `me.krunsh.kcraft.models` | `CraftRecipe`, `CraftIngredient`, `CraftResult`, `CraftTable`, `CraftType` |
| `me.krunsh.kcraft.listeners` | `VanillaCraftListener`, `CraftGUIListener`, `TableListener`, `DurabilityListener` |
| `me.krunsh.kcraft.gui` | `CraftTableGUI` (interface multi-tailles 3x3/4x4/5x5) |
| `me.krunsh.kcraft.utils` | `NBTUtil`, `MessageUtil`, `MaterialResolver` |

---

## 2. Bugs identifies par l'audit

### Critiques (correctifs appliques)

#### BUG-NBT-1 - Items vanilla rejetes par defaut

`CraftIngredient.matches()` n'avait aucun moyen de distinguer "j'accepte uniquement du blé vanilla pur" vs "j'accepte du blé ou n'importe quel blé custom". Sans `nbt:` defini, n'importe quel item (vanilla ou custom T1) etait accepte.

Correctif : ajout du flag `strict-material` dans `CraftIngredient` + parsing dans `CraftManager`. Quand `true`, refuse tout item portant des cles NBT custom (les balises vanilla comme `display`, `ench`, `Unbreakable`, etc. sont tolerees).

Filtre des cles vanilla dans `NBTUtil.hasCustomNBT()` :

```
display, ench, StoredEnchantments, AttributeModifiers, Unbreakable,
HideFlags, Damage, RepairCost, CanDestroy, CanPlaceOn,
BlockEntityTag, SkullOwner
```

Toute autre cle est consideree comme custom (sparrowmc-item, tier, etc.).

#### BUG-NBT-2 - Comparaison par `toString()` defaillante

Ancienne implementation dans `NBTUtil.hasNBTData()` :

```java
matches = requiredValue.toString().equals(actualValue.toString());
```

Probleme : `Float(1.0f)` produit `"1.0"`, `Integer(1)` produit `"1"`, donc une recette ayant `tier: 1` (Integer YAML) et un item NBT serialise en `Float` echouait sans raison apparente. Tres frequent en pratique parce que les types YAML et les types NBT API ne coincident pas systematiquement.

Correctif : nouvelle methode `compareNbtValues()` typee dans `NBTUtil` :

- `Number` vs `Number` → comparaison sur `longValue()` ou `doubleValue()` selon presence d'un decimal,
- `Boolean` ou variantes (`0`/`1`, `"true"`/`"false"`) → ramene au booleen,
- `String` vs `String` → comparaison directe,
- fallback `equals` puis `toString` pour les structures non triviales.

### Importants (non corriges dans ce jet, plan ci-dessous)

#### BUG-GUI-1 - GUI 4x4 hardcodee (CORRIGE)

`CraftTableGUI` selectionne maintenant les slots de la grille, du resultat, du bouton et des bordures decoratives selon `table.getSize()` : `3x3` (9 slots, grille centree), `4x4` (16 slots, layout historique), `5x5` (25 slots, grille top-left). Les tailles superieures (`7x7`, `9x9`) loggent un warning et retombent sur le layout `4x4` (cf. section 4 pour le plan d'extension).

`CraftTableGUI` declare des slots fixes :

```java
this.craftSlots = new int[] { 10, 11, 12, 13, 19, 20, 21, 22, 28, 29, 30, 31, 37, 38, 39, 40 };
this.resultSlot = 24;
this.craftButtonSlot = 49;
```

`CraftTable.getDimensions()` supporte pourtant `"3x3"`, `"5x5"`, `"7x7"`, `"9x9"`. Le rendu GUI est desormais branche pour 3x3 / 4x4 / 5x5 ; voir section 4 pour le plan d'extension 7x7 / 9x9.

#### BUG-VANILLA-3x3 - `VanillaCraftListener` strictement 3x3

`if (matrix == null || matrix.length != 9) return;` empeche tout usage des recettes sur table vanilla autre que 3x3. C'est cohérent avec Bukkit (la table vanilla n'a que 3x3), donc ce n'est pas un vrai bug : les tailles > 3 doivent passer par `CraftTableGUI`. Pas de correctif necessaire, mais a documenter pour eviter la confusion.

#### TODO restants

| Fichier | Ligne | Probleme |
|--------|-------|----------|
| `KcraftAPI.java` | 198 | `// TODO: Ouvrir la GUI` (API publique incomplete) |
| `KcraftAPI.java` | 271 | `// TODO: Implementer systeme de hooks personnalises` |
| `KcraftCommand.java` | 293 | `// TODO: Afficher les vraies stats depuis LoggingManager` |
| `KcraftNPCCommand.java` | 96 | `// TODO: Creer et ouvrir la GUI` |

---

## 3. Format de recette YAML

Exemple complet documentant toutes les options actuellement supportees :

```yaml
ma_recette:
  type: SHAPED              # SHAPED | SHAPELESS
  table: tier1              # ID de table (vanilla = "vanilla", customs = id declare dans tables.yml)
  instant-craft: true       # Pas de delai
  cached: true              # Cache pour perfs (mise en cache de la matrice)
  success-rate: 100         # Pourcentage de chance de reussir (0-100)
  fail-return-items: true   # Sur echec : rendre les ingredients (alias accepte : return-items-on-fail)

  pattern:                  # Uniquement pour SHAPED
    - "WWW"
    - "WWW"
    - "WWW"

  ingredients:              # Uniquement pour SHAPED (map cle -> ingredient)
    W:
      material: WHEAT       # Material Bukkit (1.8.8) ou alias resolu par MaterialResolver
      amount: 1
      data: 0               # Optionnel: data value 1.8.8 (couleur, variante)
      name: "&eBle T1"      # Optionnel: doit match exactement le displayName
      lore:                  # Optionnel: doit match exactement le lore complet
        - "&7Ligne 1"
      nbt:                  # Optionnel: cles NBT requises (typees: int, string, bool, double)
        sparrowmc-item: wheat_t1
        tier: 1
      strict-material: false # NOUVEAU - si true, refuse tout item portant des NBT custom

  result:                   # Resultat unique
    material: HAY_BLOCK
    amount: 1
    name: "&eBle Compacte T1"
    enchanted: true         # Glow effect
    lore:
      - "&79x Ble vanilla -> 1x Ble T1"
    nbt:
      sparrowmc-item: wheat_t1
      tier: 1
```

### Format SHAPELESS

```yaml
ma_recette_shapeless:
  type: SHAPELESS
  table: tier1
  ingredients:              # Liste plate
    - material: WHEAT
      amount: 4
      strict-material: true # NOUVEAU
    - material: STICK
      amount: 1
  result:
    material: BREAD
```

### Multi-resultats

`results:` est accepte (liste de `result`), avec poids et conditions, geres par `CraftManager.parseResults()`.

---

## 4. Plan d'evolution : tables multi-tailles

L'objectif demande est : supporter des tables `3x3`, `5x5`, et au-dela, totalement configurables, type WildCraft / RecipesPlus.

### Etat actuel

| Couche | 3x3 | 4x4 | 5x5 | 7x7 | 9x9 |
|--------|-----|-----|-----|-----|-----|
| `CraftTable.getDimensions()` | OK | -- | OK | OK | OK |
| `CraftManager.matchesPattern()` | OK (sqrt) | OK | OK | OK | OK |
| `CraftTableGUI` | **OK** | OK | **OK** | fallback 4x4 + warning | fallback 4x4 + warning |
| `VanillaCraftListener` | OK | non | non | non | non |

### Layouts livres

Tous les layouts utilisent un seul inventaire Bukkit de 54 slots (6 lignes x 9 colonnes).

| Taille | Slots grille | Result | Bouton craft |
|--------|--------------|--------|--------------|
| 3x3 | 10,11,12 / 19,20,21 / 28,29,30 | 24 | 49 |
| 4x4 | 10..13 / 19..22 / 28..31 / 37..40 | 24 | 49 |
| 5x5 | 0..4 / 9..13 / 18..22 / 27..31 / 36..40 | 26 | 49 |

La selection se fait via `table.getSize()` (champ `size` du YAML) avec valeurs supportees `"3x3"`, `"4x4"`, `"5x5"`. Toute autre valeur logge un warning et utilise le layout 4x4.

### Reste a faire (tailles 7x7 / 9x9)

- 7x7 (49 slots) : pas de place pour result + bouton dans un seul inventaire. Solutions possibles :
  - mode "auto-craft" : detection live de la recette + remise du resultat directement dans la hotbar du joueur (pas de bouton).
  - GUI sans bouton, slot resultat dans la barre de titre via packet (complexe en 1.8.8).
- 9x9 (81 slots) : impossible en un seul inventaire. Necessite multi-page ou deux fenetres synchronisees.
- Logger un warning quand une recette `5x5` est mappee sur une table `3x3`.

---

## 5. Configuration globale (`config.yml`)

Cles connues et leur usage :

| Cle | Type | Defaut | Usage |
|-----|------|--------|-------|
| `Kcraft.settings.debug-level` | int | 2 | 0=off, 1=erreurs, 2=info, 3=verbose (utilise par `MessageUtil.debug`) |
| `Kcraft.settings.enabled-worlds` | list<string> | (toutes) | Filtre des mondes |
| `Kcraft.settings.prefix` | string | "&8[&6Kcraft&8] " | Prefix des messages |
| `Kcraft.cache.*` | section | -- | Config `CacheManager` |
| `Kcraft.gui.borders` | bool | true | Active les vitres colorees |
| `Kcraft.gui.glass-*` | string | -- | Couleurs (WHITE, ORANGE, ...) |

Incoherences a corriger en V suivante :

- `tier2`, `tier3` mentionnes mais aucune table custom autre que `tier1` n'est pleinement implementee (GUI 4x4 cassee pour ces tailles).
- `enabled-if-plugin` documente mais non verifie dans toutes les recettes (verifier `CraftManager.parseCraftRecipe`).

---

## 6. Listeners et evenements

| Listener | Event | Priorite | Role |
|----------|-------|----------|------|
| `TableListener` | `PlayerInteractEvent` | HIGH | Detecte clic droit sur bloc enregistre -> ouvre `CraftTableGUI` |
| `TableListener` | `BlockPlaceEvent` | HIGH | Enregistre la table custom (NBT bloc) |
| `TableListener` | `BlockBreakEvent` | HIGH | Supprime la table du registre |
| `VanillaCraftListener` | `PrepareItemCraftEvent` | HIGHEST | Affiche le resultat custom dans la table vanilla 3x3 |
| `VanillaCraftListener` | `CraftItemEvent` | HIGHEST | Execute le craft, consomme les ingredients |
| `CraftGUIListener` | `InventoryClickEvent` | HIGHEST | Gere bouton craft + slots proteges (vitres, bordures) |
| `DurabilityListener` | `PlayerItemDamageEvent` | HIGHEST | Modulation durabilite via NBT custom |

Pas de conflit identifie : `VanillaCraftListener` filtre strict `matrix.length == 9`, `CraftGUIListener` filtre sur les inventaires titres comme `CraftTableGUI`.

---

## 7. Procedure de test (fix NBT)

Apres ce correctif, valider :

1. **Recette sans NBT requis, ingredient vanilla pur** -> doit craft. (`wheat_vanilla_to_t1`)
2. **Recette sans NBT requis, ingredient avec NBT custom autre** -> doit craft (sauf si `strict-material: true`).
3. **Recette avec NBT requis (`sparrowmc-item: wheat_t1`), item portant ce NBT en `Integer`** -> doit craft (auparavant cassait sur `Float`/`Integer` toString mismatch).
4. **Recette avec `strict-material: true`, item portant `sparrowmc-item: X`** -> doit refuser.
5. **Recette avec `strict-material: true`, item vanilla pur** -> doit craft.
6. **Recette avec NBT requis sur clef inexistante de l'item** -> doit refuser (comportement inchange).

Activer `debug-level: 3` dans `config.yml` pour voir les logs `NBT debug - ...`.

---

## 8. Fichiers modifies par ce correctif

| Fichier | Nature |
|--------|--------|
| `src/main/java/me/krunsh/kcraft/utils/NBTUtil.java` | Ajout `compareNbtValues()`, `hasCustomNBT()`, `toBoolean()`, remplacement comparaison toString par appel typed |
| `src/main/java/me/krunsh/kcraft/models/CraftIngredient.java` | Ajout champ `strictMaterial` + accesseurs + check dans `matches()` |
| `src/main/java/me/krunsh/kcraft/managers/CraftManager.java` | Parsing `strict-material` dans `parseIngredient()` (SHAPED) et `parseIngredients()` (SHAPELESS) |
| `src/main/java/me/krunsh/kcraft/gui/CraftTableGUI.java` | Layouts dynamiques 3x3 / 4x4 / 5x5 selon `table.getSize()` ; bordures decoratives calculees par taille ; cablage du son d'ouverture |
| `src/main/java/me/krunsh/kcraft/api/KcraftAPI.java` | `openTable()` instancie et ouvre maintenant la `CraftTableGUI` (TODO leve) |
| `src/main/java/me/krunsh/kcraft/commands/KcraftNPCCommand.java` | `/kcraft-npc` ouvre la GUI cible (TODO leve) |
| `src/main/java/me/krunsh/kcraft/listeners/TableListener.java` | Son d'ouverture branche sur clic droit du bloc-table |
| `src/main/java/me/krunsh/kcraft/listeners/CraftGUIListener.java` | Son de succes joue apres un craft reussi |
| `src/main/java/me/krunsh/kcraft/utils/SoundUtil.java` | **NOUVEAU** — helper `playSafe()` (catch des `Sound` inexistants en 1.8.8) |

Build : `mvn clean package -DskipTests` -> `BUILD SUCCESS`.

---

## 9. Prochaines etapes recommandees

1. ~~Implementer layouts 3x3 / 5x5~~ **fait**. Reste a couvrir 7x7 (mode auto-craft sans bouton) et 9x9 (multi-page).
2. ~~Resoudre les TODO `KcraftAPI` / `KcraftNPCCommand` (ouverture GUI)~~ **fait**. TODO restants : detection NPC Citizens, hooks personnalises, stats reelles dans `/kcraft stats`.
3. Ajouter des tests JUnit sur `CraftIngredient.matches()` couvrant les 6 cas de la section 7.
4. Documenter `tables.yml` (format exact) avec exemples 3x3 et 5x5.
5. Migrer la creation d'items vers `NBTUtil.createSparrowMCItem()` partout pour homogeneiser les types NBT (eviter le bug type-mismatch a la source).
6. `LoggingManager` : remplacer la serialisation JSON manuelle par Gson (deja inclus en dep transitive via Bukkit).
7. Particles : implementer le rendu des particules de craft (`recipe.particle`) via packets 1.8.8 (`PacketPlayOutWorldParticles`).
