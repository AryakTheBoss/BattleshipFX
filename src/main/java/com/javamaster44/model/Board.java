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

    private void initializeBoard() {
        for (char row = 'A'; row <= 'J'; row++) {
            List<Integer> cols = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                cols.add(0); // 0 = Empty
            }
            grid.put(row, cols);
        }
    }

    public void reset() {
        grid.clear();
        ships.clear();
        initializeBoard();
    }

    public int getStatus(char row, int col) {
        return grid.get(row).get(col);
    }

    public void setStatus(char row, int col, int status) {
        grid.get(row).set(col, status);
    }

    /**
     * Removes a ship by name from the board and clears its grid cells.
     */
    public void removeShip(String name) {
        Ship target = null;
        for (Ship s : ships) {
            if (s.getName().equals(name)) {
                target = s;
                break;
            }
        }

        if (target != null) {
            for (String coord : target.getCoordinates()) {
                char r = coord.charAt(0);
                int c = Integer.parseInt(coord.substring(1));
                setStatus(r, c, 0); // Reset to empty
            }
            ships.remove(target);
        }
    }

    /**
     * Tries to place a ship. Returns true if successful.
     */
    public boolean placeShip(String name, int length, int reward, char row, int col, boolean horizontal) {
        if (!isValidPlacement(row, col, length, horizontal)) {
            return false;
        }

        Ship ship = new Ship(name, length, reward);
        for (int i = 0; i < length; i++) {
            char r = horizontal ? row : (char) (row + i);
            int c = horizontal ? col + i : col;
            setStatus(r, c, 3); // 3 = Ship
            ship.addCoordinate(r, c);
        }
        ships.add(ship);
        return true;
    }

    public void placeShipsRandomly() {
        // Definitions matches GameController definitions
        createRandomShip("Carrier", 5, 350);
        createRandomShip("Battleship", 4, 600);
        createRandomShip("Submarine", 3, 800);
        createRandomShip("Destroyer", 3, 800);
        createRandomShip("Patrol Boat", 2, 1100);
    }

    private void createRandomShip(String name, int length, int reward) {
        boolean placed = false;
        while (!placed) {
            int rowIdx = random.nextInt(10);
            int colIdx = random.nextInt(10);
            boolean horizontal = random.nextBoolean();
            char rowChar = (char) ('A' + rowIdx);

            if (placeShip(name, length, reward, rowChar, colIdx, horizontal)) {
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
