package com.mlesniak.vi;

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
}
