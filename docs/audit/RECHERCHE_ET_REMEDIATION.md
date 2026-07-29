# Recherche et remediation Kcraft

## Objectif
Rendre Kcraft stable, coherent et configurable dans l'ecosysteme SparrowMC, avec priorite sur:
- coherence des IDs et NBT
- fiabilite des crafts GUI (single + mass craft)
- integration plugins (Kfaction, Kharvester, OutilsEvolutif)
- commandes admin claires et robustes

## Methode de recherche (workspace)
Analyse des sources et docs dans:
- Kcraft
- Kfaction
- PluginCIT
- OutilsEvolutif
- Kharvester (via usages commandes dans les recipes/docs)

Points verifies:
- usage du tag NBT `sparrowmc-item`
- commandes de don outillage evolutif (`/giveoutil`)
- conventions de recettes qui donnent un preview puis executent une commande plugin
- ecarts entre `src/main/resources` et `target/classes`

## Constats principaux

### 1) Success-rate configure mais non applique (avant correction)
- `success-rate` existait en YAML mais la logique de craft rendait toujours un succes.
- Effet concret: les recettes avec probabilite ne pouvaient jamais echouer.

### 2) Matching ingredients incomplet (avant correction)
- Le matching ne respectait pas completement la config (data value, NBT, quantites, etc.).
- Risque: crafts valides/refuses de facon incoherente.

### 3) Mass-craft vulnerable aux incoherences
- Le flux batch pouvait executer plusieurs crafts avec validation trop permissive.
- Risque d'exploitation ou de consommation incorrecte.

### 4) Ambiguite IDs table_antique
- Confusion entre recipe ID (`craft_table_antique`) et table ID (`tier1`).
- Impact direct sur les commandes de give.

### 5) Integration faction
- Le serveur cible utilise Kfaction.
- L'ancien hook Factions seul etait insuffisant.

### 6) Integrations command-based (Kharvester/OutilsEvolutif)
- Le mode robuste est: preview en resultat + action commande plugin au craft.
- Ce modele evite de dupliquer des objets non supportes nativement par Kcraft.

## Corrections appliquees

### Moteur Craft
- Application reelle de `success-rate` avec branche success/fail.
- Execution de `on-success` et `on-fail`.
- Respect de `return-items-on-fail`.
- Parsing ingredient et matching renforces (material/data/amount/name/lore/nbt).
- Recettes `enabled: false` ignorees proprement.

### GUI Craft
- Mass-craft revalide chaque iteration.
- Consommation exacte shaped/shapeless selon ingredients configures.
- Gestion fail/success coherente avec l'engine.

### Commandes et hooks
- `give` plus robuste (aliases, result null guard).
- `givetable` plus explicite.
- Alias compat pour `table_antique` / `craft_table_antique` / `craft_table_tier1`.
- Hook Kfaction ajoute (reflection), prioritaire sur Factions legacy.
- `reload` recharge aussi tables + hooks.

### Recipes YAML
- Fix des IDs NBT incoherents sur chunk harvester.
- Refonte des collectors Kharvester en mode:
  - preview item dans Kcraft
  - commande plugin sur succes
  - `consume-result: false` pour eviter double don

### Messages
- Ajout de cles manquantes pour eviter les messages fallback incoherents.

## Decision d'architecture retenue
1. Kcraft reste orchestrateur de recipes et validations.
2. Les items externes (harvester/outils evolutifs) sont livres par commande plugin cible.
3. Toutes les contraintes de craft doivent venir de YAML (pas de hardcode metier cache).
4. Kfaction est la reference faction de priorite dans cet environnement.

## Validation effectuee
- Build Maven Kcraft: OK (clean package, sans erreurs de compilation).
- Warnings non bloquants:
  - dependances systemPath (heritage du projet)
  - avertissements shading META-INF

## Risques residuels
- Verification runtime indispensable sur serveur de test avec plugins reels charges:
  - Kfaction present + API exposee selon reflection attendue
  - syntaxe exacte Kharvester (`khgive ...`) conforme a la version en prod
  - commandes OutilsEvolutif executees dans le bon contexte (console/player)

## Plan de verification serveur (court)
1. Test `/kcraft give <joueur> craft_table_antique 1`
2. Test `/kcraft givetable <joueur> tier1`
3. Craft recette avec `success-rate < 100` et verifier success/fail
4. Craft collector Kharvester et verifier que l'item final vient de la commande plugin
5. Test faction-level recipe avec Kfaction charge

## Menage dossier
- Suppression des artefacts d'analyse non necessaires.
- Clarification des docs pour limiter les doublons et exemples obsoletes.
