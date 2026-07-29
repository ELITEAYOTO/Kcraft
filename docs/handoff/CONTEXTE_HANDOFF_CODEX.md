# Contexte Handoff Codex - Kcraft

## 1) Objectif produit
Kcraft est un plugin de craft custom pour serveur Minecraft 1.8.8.
Logique metier cible:
- Etape 1: le joueur craft la Table Antique depuis une table vanilla 3x3.
- Etape 2: la Table Antique (tier1, GUI 4x4) sert a crafter les items custom Kcraft.

## 2) Environnement technique
- Plugin: Kcraft v1.0.0
- API Bukkit declaree: 1.8
- Dependance Spigot API: 1.8.8-R0.1-SNAPSHOT
- Java cible compilation: source/target 8
- Build: Maven shade
- Jar de sortie: target/Kcraft-1.0.0.jar

Fichiers de reference:
- src/main/resources/plugin.yml
- pom.xml

## 3) Emplacements de config (important)
Kcraft charge les recettes depuis le data folder runtime, pas depuis src au runtime.
- Runtime charge: plugins/Kcraft/crafts/*.yml
- Source template: src/main/resources/crafts/*.yml

Comportement de copie des defaults:
- Les fichiers src sont copies seulement si le fichier runtime n existe pas.
- Donc un fichier runtime deja present n est pas ecrase.

Fichier de reference:
- src/main/java/me/krunsh/kcraft/config/ConfigManager.java

## 4) Architecture Kcraft utile
- Boot plugin: Kcraft
- Moteur de recettes: CraftManager
- GUI custom et bouton craft: CraftGUIListener
- Gestion table custom placee: TableListener
- Bridge table vanilla 3x3 -> recette Kcraft: VanillaCraftListener
- Hooks plugins externes: PluginHookManager
- NBT util: NBTUtil

Fichiers de reference:
- src/main/java/me/krunsh/kcraft/Kcraft.java
- src/main/java/me/krunsh/kcraft/managers/CraftManager.java
- src/main/java/me/krunsh/kcraft/listeners/CraftGUIListener.java
- src/main/java/me/krunsh/kcraft/listeners/TableListener.java
- src/main/java/me/krunsh/kcraft/listeners/VanillaCraftListener.java
- src/main/java/me/krunsh/kcraft/hooks/PluginHookManager.java
- src/main/java/me/krunsh/kcraft/utils/NBTUtil.java

## 5) Etat actuel de la logique Table Antique
Correctif applique pour supporter le craft en table vanilla 3x3.

- Nouvelle cle recette: allow-vanilla-workbench
- Activee sur la recette craft_table_antique
- Listener vanilla ajoute pour preview + craft en CraftingInventory 3x3

Fichiers de reference:
- src/main/resources/crafts/tables.yml
- src/main/java/me/krunsh/kcraft/listeners/VanillaCraftListener.java
- src/main/java/me/krunsh/kcraft/Kcraft.java
- src/main/java/me/krunsh/kcraft/managers/CraftManager.java
- src/main/java/me/krunsh/kcraft/models/CraftRecipe.java

## 6) NBT conventions dans cet ecosysteme
Tags NBT utilises:
- sparrowmc-item: identifiant principal item custom (Kcraft, PluginCIT, OutilsEvolutif)
- cit-model: modele ressource pack
- kcraft-table-id: id table custom (ex: tier1)
- kcraft-table-type: type table custom
- kcraft-preview-item: item preview pour recipes basees sur commande

Points techniques:
- Matching ingredient Kcraft prend en compte: material, data, amount, name, lore, nbt
- hasMatchingNBT est central dans la validation
- Logs NBT verbeux bascules vers MessageUtil.debug niveau 3

Fichiers de reference:
- src/main/java/me/krunsh/kcraft/models/CraftIngredient.java
- src/main/java/me/krunsh/kcraft/utils/NBTUtil.java
- src/main/resources/crafts/*.yml
- ../PluginCIT/src/main/java/me/krunsh/customcit/CustomCIT.java

## 7) Integrations plugins externes
### Kfaction
- Kfaction est prioritaire sur Factions legacy.
- Hook reflection cote Kcraft:
  - getAPI()
  - getPlayerFaction(Player)
  - getLevel(), getName(), getPower()
- checkFactionLevel applique les restrictions de recettes.

Fichiers de reference:
- src/main/java/me/krunsh/kcraft/hooks/PluginHookManager.java
- ../Kfaction/src/main/java/me/krunsh/kfaction/api/KfactionAPI.java

### Factions legacy
- Fallback present mais implementation incomplete (stub).

### Kharvester
- Syntaxe retenue dans Kcraft recipes: khgive {player_name} collector_x 1
- Flux utilise preview + commande plugin externe.
- consume-result false pour eviter double don (preview + vrai item).

Fichier de reference:
- src/main/resources/crafts/collectors.yml

### OutilsEvolutif
- Commande detectee: /giveoutil
- Usage console observe: /giveoutil <type> <niveau> [joueur]
- Dans Kcraft, certaines recettes doivent executer la commande comme joueur selon contexte.

Fichiers de reference:
- ../Kclan/libs/OutilsEvolutif/src/main/java/me/krunsh/outilsevolutif/commands/GiveOutilCommand.java
- src/main/resources/crafts/evolutive_tools.yml

### PluginCIT
- Integration basee sur tag NBT sparrowmc-item.

Fichier de reference:
- ../PluginCIT/src/main/java/me/krunsh/customcit/CustomCIT.java

## 8) Commandes Kcraft utiles
- /kcraft reload
- /kcraft list [page]
- /kcraft info
- /kcraft give <joueur> <craft_id> [quantite]
- /kcraft givetable <joueur> <table_id>

Compat aliases table:
- give: craft_table_tier1 -> craft_table_antique
- givetable: table_antique ou craft_table_antique -> tier1

Fichier de reference:
- src/main/java/me/krunsh/kcraft/commands/KcraftCommand.java

## 9) Symptomes et interpretation utiles
Symptome vu en logs:
- NBT mismatch required=azurite actual=fragment_legendaire
Interpretation:
- Souvent normal pendant la recherche de recette (plusieurs recettes testees)
- Ce log seul ne prouve pas un bug recette finale

## 10) Points ouverts / dette technique
- KcraftNPCCommand: ouverture GUI encore en TODO (code commente)
- Factions legacy hook: placeholders de test, non finalise
- Nettoyage quality warnings java (concat logger, etc.) non bloquant runtime

## 11) Validation rapide apres deploiement
1. Build local: mvn clean package -DskipTests
2. Deployer jar dans plugins/Kcraft.jar
3. Verifier runtime recipe plugins/Kcraft/crafts/tables.yml
4. Executer /kcraft reload
5. Tester craft_table_antique en table vanilla 3x3
6. Poser la Table Antique, ouvrir GUI 4x4, tester un craft custom
7. Verifier une recette avec restriction faction (Kfaction)
8. Verifier une recette command based (collectors khgive)

## 12) Conseils pour nouvelle conversation Codex
Demarrer la nouvelle conversation avec:
- Le contenu de ce fichier
- Les logs exacts si un craft ne match pas
- Le fichier runtime recipe concerne (plugins/Kcraft/crafts/*.yml)
- Le resultat de /kcraft info
