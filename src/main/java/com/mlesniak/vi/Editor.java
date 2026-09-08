package com.mlesniak.vi;

/** Editor state machine: modes, cursor, scrolling and key dispatch. Does no terminal IO. */
public class Editor {
    public enum Mode {
        NORMAL
    }

    private Buffer buf;
    private Mode mode = Mode.NORMAL;
    private int row;
    private int col;
    private int scroll;
    private final int rows;
    private final int cols;
    private int pending;
    private String register;

    public Editor(Buffer buf, int rows, int cols) {
        this.buf = buf;
        this.rows = rows;
        this.cols = cols;
    }

    public void handleKey(int key) {
        switch (mode) {
            case NORMAL -> normal(key);
        }
        clampCursor();
        adjustScroll();
    }

    private void normal(int key) {
        if (pending != 0) {
            pendingKey(key);
            return;
        }
        switch (key) {
            case 'h', Terminal.LEFT -> col--;
            case 'l', Terminal.RIGHT -> col++;
            case 'j', Terminal.DOWN -> row++;
            case 'k', Terminal.UP -> row--;
            case '0' -> col = 0;
            case '$' -> col = buf.line(row).length();
            case 'w' -> wordForward();
            case 'b' -> wordBackward();
            case 'd' -> pending = key;
            case 'x' -> deleteChar();
            case 'p' -> paste();
            default -> {
            }
        }
    }

    /** Second key of a two-key command (dd). Any other combination is dropped. */
    private void pendingKey(int key) {
        int op = pending;
        pending = 0;
        if (op == 'd' && key == 'd') {
            buf.snapshot();
            register = buf.deleteLine(row);
        }
    }

    private void deleteChar() {
        if (!buf.line(row).isEmpty()) {
            buf.snapshot();
            buf.delete(row, col);
        }
    }

    private void paste() {
        if (register != null) {
            buf.snapshot();
            buf.insertLine(row + 1, register);
            row++;
            col = 0;
        }
    }

    private void wordForward() {
        int r = row;
        int c = col;
        String s = buf.line(r);
        while (c < s.length() && !Character.isWhitespace(s.charAt(c))) {
            c++;
        }
        while (true) {
            while (c < s.length() && Character.isWhitespace(s.charAt(c))) {
                c++;
            }
            if (c < s.length() || r == buf.size() - 1) {
                break;
            }
            r++;
            c = 0;
            s = buf.line(r);
        }
        row = r;
        col = c;
    }

    private void wordBackward() {
        int r = row;
        int c = col;
        while (true) {
            String s = buf.line(r);
            while (c > 0 && Character.isWhitespace(s.charAt(c - 1))) {
                c--;
            }
            if (c > 0 || r == 0) {
                break;
            }
            r--;
            c = buf.line(r).length();
        }
        String s = buf.line(r);
        while (c > 0 && !Character.isWhitespace(s.charAt(c - 1))) {
            c--;
        }
        row = r;
        col = c;
    }

    /** Keeps row inside the buffer and col on a character (or one past the end in INSERT). */
    private void clampCursor() {
        row = Math.max(0, Math.min(row, buf.size() - 1));
        int max = buf.line(row).length();
        max = Math.max(0, max - 1);
        col = Math.max(0, Math.min(col, max));
    }

    private void adjustScroll() {
        int textRows = rows - 1;
        if (row < scroll) {
            scroll = row;
        } else if (row >= scroll + textRows) {
            scroll = row - textRows + 1;
        }
    }

    public Mode mode() {
        return mode;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    public int scroll() {
        return scroll;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public Buffer buffer() {
        return buf;
    }
}
