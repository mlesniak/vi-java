# vi-java

A minimal vi clone in plain Java 25 as a learning project. No runtime dependencies.

Build and run:

    mvn -q package
    java -cp target/classes com.mlesniak.vi.Vi file.txt

Or without Maven (Java 22+ multi-file source launch):

    java src/main/java/com/mlesniak/vi/Vi.java file.txt

Supported: normal mode (h j k l, arrows, w b 0 $ gg G, i a o O, x dd yy p u, :),
insert mode (typing, Enter, Backspace, Esc), command mode (:w [name], :q, :q!, :wq, :e name).
Linux/macOS only (raw mode via stty).
