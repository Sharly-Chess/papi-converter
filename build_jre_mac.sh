#!/bin/bash
jlink \
  --add-modules java.base \
  --output jre-mac \
  --strip-debug --no-man-pages --no-header-files --compress=2
