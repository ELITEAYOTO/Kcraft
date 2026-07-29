# Kcraft

Plugin de craft custom pour serveur Minecraft 1.8.8, base YAML, avec gestion NBT (`sparrowmc-item`) et integration plugins externes.

## Etat actuel
- Build Maven valide (`mvn clean package -DskipTests`)
- Moteur de craft actif en SHAPED + SHAPELESS
- `success-rate` applique en runtime
- Gestion `on-success` / `on-fail` active
- Compatibilite legacy conservee pour:
  - `fail-return-items` et `return-items-on-fail`
  - `command-on-success` / `command-on-fail`

## Fonctionnalites clefs
- Table physique principale: `tier1` (4x4)
- Tables GUI par commande (configurables)
- Matching ingredients complet:
  - material
  - data value 1.8.8
  - quantite
  - nom
  - lore
  - NBT
- Craft en masse (shift-click) avec revalidation a chaque iteration
- Actions de craft:
  - message
  - son
  - commande joueur/console
  - commandes multiples

## Integrations
- `Kfaction` (prioritaire pour les checks de niveau faction)
- `Kharvester`
- `OutilsEvolutif`
- `PluginCIT`
- `PlaceholderAPI`
- `Vault`

## Commandes
- `/kcraft list [page]`
- `/kcraft info`
- `/kcraft reload`
- `/kcraft give <joueur> <craft_id> [quantite]`
- `/kcraft givetable <joueur> <table_id>`
- `/kcraft stats [joueur]`

Alias de compatibilite table:
- `craft_table_antique`
- `table_antique`
- `craft_table_tier1`

## Configuration recipes (exemple)
```yaml
collector_expert:
  type: "SHAPED"
  table: "tier1"
  success-rate: 80
  return-items-on-fail: false
  ingredients:
    I:
      material: "IRON_INGOT"
      amount: 2
      data: 0
  on-success:
    message: "&aCraft reussi"
    command: "khgive {player_name} collector_stone 1"
    console-command: true
```

## Documentation du depot
- `docs/README.md`: index principal de la documentation
- `docs/audit/`: audits techniques, plans et remediations
- `docs/guides/`: guides d'utilisation et de configuration
- `docs/handoff/`: contexte de transmission pour reprise rapide
- `docs/tests/`: scenarios et commandes de validation manuelle
- `docs/scripts/`: scripts utilitaires (ops/maintenance)
- `docs/archive/`: anciennes notes conservees pour historique

## Build
```bash
mvn clean package -DskipTests
```

Jar genere:
- `target/Kcraft-1.0.0.jar`

## Notes
- Le projet compile, mais garde des warnings Maven non bloquants lies aux dependances `systemPath`.
- Pour valider l'integration finale, tester en serveur avec les plugins reels charges (Kfaction/Kharvester/OutilsEvolutif).