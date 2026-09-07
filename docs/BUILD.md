# Compiler KCraft 2.8.2

Prérequis : un **JDK Java 8** complet et Maven 3.9.9 dans le `PATH`.
Exécuter les commandes depuis la racine du dépôt KCraft. Aucun JAR de `libs/`
n'est nécessaire à la compilation.

## PowerShell

Cette commande utilise Java 8 pour la session de compilation puis restaure
`JAVA_HOME`. Adapter le chemin si le JDK est installé ailleurs.

```powershell
$kcraftPreviousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot'
    mvn --version
    if ($LASTEXITCODE -ne 0) { throw 'Maven indisponible' }
    mvn --batch-mode --no-transfer-progress verify
    if ($LASTEXITCODE -ne 0) { throw 'La validation KCraft a échoué' }
} finally {
    $env:JAVA_HOME = $kcraftPreviousJavaHome
}
```

La sortie de `mvn --version` doit indiquer Java `1.8`. `verify` compile les
sources, exécute les tests et produit **`target/Kcraft-2.8.2.jar`** avec NBT API
et Gson intégrés. Spigot et les plugins du serveur ne sont pas embarqués.
`target/original-Kcraft-2.8.2.jar` est le JAR intermédiaire sans bibliothèques ;
il ne doit pas être installé sur le serveur.

Pour repartir avec des classes neuves sans effacer les artefacts déjà présents
dans `target/`, choisir un nouveau sous-dossier de sortie :

```powershell
mvn --batch-mode --no-transfer-progress "-Dkcraft.build.directory=target/verify-2.8.2" verify
```

Cette variante suppose que Maven utilise déjà Java 8. Le JAR se trouve alors
dans `target/verify-2.8.2/Kcraft-2.8.2.jar`. Choisir un autre sous-dossier si ce
répertoire contient déjà une compilation. Aucune commande `clean` n'est requise
dans le dossier de travail courant.

## Versions et dépendances

La version de référence est celle de `pom.xml`. Maven remplace
`${project.version}` dans `plugin.yml` au moment du build. Le nom du JAR et la
version annoncée par Bukkit sont donc `2.8.2`. Les autres fichiers YAML sont
copiés sans filtrage pour préserver leurs variables et leur contenu.

| Dépendance | Source | Utilisation |
| --- | --- | --- |
| Spigot API `1.8.8-R0.1-20160221.082514-43` | Groupe public officiel Spigot | API fournie par le serveur |
| BungeeCord Chat `1.8-20160221.214602-128` | Groupe public officiel Spigot | API transitive de Spigot, fournie par le serveur |
| Item NBT API `2.12.2` | CodeMC | Incluse et relocalisée dans `me.krunsh.kcraft.nbtapi` |
| Gson `2.8.9` | Maven Central | Incluse pour les données JSON |
| JUnit `4.13.2` | Maven Central | Tests uniquement |
| Mockito Inline `4.11.0` | Maven Central | Tests du rechargement, compatible Java 8 |

Les anciens `systemPath` de Vault, PlaceholderAPI et Multiverse-Inventories
ont été retirés : le code ne référence aucune classe de leurs API. Les
déclarations `softdepend` restent présentes ; la détection des plugins utilise
Bukkit et le hook Kfaction utilise la réflexion. Aucun JAR tiers local n'est
publié avec les sources.

Les deux API historiques Spigot/Bungee sont fixées à leurs versions
horodatées, disponibles dans le [dépôt officiel Spigot](https://hub.spigotmc.org/nexus/content/groups/public/).
NBT API provient du [dépôt CodeMC](https://repo.codemc.io/repository/maven-public/).
Le premier build demande un accès réseau à ces dépôts et à Maven Central.
Les versions des outils de compilation, de ressources, de tests et de
packaging sont fixées dans le POM. Cette procédure permet de construire depuis
les sources sans `libs/` ; elle ne garantit pas des JAR identiques octet par
octet entre deux machines ou deux versions de JDK.

## Intégration continue et validation serveur

Le workflow `Build Java 8` exécute `mvn verify` avec Temurin 8, puis conserve le
JAR serveur comme artefact de la tâche. Les actions GitHub sont fixées à un
commit et le workflow possède uniquement `contents: read`. Le contrôle
`Repository hygiene` reste un workflow distinct. Aucun secret de déploiement
n'est nécessaire et le workflow ne publie rien sur le serveur Minecraft.

Les tests automatisés ne remplacent pas les essais sur le serveur 1.8.8 réel :
NBT, ordre des paquets, inventaires, rechargement et interactions avec
Kfaction/Kharvester/OutilsEvolutif doivent être validés avec les plugins installés.
