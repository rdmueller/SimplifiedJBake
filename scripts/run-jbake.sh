#!/bin/bash

# Ermittle das Verzeichnis, in dem das Script liegt
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Führe das Groovy-Script mit korrektem Classpath aus
groovy -cp "$SCRIPT_DIR/SimplifiedJBake" "$SCRIPT_DIR/SimplifiedJBake/SimplifiedJBake.groovy" "$@" 