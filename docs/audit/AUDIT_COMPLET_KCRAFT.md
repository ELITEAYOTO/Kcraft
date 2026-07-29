# Audit Complet Kcraft

Mise a jour 2026-04-17:
- Hook Factions legacy retire, Kfaction uniquement.
- Vanilla 3x3 reserve au craft de la Table Antique.
- Bypass OP applique aux permissions uniquement.

Date: 2026-04-17  
Scope: code source + config source + build Maven (Kcraft uniquement)

## 1) Resume executif

Le plugin compile et package correctement (`mvn clean package -DskipTests` OK), mais plusieurs points critiques/majeurs expliquent directement tes symptomes:

- Craft table antique bloque selon permissions/config runtime.
- Preview parfois incoherente (GUI custom et vanilla).
- Ecarts entre "ce qui est configurable" et "ce qui est reellement applique".
- Integrations partiellement implementees (Factions legacy, NPC/API GUI, logging detaille).

Niveau global: moyen a risque eleve en production si non corrige (surtout pour coherence gameplay).

## 2) Methodologie

Analyse manuelle complete des classes coeur:

- Boot/plugin: `src/main/java/me/krunsh/kcraft/Kcraft.java`
- Config: `src/main/java/me/krunsh/kcraft/config/ConfigManager.java`
- Moteur craft: `src/main/java/me/krunsh/kcraft/managers/CraftManager.java`
- GUI/listeners: `src/main/java/me/krunsh/kcraft/listeners/CraftGUIListener.java`, `src/main/java/me/krunsh/kcraft/listeners/VanillaCraftListener.java`, `src/main/java/me/krunsh/kcraft/listeners/TableListener.java`
- Hooks: `src/main/java/me/krunsh/kcraft/hooks/PluginHookManager.java`
- Modeles: `src/main/java/me/krunsh/kcraft/models/*`
- Utilitaires NBT/messages: `src/main/java/me/krunsh/kcraft/utils/*`
- Configs YAML source: `src/main/resources/config.yml`, `src/main/resources/messages.yml`, `src/main/resources/crafts/*.yml`
- Build: `pom.xml`

## 3) Points critiques et majeurs (verifies)

## 3.1 Table Antique impossible a craft (cause probable #1)

Constat:

- La recette source impose `permission: "kcraft.craft.table"` dans `src/main/resources/crafts/tables.yml` (ligne 14).
- Cette permission n est pas declaree dans `src/main/resources/plugin.yml` (section permissions), ou seule `kcraft.table.use` existe (ligne 37).
- Le check de permission est strict dans `CraftRecipe.canCraft(...)` (`src/main/java/me/krunsh/kcraft/models/CraftRecipe.java`, ligne 82).

Impact:

- Un joueur sans permission explicite ne verra pas/plus le craft antique en vanilla preview et ne pourra pas valider le craft.

Patch propose (P0):

1. Soit retirer `permission` de la recette `craft_table_antique`.
2. Soit ajouter la permission `kcraft.craft.table` dans `plugin.yml` avec `default: true` (ou via LuckPerms).
3. Garder une convention claire: permission de craft vs permission d usage de table.

## 3.2 Table Antique impossible a craft (cause probable #2)

Constat:

- Le plugin charge les recettes runtime depuis `plugins/Kcraft/crafts/*.yml` et ne recopie les defaults que si le fichier n existe pas (`ConfigManager`, lignes 108-111).
- Donc modifier `src/main/resources/crafts/tables.yml` ne met pas a jour un fichier runtime deja present.

Impact:

- Tu peux croire qu un fix est actif alors que le serveur utilise encore une ancienne recette runtime.

Patch propose (P0):

1. Ajouter une commande/admin report qui affiche la provenance des recipes chargees.
2. Ajouter une option de migration safe: `kcraft reload --sync-defaults` (sans ecraser brutalement).
3. Documenter dans `/kcraft info` le chemin runtime effectif et l horodatage des fichiers.

## 3.3 Vanilla workbench: hardcode qui bypass la config

Constat:

- Dans `VanillaCraftListener.isVanillaWorkbenchEnabled(...)` la ligne 166 force:
  `recipe.isAllowVanillaWorkbench() || "craft_table_antique".equalsIgnoreCase(recipe.getId())`

Impact:

- Si tu mets `allow-vanilla-workbench: false` pour `craft_table_antique`, le code l autorise quand meme.

Patch propose (P0):

- Remplacer par `return recipe.isAllowVanillaWorkbench();`.
- Conserver les alias uniquement dans les commandes admin (give/givetable), pas dans la logique metier de matching.

## 3.4 Preview vanilla stale/incoherente

Constat:

- `onPrepareItemCraft(...)` retourne simplement quand `recipe == null` (ligne 50) sans faire `inventory.setResult(null)`.

Impact:

- Risque de preview stale (ancien resultat conserve selon contexte inventaire).

Patch propose (P0):

- Toujours nettoyer le resultat quand aucune recette Kcraft ne match.

## 3.5 Preview GUI custom incoherente avec les regles reelles

Constat:

