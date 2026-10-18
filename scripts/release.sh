#!/usr/bin/env bash
set -e

prev_tag=$(git describe --tags "$(git rev-list --tags --max-count=1)")
# Remove 'v' from tag which is in format v2.3.2 to get version name
old_version_name="${prev_tag#v}"

IFS='.' read -r major minor patch <<< "$old_version_name"
if [ -n "$patch" ]; then
  new_patch=$((patch + 1))
  new_version_name="${major}.${minor}.${new_patch}"
else
  old_version_code="${old_version_name//.}"
  new_version_code="$((old_version_code + 1))"
  new_version_code_length=${#new_version_code}
  new_version_name="${new_version_code:0:$((new_version_code_length-1))}.${new_version_code: -1}"
fi

echo "Bumping version: $old_version_name -> $new_version_name"

# Replace old appVersion line with new appVersion in gradle/libs.versions.toml
if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' "s/appVersion = \"$old_version_name\"/appVersion = \"$new_version_name\"/g" ./gradle/libs.versions.toml
else
  sed -i "s/appVersion = \"$old_version_name\"/appVersion = \"$new_version_name\"/g" ./gradle/libs.versions.toml
fi

git add -A
git commit -m "Release $new_version_name"
git push
git tag -a "v$new_version_name" -m "Tagging v$new_version_name"
git push origin "v$new_version_name"
