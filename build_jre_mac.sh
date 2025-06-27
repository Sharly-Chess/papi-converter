#!/bin/bash
jlink \
  --add-modules java.base,java.sql,java.scripting \
  --output jre-mac \
  --strip-debug --no-man-pages --no-header-files --compress=2
