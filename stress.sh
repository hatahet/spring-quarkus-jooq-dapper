#!/usr/bin/env bash
# Keep the same positional artifact convention when invoked from the project root.
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${script_dir}/scripts/stress.sh" "$@"
