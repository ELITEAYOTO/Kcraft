# Script de nettoyage des fichiers craft obsolètes
# À exécuter sur le serveur

Write-Host "=== Nettoyage fichiers craft Kcraft ===" -ForegroundColor Cyan

$serverPath = "C:\Users\timot\Desktop\SERVER\plugins\Kcraft\crafts"

if (Test-Path $serverPath) {
    Write-Host "Dossier trouvé: $serverPath" -ForegroundColor Green
    
    # Supprimer les fichiers vides/obsolètes
    $filesToDelete = @("tools.yml", "weapons.yml", "evolution.yml")
    
    foreach ($file in $filesToDelete) {
        $filePath = Join-Path $serverPath $file
        if (Test-Path $filePath) {
            Remove-Item $filePath -Force
            Write-Host "✓ Supprimé: $file" -ForegroundColor Yellow
        } else {
            Write-Host "- Inexistant: $file" -ForegroundColor Gray
        }
    }
    
    Write-Host "`n✅ Nettoyage terminé!" -ForegroundColor Green
    Write-Host "Maintenant:" -ForegroundColor Cyan
    Write-Host "1. Copie le nouveau Kcraft-1.0.0.jar dans plugins/" -ForegroundColor White
    Write-Host "2. Redémarre le serveur" -ForegroundColor White
    Write-Host "3. Les bons fichiers seront créés automatiquement" -ForegroundColor White
    
} else {
    Write-Host "❌ Dossier non trouvé: $serverPath" -ForegroundColor Red
    Write-Host "Modifie le chemin dans le script si nécessaire" -ForegroundColor Yellow
}
