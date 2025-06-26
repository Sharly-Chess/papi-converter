#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
"$DIR/jre-mac/bin/java" -cp "$DIR/dist/java/papiconverter.jar" org.sharlychess.papiconverter.PapiConverter "$@"
