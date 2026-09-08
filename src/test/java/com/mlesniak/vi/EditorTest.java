package com.mlesniak.vi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EditorTest {

    static final String ESC = "\u001b";

    /** Editor with 5 rows (4 text rows + status line) and 20 columns. */
    static Editor editor(String... lines) {
        return new Editor(new Buffer(List.of(lines)), 5, 20);
    }

    static void type(Editor e, String keys) {
        for (char c : keys.toCharArray()) {
            e.handleKey(c);
        }
    }

    static void assertCursor(Editor e, int row, int col) {
        assertEquals(row, e.row(), "row");
        assertEquals(col, e.col(), "col");
    }

    @Test
    void startsInNormalModeAtOrigin() {
        Editor e = editor("abc");
        assertEquals(Editor.Mode.NORMAL, e.mode());
        assertCursor(e, 0, 0);
        assertEquals(5, e.rows());
        assertEquals(20, e.cols());
    }

    @Test
    void hjklMoveCursor() {
        Editor e = editor("abc", "def");
        type(e, "l");
        assertCursor(e, 0, 1);
        type(e, "j");
        assertCursor(e, 1, 1);
        type(e, "h");
        assertCursor(e, 1, 0);
        type(e, "k");
        assertCursor(e, 0, 0);
    }

    @Test
    void arrowKeysMoveCursor() {
        Editor e = editor("abc", "def");
        e.handleKey(Terminal.RIGHT);
        assertCursor(e, 0, 1);
        e.handleKey(Terminal.DOWN);
        assertCursor(e, 1, 1);
        e.handleKey(Terminal.LEFT);
        assertCursor(e, 1, 0);
        e.handleKey(Terminal.UP);
        assertCursor(e, 0, 0);
    }

    @Test
    void movementIsClampedToBuffer() {
        Editor e = editor("ab", "c");
        type(e, "hk");
        assertCursor(e, 0, 0);
        type(e, "lll");
        assertCursor(e, 0, 1);
        type(e, "jj");
        assertCursor(e, 1, 0);
    }

    @Test
    void normalModeCursorStaysOnLastCharacter() {
        Editor e = editor("abcd", "x");
        type(e, "$");
        assertCursor(e, 0, 3);
        type(e, "j");
        assertCursor(e, 1, 0);
    }

    @Test
    void zeroAndDollarJumpWithinLine() {
        Editor e = editor("hello");
        type(e, "$");
        assertCursor(e, 0, 4);
        type(e, "0");
        assertCursor(e, 0, 0);
    }

    @Test
    void emptyLineKeepsColumnZero() {
        Editor e = editor("");
        type(e, "l$");
        assertCursor(e, 0, 0);
    }

    @Test
    void unknownKeysAreIgnored() {
        Editor e = editor("abc");
        type(e, "Z?");
        assertCursor(e, 0, 0);
        assertEquals(Editor.Mode.NORMAL, e.mode());
    }
}
