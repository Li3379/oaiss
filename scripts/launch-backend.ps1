param(
    [switch]$WithFabric
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$backendBatch = Join-Path $scriptDir 'start-backend.bat'
$arguments = @()

if ($WithFabric) {
    $arguments += '--with-fabric'
}

Start-Process -FilePath $backendBatch `
    -ArgumentList $arguments `
    -WorkingDirectory $projectRoot `
    -WindowStyle Hidden
