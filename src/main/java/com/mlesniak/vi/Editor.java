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
        switch (key) {
            case 'h', Terminal.LEFT -> col--;
            case 'l', Terminal.RIGHT -> col++;
            case 'j', Terminal.DOWN -> row++;
            case 'k', Terminal.UP -> row--;
            case '0' -> col = 0;
            case '$' -> col = buf.line(row).length();
            default -> {
            }
        }
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
