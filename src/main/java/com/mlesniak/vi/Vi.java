package com.mlesniak.vi;

import java.io.IOException;

/** Entry point: wires terminal, buffer, editor and screen into the main loop. */
public final class Vi {
    private Vi() {
    }

    public static void main(String[] args) {
        Buffer buf;
        try {
            buf = args.length > 0 ? Buffer.load(args[0]) : new Buffer();
        } catch (IOException e) {
            System.err.println("vi: " + e.getMessage());
            System.exit(1);
            return;
        }
        Terminal term = new Terminal();
        try {
            term.enableRaw();
            int[] size = term.size();
            Editor editor = new Editor(buf, size[0], size[1]);
            while (editor.running()) {
                term.write(Screen.render(editor));
                int key = term.readKey();
                if (key < 0) {
                    break;
                }
                editor.handleKey(key);
            }
        } catch (IOException e) {
            System.err.println("vi: " + e.getMessage());
        } finally {
            term.restore();
        }
    }
}
