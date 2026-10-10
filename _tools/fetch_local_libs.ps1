<#
    Copies the workspace mod jars into .\libs so the Gradle build can resolve
    its dependencies without network access.

    Usage (from the project root):
        pwsh -File .\_tools\fetch_local_libs.ps1
#>
[CmdletBinding()]
param(
    # Folder holding the source jars. Defaults to the parent of this project,
    # which is where the workspace keeps them.
    [string]$SourceDir = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $SourceDir)) {
    throw "Source directory not found: $SourceDir"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Force -Path $libs | Out-Null

$wanted = @(
    'create-1.21.1-6.0.10.jar',
    'createbigcannons-5.11.7+mc.1.21.1.jar',
    'ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar',
    'sable-neoforge-1.21.1-2.0.6.jar',
    'create-aeronautics-bundled-1.21.1-1.3.2.jar'
)

$missing = @()
foreach ($name in $wanted) {
    $src = Join-Path $SourceDir $name
    if (-not (Test-Path -LiteralPath $src)) { $missing += $name; continue }
    Copy-Item -LiteralPath $src -Destination (Join-Path $libs $name) -Force
    Write-Host "copied    $name"
}

# Registrate / Ponder / Flywheel are not on any public Maven. Create bundles
# them in META-INF/jarjar/, so extract them from the Create jar: the offline
# (MODE B) build then uses exactly the versions Create itself ships.
$createJar = Join-Path $libs 'create-1.21.1-6.0.10.jar'
if (Test-Path -LiteralPath $createJar) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($createJar)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -like 'META-INF/jarjar/*.jar') {
                $leaf = [System.IO.Path]::GetFileName($entry.FullName)
                $dest = Join-Path $libs $leaf
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest, $true)
                Write-Host "extracted $leaf"
            }
        }
    } finally {
        $zip.Dispose()
    }
} else {
    Write-Warning "create jar not found; skipped bundled-library extraction"
}

if ($missing.Count -gt 0) {
    Write-Warning ("Missing source jars: " + ($missing -join ', '))
    exit 1
}

Write-Host ""
Write-Host "libs/ is ready. Next: gradlew build  (see README.md section 2)."
