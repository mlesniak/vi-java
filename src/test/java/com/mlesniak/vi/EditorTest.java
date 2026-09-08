package com.mlesniak.vi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void scrollFollowsCursorDownAndUp() {
        Editor e = editor("1", "2", "3", "4", "5", "6");
        assertEquals(0, e.scroll());
        type(e, "jjj");
        assertEquals(0, e.scroll());
        type(e, "j");
        assertEquals(1, e.scroll());
        type(e, "j");
        assertEquals(2, e.scroll());
        type(e, "kkkkk");
        assertEquals(0, e.scroll());
    }

    @Test
    void wMovesToNextWordStart() {
        Editor e = editor("foo  bar baz");
        type(e, "w");
        assertCursor(e, 0, 5);
        type(e, "w");
        assertCursor(e, 0, 9);
    }

    @Test
    void wCrossesLineBoundaries() {
        Editor e = editor("foo", "", "  bar");
        type(e, "w");
        assertCursor(e, 2, 2);
    }

    @Test
    void wAtEndOfBufferStaysOnLastCharacter() {
        Editor e = editor("foo bar");
        type(e, "www");
        assertCursor(e, 0, 6);
    }

    @Test
    void bMovesToPreviousWordStart() {
        Editor e = editor("foo bar");
        type(e, "$b");
        assertCursor(e, 0, 4);
        type(e, "b");
        assertCursor(e, 0, 0);
    }

    @Test
    void bFromWordStartCrossesLineBoundaries() {
        Editor e = editor("foo", "", "bar");
        type(e, "jjb");
        assertCursor(e, 0, 0);
    }

    @Test
    void bAtStartOfBufferStays() {
        Editor e = editor("foo");
        type(e, "b");
        assertCursor(e, 0, 0);
    }

    @Test
    void ggAndGJumpToFirstAndLastLine() {
        Editor e = editor("a", "b", "c");
        type(e, "G");
        assertCursor(e, 2, 0);
        type(e, "gg");
        assertCursor(e, 0, 0);
    }

    @Test
    void pendingKeyIsCancelledByOtherKey() {
        Editor e = editor("a", "b", "c");
        type(e, "G");
        type(e, "gx");
        assertCursor(e, 2, 0);
        assertEquals("c", e.buffer().line(2));
        type(e, "gg");
        assertCursor(e, 0, 0);
    }

    @Test
    void iEntersInsertModeAndTypesBeforeCursor() {
        Editor e = editor("ac");
        type(e, "libX");
        assertEquals(Editor.Mode.INSERT, e.mode());
        assertEquals("abXc", e.buffer().line(0));
        assertCursor(e, 0, 3);
    }

    @Test
    void escLeavesInsertAndMovesCursorLeft() {
        Editor e = editor("ab");
        type(e, "ix" + ESC);
        assertEquals(Editor.Mode.NORMAL, e.mode());
        assertCursor(e, 0, 0);
        type(e, "$a!" + ESC);
        assertEquals("xab!", e.buffer().line(0));
        assertCursor(e, 0, 3);
    }

    @Test
    void aAppendsAfterCursorEvenOnEmptyLine() {
        Editor e = editor("");
        type(e, "ax" + ESC);
        assertEquals("x", e.buffer().line(0));
        Editor e2 = editor("ab");
        type(e2, "aX" + ESC);
        assertEquals("aXb", e2.buffer().line(0));
    }

    @Test
    void oOpensLineBelowAndOOpensLineAbove() {
        Editor e = editor("a", "c");
        type(e, "ob" + ESC);
        assertEquals(List.of("a", "b", "c"), lines(e));
        assertCursor(e, 1, 0);
        type(e, "OX" + ESC);
        assertEquals(List.of("a", "X", "b", "c"), lines(e));
        assertCursor(e, 1, 0);
    }

    @Test
    void enterSplitsLineInInsertMode() {
        Editor e = editor("hello world");
        type(e, "wi\r" + ESC);
        assertEquals(List.of("hello ", "world"), lines(e));
        assertCursor(e, 1, 0);
    }

    @Test
    void backspaceDeletesLeftAndJoinsAtColumnZero() {
        Editor e = editor("ab", "cd");
        type(e, "jla");
        e.handleKey(Terminal.BACKSPACE);
        assertEquals("c", e.buffer().line(1));
        e.handleKey(8);
        assertEquals("", e.buffer().line(1));
        e.handleKey(Terminal.BACKSPACE);
        assertEquals(List.of("ab"), lines(e));
        assertCursor(e, 0, 2);
    }

    @Test
    void insertModeAllowsCursorPastLastCharacter() {
        Editor e = editor("ab");
        type(e, "i");
        e.handleKey(Terminal.RIGHT);
        e.handleKey(Terminal.RIGHT);
        e.handleKey(Terminal.RIGHT);
        assertCursor(e, 0, 2);
        type(e, "c");
        assertEquals("abc", e.buffer().line(0));
    }

    @Test
    void nonPrintableKeysAreIgnoredInInsertMode() {
        Editor e = editor("a");
        type(e, "i");
        e.handleKey(1);
        e.handleKey(200);
        assertEquals("a", e.buffer().line(0));
    }

    @Test
    void wholeInsertSessionIsOneUndoStep() {
        Editor e = editor("");
        type(e, "iabc\rdef" + ESC);
        assertEquals(List.of("abc", "def"), lines(e));
        assertEquals(true, e.buffer().undo());
        assertEquals(List.of(""), lines(e));
        assertEquals(false, e.buffer().undo());
    }

    static List<String> lines(Editor e) {
        List<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < e.buffer().size(); i++) {
            out.add(e.buffer().line(i));
        }
        return out;
    }

    @Test
    void xDeletesCharacterUnderCursor() {
        Editor e = editor("abc");
        type(e, "lx");
        assertEquals("ac", e.buffer().line(0));
        assertCursor(e, 0, 1);
        type(e, "x");
        assertEquals("a", e.buffer().line(0));
        assertCursor(e, 0, 0);
    }

    @Test
    void xOnEmptyLineDoesNothing() {
        Editor e = editor("");
        type(e, "x");
        assertEquals("", e.buffer().line(0));
        assertEquals(false, e.buffer().undo());
    }

    @Test
    void ddDeletesLineIntoRegisterAndPPastesBelow() {
        Editor e = editor("a", "b", "c");
        type(e, "jdd");
        assertEquals(List.of("a", "c"), lines(e));
        assertCursor(e, 1, 0);
        type(e, "p");
        assertEquals(List.of("a", "c", "b"), lines(e));
        assertCursor(e, 2, 0);
    }

    @Test
    void ddOnOnlyLineLeavesEmptyLine() {
        Editor e = editor("only");
        type(e, "dd");
        assertEquals(List.of(""), lines(e));
        assertCursor(e, 0, 0);
    }

    @Test
    void ddOnLastLineMovesCursorUp() {
        Editor e = editor("a", "b");
        type(e, "jdd");
        assertEquals(List.of("a"), lines(e));
        assertCursor(e, 0, 0);
    }

    @Test
    void yyCopiesWithoutDeleting() {
        Editor e = editor("a", "b");
        type(e, "yyjp");
        assertEquals(List.of("a", "b", "a"), lines(e));
        assertCursor(e, 2, 0);
    }

    @Test
    void pWithEmptyRegisterDoesNothing() {
        Editor e = editor("a");
        type(e, "p");
        assertEquals(List.of("a"), lines(e));
        assertEquals(false, e.buffer().undo());
    }

    @Test
    void uUndoesLastChange() {
        Editor e = editor("abc");
        type(e, "x");
        type(e, "dd");
        assertEquals(List.of(""), lines(e));
        type(e, "u");
        assertEquals(List.of("bc"), lines(e));
        type(e, "u");
        assertEquals(List.of("abc"), lines(e));
        assertEquals("", e.status());
        type(e, "u");
        assertEquals("Already at oldest change", e.status());
    }

    @Test
    void undoClampsCursor() {
        Editor e = editor("a", "b");
        type(e, "jddu");
        assertEquals(List.of("a", "b"), lines(e));
        type(e, "G");
        assertCursor(e, 1, 0);
    }

    @Test
    void colonEntersCommandModeAndCollectsText() {
        Editor e = editor("a");
        type(e, ":wq");
        assertEquals(Editor.Mode.COMMAND, e.mode());
        assertEquals("wq", e.cmd());
    }

    @Test
    void escLeavesCommandMode() {
        Editor e = editor("a");
        type(e, ":w" + ESC);
        assertEquals(Editor.Mode.NORMAL, e.mode());
        assertEquals(true, e.running());
    }

    @Test
    void backspaceEditsCommandAndLeavesWhenEmpty() {
        Editor e = editor("a");
        type(e, ":ab");
        e.handleKey(Terminal.BACKSPACE);
        assertEquals("a", e.cmd());
        e.handleKey(8);
        assertEquals(Editor.Mode.COMMAND, e.mode());
        e.handleKey(Terminal.BACKSPACE);
        assertEquals(Editor.Mode.NORMAL, e.mode());
    }

    @Test
    void emptyCommandJustReturnsToNormal() {
        Editor e = editor("a");
        type(e, ":\r");
        assertEquals(Editor.Mode.NORMAL, e.mode());
        assertEquals("", e.status());
    }

    @Test
    void qQuitsCleanBuffer() {
        Editor e = editor("a");
        type(e, ":q\r");
        assertEquals(false, e.running());
    }

    @Test
    void qRefusesDirtyBufferAndQBangForces() {
        Editor e = editor("a");
        type(e, "x:q\r");
        assertEquals(true, e.running());
        assertEquals("No write since last change (add ! to override)", e.status());
        type(e, ":q!\r");
        assertEquals(false, e.running());
    }

    @Test
    void wWritesToNamedFile(@TempDir Path dir) throws IOException {
        Editor e = editor("a");
        String name = dir.resolve("f.txt").toString();
        type(e, "x:w " + name + "\r");
        assertEquals("\n", Files.readString(dir.resolve("f.txt")));
        assertEquals(name, e.buffer().name());
        assertEquals(false, e.buffer().dirty());
        assertEquals("\"" + name + "\" written", e.status());
    }

    @Test
    void wWithoutNameShowsError() {
        Editor e = editor("a");
        type(e, ":w\r");
        assertEquals("No file name", e.status());
        assertEquals(true, e.running());
    }

    @Test
    void wqWritesAndQuits(@TempDir Path dir) throws IOException {
        Editor e = editor("a");
        String name = dir.resolve("f.txt").toString();
        type(e, "ib" + ESC + ":wq " + name + "\r");
        assertEquals("ba\n", Files.readString(dir.resolve("f.txt")));
        assertEquals(false, e.running());
    }

    @Test
    void wqWithoutNameDoesNotQuit() {
        Editor e = editor("a");
        type(e, ":wq\r");
        assertEquals("No file name", e.status());
        assertEquals(true, e.running());
    }

    @Test
    void unknownCommandShowsError() {
        Editor e = editor("a");
        type(e, ":foo bar\r");
        assertEquals("Not an editor command: foo bar", e.status());
        assertEquals(Editor.Mode.NORMAL, e.mode());
    }

    @Test
    void statusIsClearedOnNextKey() {
        Editor e = editor("a");
        type(e, ":foo\r");
        assertEquals("Not an editor command: foo", e.status());
        type(e, "l");
        assertEquals("", e.status());
    }

    @Test
    void statusIsEmptyInitially() {
        Editor e = editor("a");
        assertEquals("", e.status());
        assertEquals("", e.cmd());
        assertEquals(true, e.running());
    }
}
