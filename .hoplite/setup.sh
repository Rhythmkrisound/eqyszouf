#!/usr/bin/env bash
set -euo pipefail
uv venv --allow-existing .venv -q
uv pip install -q -p .venv -r requirements.txt
