#!/usr/bin/env bash
# Local preview for the side-build UI. Does not deploy GitHub Pages.
# New UI:  http://127.0.0.1:4173/preview/
# Live UI: http://127.0.0.1:4173/
cd "$(dirname "$0")/.."
echo "New UI:  http://127.0.0.1:4173/preview/"
echo "Live UI: http://127.0.0.1:4173/"
exec python3 -m http.server 4173
