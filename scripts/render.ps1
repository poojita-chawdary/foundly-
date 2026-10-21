[CmdletBinding()]
param(
    [ValidateSet('Check', 'Validate', 'Build', 'Deploy')]
    [string]$Action = 'Check',
    [string]$BackendServiceId,
    [string]$FrontendServiceId,
    [string]$Commit
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
function Invoke-Checked {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Command failed with exit code $LASTEXITCODE" }
}
Push-Location $repoRoot
try {
    switch ($Action) {
        'Check' {
            Invoke-Checked 'render' @('--version')
            Invoke-Checked 'java' @('-version')
            Invoke-Checked 'node' @('--version')
            Invoke-Checked 'docker' @('info', '--format', '{{.ServerVersion}}')
            Write-Host 'Tooling is available. Validate also checks Render authentication.'
        }
        'Validate' {
            Invoke-Checked 'render' @('blueprints', 'validate', 'render.yaml', '-o', 'text')
        }
        'Build' {
            foreach ($component in @('mysql', 'backend', 'frontend')) {
                Invoke-Checked 'docker' @('build', '--file', "deploy/render/$component.Dockerfile", '--tag', "foundly-render-${component}:local", '.')
            }
        }
        'Deploy' {
            if ($BackendServiceId -notmatch '^srv-[a-zA-Z0-9]+$' -or
                $FrontendServiceId -notmatch '^srv-[a-zA-Z0-9]+$') {
                throw 'Provide the two service IDs from the Render dashboard. Create the Blueprint first.'
            }
            if ($BackendServiceId -eq $FrontendServiceId) { throw 'Service IDs must be different.' }
            if ($Commit -notmatch '^[a-fA-F0-9]{40}$') {
                throw 'Provide the full, tested Git commit SHA already pushed to the linked repository.'
            }
            Invoke-Checked 'render' @('blueprints', 'validate', 'render.yaml', '-o', 'text')
            foreach ($serviceId in @($BackendServiceId, $FrontendServiceId)) {
                Invoke-Checked 'render' @('deploys', 'create', $serviceId, '--commit', $Commit, '--wait', '--confirm', '-o', 'text')
            }
        }
    }
} finally { Pop-Location }