- `CraftGUIListener.updateCraftResult(...)` affiche le resultat des qu une recette match la matrice (`line 510+`), sans verifier:
  - `recipe.canCraft(player)`
  - plugin requis disponible
  - niveau faction
- Le clic craft, lui, verifie ces conditions (single craft, lignes 196 et 202).

Impact:

- Le joueur voit un preview craftable puis obtient un refus au clic.

Patch propose (P0):

- Appliquer la meme validation en preview que sur l execution (single source of truth).
- Ajouter un mode configurable:
  - `preview-when-blocked: false` (par defaut)
  - `preview-when-blocked: true` avec lore explicite "conditions non remplies".

## 3.6 Mass craft: validation plugin requise manquante

Constat:

- Dans `handleMassCraft(...)` (`CraftGUIListener`, ligne 259+), il y a check permission + faction, mais pas de check `isPluginAvailable(...)` (contrairement au single craft, ligne 196).

Impact:

- Les recettes dependantes d un plugin externe peuvent passer en mass craft dans un etat incomplet/invalide.

Patch propose (P0):

- Ajouter check plugin requis avant la boucle et a chaque iteration (comme la revalidation recette).

## 3.7 Factions legacy: bypass de restriction

Constat:

- `PluginHookManager.FactionsHook.checkFactionLevel(...)` retourne `true` temporaire (ligne 256), avec TODO.

Impact:

- Si Kfaction n est pas charge et Factions legacy est present, les restrictions `faction-level-required` sont bypass.

Patch propose (P0/P1):

1. Soit implementer reellement le hook Factions.
2. Soit fail-closed: si hook non implemente et recipe requiert faction, refuser craft + message admin clair.

## 4) Points majeurs de flexibilite/configurabilite

## 4.1 Taille de table configuree mais GUI hardcodee 4x4

Constat:

- `TableManager` charge `size` (ligne 175) et `CraftTable` supporte 3x3/5x5/7x7/9x9.
- Mais `CraftTableGUI` cree toujours un inventaire 54 slots + matrice fixe 4x4 (lignes 40-43).

Impact:

- Le plugin n est pas reellement flexible sur les tailles de table.
- `command-tables` de `config.yml` (5x5, 9x9) sont non fonctionnelles en pratique.

Patch propose (P1):

- Generer dynamiquement layout GUI selon `table.getDimensions()`.
- Exposer un schema de layout configurable par table (slots craft/result/button/decor).

## 4.2 Command tables et API d ouverture inachevees

Constat:

- `KcraftNPCCommand` contient TODO ouverture GUI (ligne 96).
- `KcraftAPI.openTable(...)` contient TODO ouverture GUI (ligne 198).

Impact:

- Partie "command-tables/NPC/API" incomplete donc non exploitable en production.

Patch propose (P1):

- Factoriser une methode unique `openTableFor(Player, CraftTable, OpenContext)` appelee par:
  - listener bloc
  - commande NPC
  - API publique

## 4.3 Incoherence de namespace config GUI

Constat:

- `ConfigManager` lit a la fois:
  - `Kcraft.gui.use-borders` (ligne 204)
  - `gui.use-borders` (ligne 288)
  - `Kcraft.gui.title-prefix` (ligne 212)
  - `gui.glass-colors.*` (lignes 272, 276, 280, 284)

Impact:

- Config confuse, risque d edition du mauvais bloc YAML.

Patch propose (P1):

- Unifier sur un seul namespace (recommande: `Kcraft.gui.*`).
- Conserver aliases legacy avec warning de deprecation au chargement.

## 4.4 Champ config `title` non lu pour command tables

Constat:

- `config.yml` utilise `title` dans `command-tables` (ligne 61).
- `TableManager.parseCommonProperties(...)` lit `name` uniquement (ligne 169).

Impact:

- Les titres configures peuvent etre ignores.

Patch propose (P1):

- Lire `name` puis fallback sur `title`, avec warning de migration.

## 4.5 Cache configure mais presque decoratif

Constat:

- `CacheManager` est initialise/rebuild, mais `CraftManager.findMatchingRecipe(...)` ne consulte pas ce cache.
- `getCachedRecipe(...)` est appelee nulle part hors `CacheManager`.

Impact:

- Les options `cache-popular-crafts`, `smart-cache-size` donnent peu d effet reel.

Patch propose (P2):

- Ajouter un index de matching base sur hash de matrice + table + recipe version.
- Invalider index au reload.

## 4.6 Logging partiellement non implemente

Constat:

- `LoggingManager.writeLogEntry(...)` TODO (ligne 181).
- `savePlayerStats()` TODO (ligne 242).
- hook faction pour logs TODO (lignes 266, 274).

Impact:

- Logs incomplets, stats non fiables, observabilite reduite.

Patch propose (P1/P2):

- Ecriture JSON append robuste (Gson deja present).
- Rotation/retention reellement appliquees.
- Stats par recipe/joueur/faction fiables.

## 4.7 Events API declares mais non emises en flux normal

Constat:

- `CraftManager.executeCraft(...)` contient TODO pour Pre/Post events (ligne 772).
- Les events existent (`api/events/*`) mais ne sont appeles que dans `forceCraft` API.

