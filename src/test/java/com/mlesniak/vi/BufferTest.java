package com.mlesniak.vi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BufferTest {

    @Test
    void emptyBufferHasOneEmptyLineAndNoName() {
        Buffer buf = new Buffer();
        assertEquals(1, buf.size());
        assertEquals("", buf.line(0));
        assertNull(buf.name());
        assertFalse(buf.dirty());
    }

    @Test
    void bufferFromEmptyListStillHasOneLine() {
        Buffer buf = new Buffer(List.of());
        assertEquals(1, buf.size());
        assertEquals("", buf.line(0));
    }

    @Test
    void insertAddsCharacterAndMarksDirty() {
        Buffer buf = new Buffer(List.of("ac"));
        buf.insert(0, 1, 'b');
        assertEquals("abc", buf.line(0));
        assertTrue(buf.dirty());
    }

    @Test
    void deleteRemovesCharacterAtColumn() {
        Buffer buf = new Buffer(List.of("abc"));
        buf.delete(0, 1);
        assertEquals("ac", buf.line(0));
    }

    @Test
    void splitCreatesTwoLines() {
        Buffer buf = new Buffer(List.of("hello world"));
        buf.split(0, 5);
        assertEquals(2, buf.size());
        assertEquals("hello", buf.line(0));
        assertEquals(" world", buf.line(1));
    }

    @Test
    void joinAppendsNextLine() {
        Buffer buf = new Buffer(List.of("foo", "bar", "baz"));
        buf.join(0);
        assertEquals(2, buf.size());
        assertEquals("foobar", buf.line(0));
        assertEquals("baz", buf.line(1));
    }

    @Test
    void insertLineAddsLineAtIndex() {
        Buffer buf = new Buffer(List.of("a", "c"));
        buf.insertLine(1, "b");
        assertEquals(List.of("a", "b", "c"), List.of(buf.line(0), buf.line(1), buf.line(2)));
    }

    @Test
    void deleteLineReturnsRemovedLine() {
        Buffer buf = new Buffer(List.of("a", "b"));
        assertEquals("a", buf.deleteLine(0));
        assertEquals(1, buf.size());
        assertEquals("b", buf.line(0));
    }

    @Test
    void deletingLastLineLeavesOneEmptyLine() {
        Buffer buf = new Buffer(List.of("only"));
        buf.deleteLine(0);
        assertEquals(1, buf.size());
        assertEquals("", buf.line(0));
    }

    @Test
    void nameCanBeSet() {
        Buffer buf = new Buffer();
        buf.name("x.txt");
        assertEquals("x.txt", buf.name());
    }

    @Test
    void loadReadsExistingFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("in.txt");
        Files.writeString(file, "one\ntwo\n");
        Buffer buf = Buffer.load(file.toString());
        assertEquals(2, buf.size());
        assertEquals("one", buf.line(0));
        assertEquals("two", buf.line(1));
        assertEquals(file.toString(), buf.name());
        assertFalse(buf.dirty());
    }

    @Test
    void loadMissingFileGivesEmptyNamedBuffer(@TempDir Path dir) throws IOException {
        String name = dir.resolve("missing.txt").toString();
        Buffer buf = Buffer.load(name);
        assertEquals(1, buf.size());
        assertEquals("", buf.line(0));
        assertEquals(name, buf.name());
    }

    @Test
    void saveWritesLinesWithTrailingNewlineAndClearsDirty(@TempDir Path dir) throws IOException {
        Buffer buf = new Buffer(List.of("a", "b"));
        buf.name(dir.resolve("out.txt").toString());
        buf.insert(0, 1, "x".charAt(0));
        buf.save();
        assertEquals("ax\nb\n", Files.readString(dir.resolve("out.txt")));
        assertFalse(buf.dirty());
    }

    @Test
    void saveWithoutNameThrows() {
        Buffer buf = new Buffer();
        IOException e = org.junit.jupiter.api.Assertions.assertThrows(IOException.class, buf::save);
        assertEquals("No file name", e.getMessage());
    }

    @Test
    void undoRestoresSnapshot() {
        Buffer buf = new Buffer(List.of("abc"));
        buf.snapshot();
        buf.delete(0, 0);
        assertEquals("bc", buf.line(0));
        assertTrue(buf.undo());
        assertEquals("abc", buf.line(0));
    }

    @Test
    void undoWithoutSnapshotReturnsFalse() {
        Buffer buf = new Buffer();
        assertFalse(buf.undo());
    }

    @Test
    void snapshotIsIndependentOfLaterChanges() {
        Buffer buf = new Buffer(List.of("a"));
        buf.snapshot();
        buf.insertLine(1, "b");
        buf.snapshot();
        buf.insertLine(2, "c");
        assertTrue(buf.undo());
        assertEquals(2, buf.size());
        assertTrue(buf.undo());
        assertEquals(1, buf.size());
        assertFalse(buf.undo());
    }
}
