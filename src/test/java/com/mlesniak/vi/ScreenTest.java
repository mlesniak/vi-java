package com.mlesniak.vi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScreenTest {
    static final String ESC = "\u001b";

    /** Splits a frame into its lines, stripping the cursor-hide/home prefix. */
    static String[] rows(String frame) {
        String body = frame.substring((ESC + "[?25l" + ESC + "[H").length());
        return body.split("\r\n", -1);
    }

    static Editor editor(String... lines) {
        return new Editor(new Buffer(List.of(lines)), 4, 40);
    }

    @Test
    void frameStartsWithHideCursorAndHomeAndEndsWithShowCursor() {
        String frame = Screen.render(editor("a"));
        assertTrue(frame.startsWith(ESC + "[?25l" + ESC + "[H"));
        assertTrue(frame.endsWith(ESC + "[?25h"));
    }

    @Test
    void rendersTextRowsWithClearToEndOfLineAndTildeFiller() {
        String[] rows = rows(Screen.render(editor("one", "two")));
        assertEquals(4, rows.length);
        assertEquals("one" + ESC + "[K", rows[0]);
        assertEquals("two" + ESC + "[K", rows[1]);
        assertEquals("~" + ESC + "[K", rows[2]);
    }

    @Test
    void truncatesLongLinesToColumns() {
        String[] rows = rows(Screen.render(editor("0123456789".repeat(5))));
        assertEquals("0123456789".repeat(4) + ESC + "[K", rows[0]);
    }

    @Test
    void rendersFromScrollOffset() {
        Editor e = editor("1", "2", "3", "4", "5");
        e.handleKey('G');
        assertEquals(2, e.scroll());
        String[] rows = rows(Screen.render(e));
        assertEquals("3" + ESC + "[K", rows[0]);
        assertEquals("5" + ESC + "[K", rows[2]);
    }

    @Test
    void cursorPositionIsOneBasedAndScrollAware() {
        Editor e = editor("1", "2", "3", "4", "5");
        assertTrue(Screen.render(e).endsWith(ESC + "[1;1H" + ESC + "[?25h"));
        e.handleKey('G');
        assertTrue(Screen.render(e).endsWith(ESC + "[3;1H" + ESC + "[?25h"));
    }

    @Test
    void cursorColumnIsClampedToScreenWidth() {
        Editor e = editor("0123456789".repeat(5));
        e.handleKey('$');
        assertEquals(49, e.col());
        assertTrue(Screen.render(e).endsWith(ESC + "[1;40H" + ESC + "[?25h"));
    }
}
