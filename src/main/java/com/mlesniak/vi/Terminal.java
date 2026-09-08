package com.mlesniak.vi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Raw-mode terminal access via stty and ANSI escape sequences. Linux/macOS only. */
public class Terminal {
    public static final int ESC = 27;
    public static final int ENTER = 13;
    public static final int BACKSPACE = 127;
    public static final int UP = 1000;
    public static final int DOWN = 1001;
    public static final int RIGHT = 1002;
    public static final int LEFT = 1003;

    private String saved;

    public void enableRaw() throws IOException {
        saved = stty("-g").trim();
        stty("raw", "-echo");
        write("\033[?1049h");
    }

    public void restore() {
        write("\033[?1049l");
        if (saved == null) {
            return;
        }
        try {
            stty(saved);
        } catch (IOException e) {
            System.err.println("vi: cannot restore terminal: " + e.getMessage());
        }
    }

    /** Returns {rows, cols}; falls back to 24x80 if stty is unavailable or reports 0x0. */
    public int[] size() {
        try {
            String[] parts = stty("size").trim().split(" ");
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            if (rows <= 0 || cols <= 0) {
                return new int[] {24, 80};
            }
            return new int[] {rows, cols};
        } catch (IOException | RuntimeException e) {
            return new int[] {24, 80};
        }
    }

    public void write(String s) {
        System.out.print(s);
        System.out.flush();
    }

    private static String stty(String... args) throws IOException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "stty";
        System.arraycopy(args, 0, cmd, 1, args.length);
        Process p = new ProcessBuilder(cmd).redirectInput(new File("/dev/tty")).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            if (p.waitFor() != 0) {
                throw new IOException("stty " + String.join(" ", args) + " failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while running stty", e);
        }
        return out;
    }
}
