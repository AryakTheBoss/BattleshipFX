package com.javamaster44.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Board {
    // 0 = Empty, 1 = Miss, 2 = Hit, 3 = Ship
    private final Map<Character, List<Integer>> grid;
    private final List<Ship> ships;
    private final Random random = new Random();

    public Board() {
        grid = new HashMap<>();
        ships = new ArrayList<>();
        initializeBoard();
    }

    public void reset() {
        grid.clear();
        ships.clear();
        initializeBoard();
    }

    private void initializeBoard() {
        for (char row = 'A'; row <= 'J'; row++) {
            List<Integer> cols = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                cols.add(0); // 0 = Empty
            }
            grid.put(row, cols);
        }
    }

    public int getStatus(char row, int col) {
        return grid.get(row).get(col);
    }

    public void setStatus(char row, int col, int status) {
        grid.get(row).set(col, status);
    }

    public void placeShipsRandomly() {
        createShip("Carrier", 5, 350);
        createShip("Battleship", 4, 600);
        createShip("Submarine", 3, 800);
        createShip("Destroyer", 3, 800);
        createShip("Patrol Boat", 2, 1100);
    }

    private void createShip(String name, int length, int reward) {
        boolean placed = false;
        while (!placed) {
            Ship ship = new Ship(name, length, reward);
            int rowIdx = random.nextInt(10);
            int colIdx = random.nextInt(10);
            boolean horizontal = random.nextBoolean();
            char rowChar = (char) ('A' + rowIdx);

            if (isValidPlacement(rowChar, colIdx, length, horizontal)) {
                for (int i = 0; i < length; i++) {
                    char r = horizontal ? rowChar : (char) (rowChar + i);
                    int c = horizontal ? colIdx + i : colIdx;
                    setStatus(r, c, 3); // 3 = Ship
                    ship.addCoordinate(r, c);
                }
                ships.add(ship);
                placed = true;
            }
        }
    }

    private boolean isValidPlacement(char row, int col, int length, boolean horizontal) {
        if (horizontal) {
            if (col + length > 10) return false;
            for (int i = 0; i < length; i++) {
                if (getStatus(row, col + i) != 0) return false;
            }
        } else {
            if ((row - 'A') + length > 10) return false;
            for (int i = 0; i < length; i++) {
                if (getStatus((char) (row + i), col) != 0) return false;
            }
        }
        return true;
    }

    public Ship getShipAt(char row, int col) {
        String coord = "" + row + col;
        for (Ship s : ships) {
            if (s.getCoordinates().contains(coord)) {
                return s;
            }
        }
        return null;
    }

    public List<Ship> getShips() { return ships; }

    public boolean allShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }
}
