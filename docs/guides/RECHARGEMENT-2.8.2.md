# KCraft 2.8.2 — rechargement cohérent

Mise à jour : 7 septembre 2026. Cette version intègre les travaux locaux
V2.1–V2.8.1 déjà développés et corrige leur rechargement avant publication.
La compilation et les tests sont distincts d'une validation en jeu.

## Comportement de `/kcraft reload`

La commande reste réservée à `kcraft.admin`. Le rechargement se déroule sur le
thread principal du serveur ; un appel asynchrone ou réentrant est refusé.

1. Lire strictement `config.yml`, `messages.yml` et les fichiers `crafts/*.yml`.
2. Préparer les nouvelles définitions de tables et recettes, sans toucher à
   celles qui sont actives. Les fichiers malformés et recettes partiellement
   illisibles ne sont plus ignorés silencieusement.
3. Vérifier les références aux tables, compiler le catalogue et son index,
   puis préparer les branchements aux plugins présents.
4. Publier les références préparées sans entrée/sortie disque ni callback
   de plugin au milieu de la bascule.
5. Fermer les menus custom et les grilles vanilla portant un aperçu KCraft
   afin de ne pas réutiliser un ancien résultat, même si l'ID de recette reste
   identique. Les ingrédients des grilles sont restitués par les chemins de
   fermeture existants ; leur restitution en situation réelle reste à tester.

Si une étape de préparation échoue, les configurations et messages actifs,
tables, recettes, index et aperçus restent ceux d'avant. Aucun menu n'est fermé.
Après une publication réussie, une éventuelle erreur de fermeture est signalée
comme une erreur de rafraîchissement, pas comme un faux retour à l'ancienne config.

La cohérence de la bascule gameplay repose sur le thread principal Bukkit.
Ce n'est pas une promesse de transaction globale pour des appels API asynchrones.
Le couple configuration/messages/runtime lu notamment par les logs est publié
dans un même snapshot `volatile`. Les fichiers sur disque sont la prochaine
configuration candidate, pas nécessairement la configuration active après un refus.

## Ce qui est préservé

- Les placements de tables en mémoire, leur fichier et leur tampon de sauvegarde.
- L'instance de journalisation, ses événements en attente et ses statistiques.
- Les exemples déjà supprimés volontairement : ils ne sont pas recréés au reload.
- Les configurations existantes : aucune migration ni réécriture automatique.

Les métriques du nouveau gestionnaire de matching repartent de zéro après un
reload accepté. Les recettes ajoutées seulement en mémoire via une API doivent
être réenregistrées après reload ; les fichiers restent sa source de chargement,
comme précédemment. Un redémarrage nécessite de réenregistrer ces recettes aussi.

## Journalisation

L'activation des logs après un démarrage avec logs désactivés crée le worker
manquant sans recréer les compteurs. Un second appel ne crée pas un second worker
dans le contrat d'appel sur le thread principal.

À l'arrêt, le dernier flush en cours est attendu. En cas d'erreur disque,
le flush ne boucle plus indéfiniment : une erreur indique combien d'événements
n'ont pas été écrits. Ils restent uniquement en mémoire, donc peuvent être perdus
à la fin du processus. Une panne disque ou un arrêt brutal ne peut pas être
présenté comme une garantie de zéro perte.

## Vérifications automatiques

99 tests Java 8 réussis : 60 tests déjà présents dans la refonte locale et
39 ajoutés pour les snapshots, tables candidates, rechargement, aperçus et logs.

- Configuration/messages malformés, YAML de recette invalide, matériau inconnu.
- ID de recette dupliqué, table absente, élément scalaire dans les ingrédients.
- Échec d'un branchement préparé, appel hors thread principal, rechargement réentrant.
- Conservation des objets actifs lors du refus et reprise possible ensuite.
- Publication avant fermeture, erreur d'un joueur ne bloquant pas les suivants.
- Activation du worker, panne disque, reprise et synchronisation de son arrêt.
- Chargement et compilation des 112 recettes d'exemple embarquées.

Les dépendances sont résolues depuis un cache Maven neuf, sans `libs/` local.
Voir [le guide de compilation](../BUILD.md) pour Java 8, la version unique Maven
et la localisation du JAR complet. Le contrôle `Repository hygiene` est distinct
du workflow de compilation Java 8.

## Recette manuelle avant installation générale

1. Sur le serveur de développement, ouvrir une table custom avec des ingrédients,
   puis une grille vanilla utilisant une recette KCraft.
2. Introduire une erreur de taille de table ou un matériau inconnu, lancer reload :
   vérifier refus, ancien résultat, anciens messages et conservation des ingrédients.
3. Corriger et modifier le résultat d'une recette sans changer son ID : reload doit
   fermer les grilles concernées, puis leur réouverture doit proposer le nouveau résultat.
4. Vérifier inventaire plein, curseur occupé, fermeture et déconnexion simultanées.
5. Modifier une table posée sans la casser, recharger puis redémarrer : vérifier
   que son emplacement reste enregistré. Une définition supprimée n'efface pas son
   enregistrement ; éviter de retirer un type encore placé sans plan de migration.
6. Vérifier les intégrations Kenchantement/KJobs/KFaction et le deuxième résultat
   vanilla avec le fork KHopeSpigot actuel.

Ce lot n'atteste ni la charge 500/700 joueurs ni le comportement visuel en jeu.
L'harmonisation complète des logs colorés et l'anti-duplication transverse restent
des lots distincts de la roadmap.
