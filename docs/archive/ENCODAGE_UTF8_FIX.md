# 🔧 FIX ENCODAGE UTF-8 - Hologrammes Corrompus

## 📋 Problème Identifié

Les hologrammes affichent des caractères corrompus (α au lieu de à, Θ au lieu de é) après l'installation de Kcraft/Kclan.

**Cause:** L'ajout de nouveaux plugins a changé l'**ordre de chargement**, et HolographicExtension charge maintenant **AVANT** que l'encodage UTF-8 soit établi par la JVM.

---

## ✅ Solutions (par ordre de priorité)

### **Solution 1: Arguments JVM (RECOMMANDÉ)**

Ajoute ces arguments au démarrage de ton serveur Pterodactyl:

```bash
-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8
```

**Dans Pterodactyl:**
1. Va dans `Startup` de ton serveur
2. Dans "Additional Java Arguments" ou "JVM Arguments"
3. Ajoute: `-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8`
4. Redémarre le serveur

---

### **Solution 2: Forcer l'ordre de chargement**

Modifie `plugin.yml` de **HolographicExtension** ou des plugins d'hologrammes:

```yaml
# Ajouter dans leur plugin.yml:
loadbefore: [PlaceholderAPI, OutilsEvolutif, Kcraft, Kclan]
```

Ou ajoute `depend` dans Kcraft pour forcer le chargement après:

```yaml
# Dans Kcraft/plugin.yml
depend: [HolographicExtension, Kharvester]
softdepend: [Factions, Vault, PlaceholderAPI, ...]
```

---

### **Solution 3: Configuration Pterodactyl Docker**

Vérifie que ton Docker utilise UTF-8:

```bash
# Dans le container Docker
echo $LANG
# Doit afficher: en_US.UTF-8 ou fr_FR.UTF-8

# Si ce n'est pas le cas, ajoute dans docker-compose.yml:
environment:
  - LANG=fr_FR.UTF-8
  - LC_ALL=fr_FR.UTF-8
```

---

## 🔍 Vérification

Après application de la solution, vérifie:

1. **Les nouveaux hologrammes:**
   - Créé un nouvel hologramme avec accents
   - Vérifie qu'il s'affiche correctement

2. **Les anciens hologrammes:**
   - Ils peuvent rester corrompus (données déjà sauvegardées)
   - Tu devras les **recréer** ou **éditer** pour les corriger

3. **Logs au démarrage:**
```
[INFO]: Server startup encoding: UTF-8
[INFO]: Console encoding: UTF-8
```

---

## 📝 Notes Importantes

1. **Kcraft n'est PAS la cause directe**
   - Kcraft lit et écrit en UTF-8
   - Ne touche PAS aux hologrammes
   - N'affecte PAS l'encodage d'autres plugins

2. **Le problème est le timing**
   - L'ordre de chargement a changé
   - HolographicExtension charge trop tôt
   - La JVM n'a pas encore établi UTF-8

3. **Les FileWriter ont été corrigés**
   - TableManager et LoggingManager utilisent maintenant OutputStreamWriter avec UTF-8
   - Pour être 100% propre, même si ça n'affectait que les fichiers internes

---

## 🚀 Recommandation Finale

**Applique la Solution 1** (arguments JVM) car:
- ✅ Plus simple et rapide
- ✅ Affecte tout le serveur (pas que Kcraft)
- ✅ Permanent (pas besoin de modifier les plugins)
- ✅ Standard pour serveurs Minecraft

```bash
# Arguments complets recommandés:
-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true
```

---

**Questions?** Contacte-moi sur Discord ou GitHub.
