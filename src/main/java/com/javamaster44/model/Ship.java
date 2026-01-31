package com.javamaster44.model;


import java.util.ArrayList;
import java.util.List;

public class Ship {
    private final String name;
    private final int length;
    private int hits;
    // Store coordinates as "A1", "B2", etc. for easy lookup
    private final List<String> coordinates = new ArrayList<>();
    private final int reward;

    public Ship(String name, int length, int reward) {
        this.name = name;
        this.length = length;
        this.reward = reward;
        this.hits = 0;
    }

    public String getName() { return name; }
    public int getLength() { return length; }
    public List<String> getCoordinates() { return coordinates; }
    public int getReward() { return reward; }

    public void addCoordinate(char row, int col) {
        coordinates.add("" + row + col);
    }

    public void hit() {
        hits++;
    }

    public boolean isSunk() {
        return hits >= length;
    }
}
