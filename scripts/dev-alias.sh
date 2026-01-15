function extract_root_dir() {
  dirname "$(dirname "$(realpath "$1")")"
}

if [ -n "$BASH_SOURCE" ]; then
  CLI_DIR=$(extract_root_dir "${BASH_SOURCE[0]}")
elif [ -n "$ZSH_VERSION" ]; then
  # shellcheck disable=SC2296
  CLI_DIR=$(extract_root_dir "${(%):-%x}")
else
  CLI_DIR="."
fi

echo "Resolved docs-helper directory '$CLI_DIR'"

function dh() {
  export USERS_ACTUAL_CWD=$PWD

  cd "$CLI_DIR" || {
    echo "Failed to change directory to script location '$CLI_DIR'"
    exit 1
  }

  ./gradlew -q fatJar && java --enable-native-access=ALL-UNNAMED -jar build/libs/verifier.jar "$@"
}
