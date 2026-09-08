package com.mlesniak.vi;

/** Renders the editor state into one complete ANSI frame string. */
public final class Screen {
    private static final String CSI = "\033[";

    private Screen() {
    }

    public static String render(Editor e) {
        Buffer buf = e.buffer();
        StringBuilder sb = new StringBuilder(CSI + "?25l" + CSI + "H");
        for (int i = 0; i < e.rows() - 1; i++) {
            int r = e.scroll() + i;
            String line = r < buf.size() ? buf.line(r) : "~";
            sb.append(line, 0, Math.min(line.length(), e.cols()));
            sb.append(CSI).append("K\r\n");
        }
        int cy = e.row() - e.scroll() + 1;
        int cx = Math.min(e.col(), e.cols() - 1) + 1;
        sb.append(CSI).append(cy).append(';').append(cx).append('H');
        sb.append(CSI).append("?25h");
        return sb.toString();
    }
}
