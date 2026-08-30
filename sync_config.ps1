# Script para sincronizar la configuración desde el móvil al proyecto Android Studio

$PACKAGE_NAME = "com.example.nfcautomation"
$REMOTE_PATH = "/data/user/0/$PACKAGE_NAME/files/config"
$LOCAL_PATH = "app/src/main/assets/config"
$ADB = "C:\Users\ander\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "--- Iniciando sincronización de configuración ---" -ForegroundColor Cyan

# Crear carpeta local si no existe
if (!(Test-Path $LOCAL_PATH)) {
    New-Item -ItemType Directory -Path $LOCAL_PATH | Out-Null
}

$files = @("tags.json", "workflows.json", "states.json", "actions_template.json", "settings.json")

foreach ($file in $files) {
    Write-Host "Extrayendo $file..."
    & $ADB pull "$REMOTE_PATH/$file" "$LOCAL_PATH/$file"
}

Write-Host "--- Sincronización completada exitosamente ---" -ForegroundColor Green
Write-Host "Los cambios realizados en la app ahora son parte de tu proyecto."
