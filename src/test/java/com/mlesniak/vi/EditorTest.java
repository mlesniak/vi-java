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
}
