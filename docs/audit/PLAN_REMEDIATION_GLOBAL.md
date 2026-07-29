# Plan global de remediation Kcraft

## Objectif
Stabiliser Kcraft pour un usage production serveur, avec un comportement coherent, previsible, configurable, et des integrations propres avec les plugins du workspace.

## Contraintes validees
- Le hook Factions historique n est plus prioritaire tel quel (plugin serveur cible: Kfaction).
- Le success-rate peut etre corrige rapidement si c est simple, sinon non bloquant immediate.
- Pour les collecteurs Kharvester:
  - Preview visuelle autorisee dans Kcraft.
  - A la validation du craft, remise finale via commande du plugin Kharvester.
- Priorite a une vision centralisee et synchronisee (pas de rework repetitif).

## Perimetre
1. Correctifs moteur craft (exactitude gameplay)
2. Correctifs commandes et IDs (give, givetable, docs)
3. Configurabilite complete (messages, effets, comportements)
4. Integrations inter-plugins (Kfaction, PluginCIT, Kharvester, OutilsEvolutif, etc.)
5. Menage documentaire Kcraft (.md)

## Roadmap execution

### Phase 0 - Inventaire et recherche (read-only)
- Cartographier:
  - IDs recipes vs IDs tables
  - Flux de craft (single et mass craft)
  - Usages NBT/CIT (sparrowmc-item, tags complementaires)
  - Integrations existantes et pseudo-hooks
- Resultat attendu:
  - Matrice de compatibilite
  - Liste de divergences config/code

### Phase 1 - Correctifs critiques gameplay
- Corriger les causes de crafts incoherents:
  - verification stricte des ingredients (type, quantite, data, nbt)
  - consommation correcte selon recette
  - boucle mass-craft robuste (revalidation recette a chaque iteration)
- Durcir les commandes:
  - give/givetable plus explicites
  - validation des IDs et messages de diagnostic

### Phase 2 - Configurabilite et proprete
- Uniformiser les cles de messages et placeholders
- Finaliser la lecture des options depuis YAML (eviter hardcode)
- Couvrir les chemins de fallback propres (null-safe, logs explicites)

### Phase 3 - Integrations plugins
- Kharvester:
  - conserver preview Kcraft
  - livraison finale via commande plugin
- Kfaction:
  - remplacer / isoler les points relies a Factions legacy
- PluginCIT et ecosysteme:
  - verifier coherence des IDs NBT utilises dans les crafts

### Phase 4 - Menage dossier Kcraft
- Inventorier tous les .md
- Classer en:
  - utile a conserver
  - a fusionner
  - obsolete
- Produire une structure documentaire claire (sans suppression destructive sans validation)

## Livrables
- Correctifs code Java
- Correctifs YAML (crafts/messages/config)
- Rapport integration inter-plugins
- Index documentaire Kcraft et proposition de menage
- Journal de verification (build + checks fonctionnels)

## Criteres de validation
- Build Maven OK
- Commandes give/givetable coherentes
- Craft single et mass conformes aux recettes
- Aucun craft impossible a cause d IDs incoherents
- Messages lisibles et non hardcodes critiques
- Integrations minimales fonctionnelles avec plugins cibles

## Notes de pilotage
- Commencer par les blocages production, ensuite la dette technique.
- Privilegier des changements incrementaux, testables, et clairement traces.
