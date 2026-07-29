# 📋 GUIDE COMMANDE GIVE - Kcraft

## ✅ SYNTAXE CORRECTE

```bash
/kcraft give <JOUEUR> <CRAFT_ID> [quantité]
```

### **ORDRE DES ARGUMENTS (IMPORTANT!) :**
1. **JOUEUR** - Nom du joueur (obligatoire)
2. **CRAFT_ID** - ID du craft (obligatoire)  
3. **QUANTITÉ** - Nombre d'items 1-64 (optionnel, défaut: 1)

## 📝 EXEMPLES CORRECTS

### **1 item (quantité par défaut)**
```bash
/kcraft give Krunsh_ azurite_helmet
/kcraft give Krunsh_ wheat_t1
/kcraft give Player123 evolutive_pickaxe
```

### **Plusieurs items (avec quantité)**
```bash
/kcraft give Krunsh_ wheat_t1 10
/kcraft give Krunsh_ azurite_ingot 20
/kcraft give Player123 carrot_compact_t2 15
```

### **Stack complet**
```bash
/kcraft give Krunsh_ wheat_t1 64
/kcraft give Krunsh_ string_t1 64
```

## ❌ ERREURS COURANTES

### **Ordre incorrect** (NE FONCTIONNE PAS)
```bash
❌ /kcraft give azurite_helmet Krunsh_        # Mauvais ordre
❌ /kcraft give azurite_helmet 1 Krunsh_      # Mauvais ordre
❌ /kcraft give 10 wheat_t1 Krunsh_           # Mauvais ordre

✅ /kcraft give Krunsh_ azurite_helmet 1      # BON ORDRE
✅ /kcraft give Krunsh_ wheat_t1 10           # BON ORDRE
```

### **Arguments manquants**
```bash
❌ /kcraft give azurite_helmet                # Manque le joueur
❌ /kcraft give Krunsh_                       # Manque le craft_id

✅ /kcraft give Krunsh_ azurite_helmet        # Complet
```

## 🎮 ITEMS DISPONIBLES PAR CATÉGORIE

### **🌾 CULTURES (avec progression T1→T2→T3)**
```bash
# Blé
/kcraft give Krunsh_ wheat_t1 20
/kcraft give Krunsh_ wheat_compact_t2 10
/kcraft give Krunsh_ wheat_compact_t3 5

# Carottes  
/kcraft give Krunsh_ carrot_t1 20
/kcraft give Krunsh_ carrot_compact_t2 10

# Pommes de terre
/kcraft give Krunsh_ potato_compact_t1 20

# Verrues du Nether
/kcraft give Krunsh_ nether_wart_compact_t1 15

# Pastèques
/kcraft give Krunsh_ watermelon_compact_t1 12

# Citrouilles
/kcraft give Krunsh_ pumpkin_compact_t1 10

# Fèves de Cacao
/kcraft give Krunsh_ cocoa_compact_t1 25
```

### **💎 AZURITE**
```bash
# Base
/kcraft give Krunsh_ azurite_ingot 10
/kcraft give Krunsh_ azurite_stick 5
/kcraft give Krunsh_ azurite_block 2

# Armure complète
/kcraft give Krunsh_ azurite_helmet 1
/kcraft give Krunsh_ azurite_chestplate 1
/kcraft give Krunsh_ azurite_leggings 1
/kcraft give Krunsh_ azurite_boots 1

# Arme
/kcraft give Krunsh_ azurite_sword 1
```

### **🎯 LOOTS DE MOBS**
```bash
/kcraft give Krunsh_ string_t1 32
/kcraft give Krunsh_ string_t2 16
/kcraft give Krunsh_ string_t3 8

/kcraft give Krunsh_ rotten_flesh_t3 10
/kcraft give Krunsh_ bone_t3 15
/kcraft give Krunsh_ ender_pearl_t3 5
/kcraft give Krunsh_ leather_t3 12
/kcraft give Krunsh_ blaze_rod_t3 8
```

### **🔧 OUTILS ÉVOLUTIFS**
```bash
/kcraft give Krunsh_ evolutive_hoe 1
/kcraft give Krunsh_ evolutive_axe 1
/kcraft give Krunsh_ evolutive_pickaxe 1
/kcraft give Krunsh_ evolutive_fishing_rod 1
```

### **🏭 COLLECTEURS KHARVESTER**
```bash
/kcraft give Krunsh_ kharvester_collector_stone 1
/kcraft give Krunsh_ kharvester_collector_iron 1
/kcraft give Krunsh_ kharvester_collector_diamond 1
```

### **🛠️ TABLES**
```bash
/kcraft give Krunsh_ craft_table_antique 1
# OU utilise :
/kcraft givetable Krunsh_ tier1
```

## 💡 ASTUCES

### **Vérifier les crafts disponibles**
```bash
/kcraft list          # Liste tous les crafts
/kcraft list 2        # Page 2
/kcraft info          # Infos sur le plugin
```

### **Kit complet pour un joueur**
```bash
/kcraft give Player123 azurite_helmet 1
/kcraft give Player123 azurite_chestplate 1
/kcraft give Player123 azurite_leggings 1
/kcraft give Player123 azurite_boots 1
/kcraft give Player123 azurite_sword 1
/kcraft give Player123 evolutive_pickaxe 1
/kcraft give Player123 wheat_t1 20
```

### **Test rapide items**
```bash
/kcraft give Krunsh_ wheat_t1 1
/kcraft give Krunsh_ azurite_ingot 5
/kcraft give Krunsh_ string_t1 10
```

## 🔍 DEBUG

Si la commande ne fonctionne pas, vérifie dans la console :

```
[GIVE] Tentative: joueur=Krunsh_, craft=wheat_t1
[GIVE] Succès: Admin a donné 10x wheat_t1 à Krunsh_
```

Ou si erreur :
```
❌ Craft introuvable: wheat_t1
Utilisez /kcraft list pour voir tous les crafts disponibles
```

## 📌 RAPPEL IMPORTANT

**ORDRE = JOUEUR → CRAFT → QUANTITÉ**

```bash
✅ /kcraft give <JOUEUR> <CRAFT> [QTÉ]
   /kcraft give Krunsh_ wheat_t1 10
```

C'est tout ! Plus simple et plus clair ! 🎉