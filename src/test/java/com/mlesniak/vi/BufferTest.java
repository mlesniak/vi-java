package com.mlesniak.vi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

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
    void nameCanBeSet() {
        Buffer buf = new Buffer();
        buf.name("x.txt");
        assertEquals("x.txt", buf.name());
    }
}
