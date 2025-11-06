#!/bin/bash
jlink \
  --add-modules java.base,java.sql,java.scripting \
  --output jre-linux \
  --strip-debug --no-man-pages --no-header-files --compress=2
