# Documentation Kcraft

Ce dossier centralise toute la documentation du plugin.

## Index rapide

- [Rechargement 2.8.2](guides/RECHARGEMENT-2.8.2.md) : état actif conservé en cas de refus, tests et recette manuelle
- `../README.md` : vue d'ensemble, commandes principales et build
- [BUILD.md](BUILD.md) : compilation Java 8, dependances publiques et integration continue
- `audit/` : audits techniques, plans de remediations, et historiques d'analyses
- `guides/` : guides fonctionnels (give/givetable, CIT, etc.)
- `handoff/` : contexte de passation pour reprise rapide d'un debug
- `tests/` : scenarios manuels et commandes de validation
- `scripts/` : scripts utilitaires d'ops/maintenance
- `archive/` : anciens rapports conserves pour historique

## Convention de maintenance

Les notes `V2.1-*` à `V2.8.1-*` décrivent les étapes historiques de la refonte.
Le guide 2.8.2 et `BUILD.md` font référence pour cette publication ; les anciennes
matrices de tests ne constituent pas de nouvelles validations serveur.

- Les documents actifs doivent vivre dans `audit/`, `guides/`, `handoff/`, `tests/` ou `scripts/`.
- Les notes devenues obsoletes ou remplacees sont deplacees vers `archive/`.
- La racine du projet doit rester minimale: code, build, et README principal.
