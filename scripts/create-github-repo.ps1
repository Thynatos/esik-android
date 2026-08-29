$ErrorActionPreference = "Stop"

$Repo = "Thynatos/esik-android"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI is required. Install it from https://cli.github.com/"
}
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git is required."
}

gh auth status | Out-Null
gh auth setup-git | Out-Null

if (-not (Test-Path ".git")) {
    git init -b main
    if ($LASTEXITCODE -ne 0) { throw "git init failed." }
}

$TrackedPaths = @(
    ".github",
    ".gitignore",
    "AGENTS.md",
    "COPILOT_PROMPT.md",
    "README.md",
    "app",
    "build.gradle.kts",
    "docs",
    "gradle",
    "gradle.properties",
    "local.properties.example",
    "scripts",
    "settings.gradle.kts"
)
foreach ($OptionalPath in @("gradlew", "gradlew.bat")) {
    if (Test-Path $OptionalPath) {
        $TrackedPaths += $OptionalPath
    }
}

git add -- $TrackedPaths
if ($LASTEXITCODE -ne 0) { throw "git add failed." }

git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -m "chore: bootstrap Esik Android prototype"
    if ($LASTEXITCODE -ne 0) {
        throw "git commit failed. Configure git user.name and user.email, then run the script again."
    }
}

git branch -M main
if ($LASTEXITCODE -ne 0) { throw "Could not set the main branch." }

gh repo view $Repo *> $null
if ($LASTEXITCODE -eq 0) {
    git remote get-url origin *> $null
    if ($LASTEXITCODE -eq 0) {
        git remote set-url origin "https://github.com/$Repo.git"
    }
    else {
        git remote add origin "https://github.com/$Repo.git"
    }
    if ($LASTEXITCODE -ne 0) { throw "Could not configure the origin remote." }
    git push -u origin main
}
else {
    gh repo create $Repo --private --source=. --remote=origin --push
}
if ($LASTEXITCODE -ne 0) { throw "GitHub repository creation or push failed." }

Write-Host "Repository ready: https://github.com/$Repo"
