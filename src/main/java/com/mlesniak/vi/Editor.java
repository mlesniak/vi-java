package com.mlesniak.vi;

/** Editor state machine: modes, cursor, scrolling and key dispatch. Does no terminal IO. */
public class Editor {
    public enum Mode {
        NORMAL, INSERT
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
    private String status = "";

    public Editor(Buffer buf, int rows, int cols) {
        this.buf = buf;
        this.rows = rows;
        this.cols = cols;
    }

    public void handleKey(int key) {
        status = "";
        switch (mode) {
            case NORMAL -> normal(key);
            case INSERT -> insert(key);
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
            case 'G' -> row = buf.size() - 1;
            case 'd', 'y', 'g' -> pending = key;
            case 'x' -> deleteChar();
            case 'p' -> paste();
            case 'u' -> undo();
            case 'i' -> enterInsert();
            case 'a' -> {
                enterInsert();
                col++;
            }
            case 'o' -> {
                enterInsert();
                buf.insertLine(row + 1, "");
                row++;
                col = 0;
            }
            case 'O' -> {
                enterInsert();
                buf.insertLine(row, "");
                col = 0;
            }
            default -> {
            }
        }
    }

    /** Second key of a two-key command (dd, yy, gg). Any other combination is dropped. */
    private void pendingKey(int key) {
        int op = pending;
        pending = 0;
        if (op == 'd' && key == 'd') {
            buf.snapshot();
            register = buf.deleteLine(row);
        } else if (op == 'y' && key == 'y') {
            register = buf.line(row);
        } else if (op == 'g' && key == 'g') {
            row = 0;
        }
    }

    /** Entering INSERT takes one snapshot, so the whole insert session is one undo step. */
    private void enterInsert() {
        buf.snapshot();
        mode = Mode.INSERT;
    }

    private void insert(int key) {
        switch (key) {
            case Terminal.ESC -> {
                mode = Mode.NORMAL;
                col--;
            }
            case Terminal.ENTER -> {
                buf.split(row, col);
                row++;
                col = 0;
            }
            case Terminal.BACKSPACE, 8 -> backspace();
            case Terminal.LEFT -> col--;
            case Terminal.RIGHT -> col++;
            case Terminal.UP -> row--;
            case Terminal.DOWN -> row++;
            default -> {
                if (key >= 32 && key <= 126) {
                    buf.insert(row, col, (char) key);
                    col++;
                }
            }
        }
    }

    private void backspace() {
        if (col > 0) {
            col--;
            buf.delete(row, col);
        } else if (row > 0) {
            col = buf.line(row - 1).length();
            buf.join(row - 1);
            row--;
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

    private void undo() {
        if (!buf.undo()) {
            status = "Already at oldest change";
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
        if (mode != Mode.INSERT) {
            max = Math.max(0, max - 1);
        }
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

    public String status() {
        return status;
    }

    public Buffer buffer() {
        return buf;
    }
}
