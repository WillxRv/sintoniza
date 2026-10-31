# Gera alexa-skill/dist/sintoniza-lambda.zip para o "Import Code" da Alexa-hosted.
# O zip inclui a chave do Firebase: não compartilhe e apague depois de importar.
# Uso (PowerShell): .\alexa-skill\pack.ps1

$ErrorActionPreference = 'Stop'
$lambda = Join-Path $PSScriptRoot 'lambda'
$dist = Join-Path $PSScriptRoot 'dist'
$zip = Join-Path $dist 'sintoniza-lambda.zip'

if (-not (Test-Path (Join-Path $lambda 'firebase-service-account.json'))) {
    throw 'Falta lambda/firebase-service-account.json (ver docs/AUTO-HOSPEDAGEM.md).'
}

New-Item -ItemType Directory -Force $dist | Out-Null
if (Test-Path $zip) { Remove-Item $zip -Force }

Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$files = @('index.js', 'package.json', 'firebase-service-account.json') +
    (Get-ChildItem (Join-Path $lambda 'src') -File | ForEach-Object { "src/$($_.Name)" })

# O console exige a pasta lambda/ na raiz do zip, com "/" nos caminhos (a Lambda roda em Linux).
$archive = [System.IO.Compression.ZipFile]::Open($zip, 'Create')
try {
    foreach ($f in $files) {
        $source = Join-Path $lambda ($f -replace '/', '\')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $source, "lambda/$f") | Out-Null
    }
} finally {
    $archive.Dispose()
}
Write-Output "Gerado: $zip"