Impact:

- Les add-ons externes ne peuvent pas intercepter les crafts normaux.

Patch propose (P1):

- Emission systematique:
  - PreCraft cancellable avant roll success.
  - PostCraft apres execution et resultat.

## 5) Points de qualite metier (craft/preview/matching)

## 5.1 Matching NBT type-unsafe

Constat:

- `NBTUtil.hasNBTData(...)` compare via `toString()` (ligne 153).

Impact:

- Risque de faux positifs/faux negatifs selon types (int/string/float).

Patch propose (P1):

- Comparaison type-safe, avec mode configurable par recette:
  - `nbt-match-mode: strict|coerce-number|string`.

## 5.2 Matching nom/lore tres strict

Constat:

- `CraftIngredient.matches(...)` exige egalite exacte nom/lore (lignes 61 et 70).

Impact:

- Un detail de formatage/couleur/lore fait rater le craft.

Patch propose (P2):

- Ajouter options de recette:
  - `name-match: exact|strip-color|contains|regex`
  - `lore-match: exact|strip-color|contains-lines`

## 5.3 Resultat probabiliste et preview

Constat:

- `CraftRecipe.getResult()` est aleatoire pour plusieurs resultats (ligne 121+).
- Preview GUI appelle `getResult()` egalement.

Impact:

- Si `results` est utilise plus tard, preview peut differer du resultat final.

Patch propose (P2):

- Ajouter `getPreviewResult()` deterministe (plus probable ou explicite via config).
- Conserver le roll aleatoire uniquement a l execution.

## 5.4 Validation schema recipe insuffisante

Constat:

- Pas de validation forte sur:
  - rows pattern de tailles differentes
  - symboles pattern non declares dans ingredients
  - ingredients declares mais jamais utilises
  - compatibilite pattern vs taille table minimale

Patch propose (P1):

- Ajouter un validateur au chargement avec niveau:
  - erreur bloquante (recette ignoree)
  - warning (recette chargee mais signalee)

## 6) Cible "100% configurable et flexible": ce qu il manque encore

Pour atteindre ce niveau proprement, il faut au minimum:

1. Un schema de configuration versionne (`schema-version`) + migration.
2. Une validation formelle au boot/reload (et rapport lisible).
3. Une separation claire:
   - matching
   - autorisation
   - preview
   - execution
   - consommation
4. Des strategies configurables de preview et de matching.
5. Un systeme d events complet pour extensions.
6. Un comportement runtime explicite (source vs runtime configs) et outils de sync.

## 7) Backlog de patch priorise

## P0 (immediat, stabilisation gameplay)

1. Corriger permission recette table antique (`kcraft.craft.table`) ou declaration permission.
2. Corriger `isVanillaWorkbenchEnabled()` pour respecter uniquement la config.
3. Nettoyer `PrepareItemCraftEvent` quand aucune recette ne match (`setResult(null)`).
4. Harmoniser validation preview GUI avec validation execution.
5. Ajouter check plugin requis dans `handleMassCraft`.
6. Corriger/fail-closed le hook Factions legacy (sinon bypass).
7. Verifier fichier runtime `plugins/Kcraft/crafts/tables.yml` apres deploy.

## P1 (fiabilite + extensibilite)

1. Emission Pre/Post craft events sur flux normal.
2. Implementer command tables/NPC/API ouverture GUI.
3. Unifier namespace config GUI + aliases legacy.
4. Validation schema recipes au chargement.
5. Logging JSON reel (entries + stats player).

## P2 (performance et flexibilite avancee)

1. Rendre le cache reellement utile dans le matching.
2. Matching NBT type-safe configurable.
3. Strategies flexibles name/lore matching.
4. Preview deterministe vs resultats probabilistes.
5. Layout GUI dynamique par taille de table.

## 8) Plan de tests apres patch

1. `mvn clean package -DskipTests`.
2. Verifier runtime recipes chargees (`/kcraft info` + log de source).
3. Test craft_table_antique en vanilla 3x3 avec compte joueur non-op et op.
4. Test preview GUI sur recette avec permission manquante.
5. Test mass craft sur recette `enabled-if-plugin` avec plugin absent/present.
6. Test recette avec `faction-level-required` selon Kfaction/Factions.
7. Test recette command-based (`consume-result: false`) en single + mass.
8. Test reload et persistance des tables placees.

## 9) Notes de vigilance supplementaires

- `checkSpigotVersion()` est permissif (`contains("Spigot")`), peut accepter des versions non cibles.
- Plusieurs warnings statiques (concat logger, TODO, etc.) ne bloquent pas le build mais augmentent la dette.
- `command-tables` dans `config.yml` utilisent `title`, alors que le parser lit `name`.

## 10) Conclusion

Le plugin est deja sur une base exploitable, mais il n est pas encore "100% configurable/flexible" au sens strict.  
Avec les patchs P0 puis P1, tu regles tes symptomes actuels (craft defectueux, preview mauvaise, table antique) et tu securises le comportement production sans rework destructif.
