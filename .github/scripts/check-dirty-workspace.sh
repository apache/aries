#!/usr/bin/env bash
DIRTY=$(git status --porcelain)
if [ -n "$DIRTY" ]; then
  echo "::error title=Dirty workspace::Build modified files. Please run the build locally and commit all the changes."
  echo "$DIRTY" | while IFS= read -r line; do
    file="${line:3}"
    echo "::error file=$file,title=Unexpected change::This file was modified or created by the build"
  done
  exit 1
fi
