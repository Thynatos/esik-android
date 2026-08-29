#!/usr/bin/env bash
set -euo pipefail

repo="Thynatos/esik-android"
script_dir="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(CDPATH= cd -- "${script_dir}/.." && pwd)"

cd "$repo_root"

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI is required: https://cli.github.com/" >&2
  exit 1
fi
if ! command -v git >/dev/null 2>&1; then
  echo "Git is required." >&2
  exit 1
fi

gh auth status >/dev/null
gh auth setup-git >/dev/null

if [ ! -d .git ]; then
  git init -b main
fi

tracked_paths=(
  .github
  .gitignore
  AGENTS.md
  COPILOT_PROMPT.md
  README.md
  app
  build.gradle.kts
  docs
  gradle
  gradle.properties
  local.properties.example
  scripts
  settings.gradle.kts
)
for optional_path in gradlew gradlew.bat; do
  if [ -e "$optional_path" ]; then
    tracked_paths+=("$optional_path")
  fi
done

git add -- "${tracked_paths[@]}"
if ! git diff --cached --quiet; then
  git commit -m "chore: bootstrap Esik Android prototype"
fi

git branch -M main
if gh repo view "$repo" >/dev/null 2>&1; then
  if git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "https://github.com/${repo}.git"
  else
    git remote add origin "https://github.com/${repo}.git"
  fi
  git push -u origin main
else
  gh repo create "$repo" --private --source=. --remote=origin --push
fi

printf 'Repository ready: https://github.com/%s\n' "$repo"
