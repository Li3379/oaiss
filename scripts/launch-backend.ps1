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

$startProcessParams = @{
    FilePath = $backendBatch
    WorkingDirectory = $projectRoot
    WindowStyle = 'Hidden'
}

if ($arguments.Count -gt 0) {
    $startProcessParams.ArgumentList = $arguments
}

Start-Process @startProcessParams
