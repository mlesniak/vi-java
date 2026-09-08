package com.mlesniak.vi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Text model: a list of lines plus file name and dirty flag. Never has zero lines. */
public class Buffer {
    private List<String> lines;
    private String name;
    private boolean dirty;

    public Buffer() {
        this(List.of(""));
    }

    public Buffer(List<String> initial) {
        lines = new ArrayList<>(initial);
        if (lines.isEmpty()) {
            lines.add("");
        }
    }

    public static Buffer load(String name) throws IOException {
        Path path = Path.of(name);
        List<String> read = Files.exists(path)
                ? Files.readAllLines(path, StandardCharsets.UTF_8)
                : List.of();
        Buffer buf = new Buffer(read);
        buf.name = name;
        return buf;
    }

    public void save() throws IOException {
        if (name == null) {
            throw new IOException("No file name");
        }
        String text = String.join("\n", lines) + "\n";
        Files.writeString(Path.of(name), text, StandardCharsets.UTF_8);
        dirty = false;
    }

    public String line(int row) {
        return lines.get(row);
    }

    public int size() {
        return lines.size();
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public boolean dirty() {
        return dirty;
    }

    public void insert(int row, int col, char c) {
        String s = lines.get(row);
        lines.set(row, s.substring(0, col) + c + s.substring(col));
        dirty = true;
    }

    public void delete(int row, int col) {
        String s = lines.get(row);
        lines.set(row, s.substring(0, col) + s.substring(col + 1));
        dirty = true;
    }

    public void split(int row, int col) {
        String s = lines.get(row);
        lines.set(row, s.substring(0, col));
        lines.add(row + 1, s.substring(col));
        dirty = true;
    }

    public void join(int row) {
        lines.set(row, lines.get(row) + lines.remove(row + 1));
        dirty = true;
    }

    public void insertLine(int row, String s) {
        lines.add(row, s);
        dirty = true;
    }

    public String deleteLine(int row) {
        String s = lines.remove(row);
        if (lines.isEmpty()) {
            lines.add("");
        }
        dirty = true;
        return s;
    }
}
