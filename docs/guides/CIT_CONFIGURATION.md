# ===============================================
#        KCRAFT - Configuration PluginCIT
#      Textures individuelles pour armures
# ===============================================

Ce fichier explique comment configurer PluginCIT pour que les armures en azurite 
aient leur texture visible sans porter tout le set complet.

## 📋 NBT ajoutés pour PluginCIT :

### 🛡️ ARMURE EN AZURITE :
Chaque pièce d'armure a maintenant des NBT spécifiques :

```yaml
# CASQUE
nbt:
  cit-model: "azurite_helmet"
  cit-texture: "azurite_helmet_texture"
  armor-piece: "helmet"
  individual-texture: true

# PLASTRON
nbt:
  cit-model: "azurite_chestplate"
  cit-texture: "azurite_chestplate_texture"
  armor-piece: "chestplate"
  individual-texture: true

# JAMBIÈRES
nbt:
  cit-model: "azurite_leggings"
  cit-texture: "azurite_leggings_texture"
  armor-piece: "leggings"
  individual-texture: true

# BOTTES
nbt:
  cit-model: "azurite_boots"
  cit-texture: "azurite_boots_texture"
  armor-piece: "boots"
  individual-texture: true
```

### ⚔️ ÉPÉE EN AZURITE :
```yaml
nbt:
  cit-model: "azurite_sword"
  cit-texture: "azurite_sword_texture"
  weapon-type: "sword"
  individual-texture: true
```

## 🔧 Configuration PluginCIT requise :

### 1. Dans votre pack de ressources CIT :

Créez les fichiers `.properties` suivants dans `assets/minecraft/cit/` :

#### `azurite_helmet.properties` :
```properties
type=armor
items=diamond_helmet
nbt.sparrowmc-item=azurite_helmet
nbt.cit-model=azurite_helmet
model=azurite_helmet
texture=azurite_helmet_texture
```

#### `azurite_chestplate.properties` :
```properties
type=armor
items=diamond_chestplate
nbt.sparrowmc-item=azurite_chestplate
nbt.cit-model=azurite_chestplate
model=azurite_chestplate
texture=azurite_chestplate_texture
```

#### `azurite_leggings.properties` :
```properties
type=armor
items=diamond_leggings
nbt.sparrowmc-item=azurite_leggings
nbt.cit-model=azurite_leggings
model=azurite_leggings
texture=azurite_leggings_texture
```

#### `azurite_boots.properties` :
```properties
type=armor
items=diamond_boots
nbt.sparrowmc-item=azurite_boots
nbt.cit-model=azurite_boots
model=azurite_boots
texture=azurite_boots_texture
```

#### `azurite_sword.properties` :
```properties
type=item
items=diamond_sword
nbt.sparrowmc-item=azurite_sword
nbt.cit-model=azurite_sword
model=azurite_sword
texture=azurite_sword_texture
```

### 2. Modèles et textures :

Placez vos fichiers de modèles 3D et textures dans :
- `assets/minecraft/models/item/` pour les modèles
- `assets/minecraft/textures/item/` pour les textures

### 3. Avantages de cette méthode :

✅ **Texture visible immédiatement** : Chaque pièce a sa texture sans porter le set complet
✅ **Identification unique** : Chaque pièce a son propre `cit-model`
✅ **Compatible PluginCIT** : Utilise les NBT standards de PluginCIT
✅ **Flexible** : Permet de porter une seule pièce avec sa texture

### 4. Pour tester :

1. Installez PluginCIT sur votre serveur
2. Créez le pack de ressources avec les `.properties` ci-dessus
3. Craftez une pièce d'armure en azurite
4. La texture devrait être visible même sans porter les autres pièces !

## 🎮 Commandes de test :

```
/kcraft give azurite_helmet <joueur>
/kcraft give azurite_chestplate <joueur>
/kcraft give azurite_leggings <joueur>
/kcraft give azurite_boots <joueur>
/kcraft give azurite_sword <joueur>
```

## 📝 Notes importantes :

- Les NBT `individual-texture: true` indiquent que la texture fonctionne seule
- Les `armor-piece` et `weapon-type` permettent une identification rapide
- Chaque pièce a son propre `cit-model` unique pour éviter les conflits