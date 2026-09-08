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
        String bottom = bottomLine(e);
        sb.append(bottom, 0, Math.min(bottom.length(), e.cols() - 1));
        sb.append(CSI).append('K');
        int cy;
        int cx;
        if (e.mode() == Editor.Mode.COMMAND) {
            cy = e.rows();
            cx = e.cmd().length() + 2;
        } else {
            cy = e.row() - e.scroll() + 1;
            cx = Math.min(e.col(), e.cols() - 1) + 1;
        }
        sb.append(CSI).append(cy).append(';').append(cx).append('H');
        sb.append(CSI).append("?25h");
        return sb.toString();
    }

    private static String bottomLine(Editor e) {
        if (e.mode() == Editor.Mode.COMMAND) {
            return ":" + e.cmd();
        }
        if (!e.status().isEmpty()) {
            return e.status();
        }
        Buffer buf = e.buffer();
        String name = buf.name() == null ? "[No Name]" : buf.name();
        String flag = buf.dirty() ? " [+]" : "";
        String mode = e.mode() == Editor.Mode.INSERT ? "  -- INSERT --" : "";
        return name + flag + mode + "  " + (e.row() + 1) + "," + (e.col() + 1);
    }
}
