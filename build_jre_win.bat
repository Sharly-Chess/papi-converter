jlink --add-modules java.base,java.sql,java.scripting ^
      --output jre-win ^
      --strip-debug --no-man-pages --no-header-files --compress=2
