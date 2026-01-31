package com.javamaster44.logic;

import com.javamaster44.model.Board;
import com.javamaster44.model.Ship;
import com.javamaster44.ui.GameUI;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.*;

public class GameController {
    private final Board playerBoard;
    private final Board cpuBoard;
    private final GameUI ui;

    private int playerMoney;
    private final Map<String, Integer> inventory;
    private boolean gameOver = false;

    // AI State
    private final List<String> availableShots = new ArrayList<>();
    private final Deque<String> targetStack = new ArrayDeque<>();

    // Powerup Costs
    public static final Map<String, Integer> COSTS = Map.of(
            "Nuke", 100000,
            "Confusion Ray", 1000,
            "Ship Finder", 3000,
            "Torpedo", 5000,
            "Frag Bomb", 2000,
            "Cross Fire", 5200,
            "Bomb", 10000
    );

    public GameController(GameUI ui) {
        this.ui = ui;
        this.playerBoard = new Board();
        this.cpuBoard = new Board();

        SaveManager.SaveData data = SaveManager.load();
        this.playerMoney = data.money;
        this.inventory = data.inventory;

        initializeAI();
        startNewGame();
    }

    private void initializeAI() {
        availableShots.clear();
        targetStack.clear();
        for (char r = 'A'; r <= 'J'; r++) {
            for (int c = 0; c < 10; c++) {
                availableShots.add("" + r + c);
            }
        }
        Collections.shuffle(availableShots);
    }

    public void startNewGame() {
        gameOver = false;
        playerBoard.reset();
        cpuBoard.reset();
        playerBoard.placeShipsRandomly();
        cpuBoard.placeShipsRandomly();
        initializeAI();
        updateUI();
    }

    public void handlePlayerShot(char row, int col, String activePowerup) {
        if (gameOver) return;

        if (activePowerup != null) {
            usePowerup(activePowerup, row, col);
            ui.clearSelection();
        } else {
            // Standard shot
            if (cpuBoard.getStatus(row, col) == 1 || cpuBoard.getStatus(row, col) == 2) {
                return; // Already shot
            }
            processShot(cpuBoard, row, col, true);
        }

        // Check win BEFORE cpu turns
        if (!checkWinCondition()) {
            cpuTurn();
        }
    }

    private void processShot(Board targetBoard, char row, int col, boolean isPlayerShooter) {
        int status = targetBoard.getStatus(row, col);
        if (status == 1 || status == 2) return;

        if (status == 3) {
            targetBoard.setStatus(row, col, 2); // Hit
            Ship s = targetBoard.getShipAt(row, col);
            if (s != null) {
                s.hit();
                if (s.isSunk()) {
                    handleSink(targetBoard, s, isPlayerShooter);
                }
            }
        } else {
            targetBoard.setStatus(row, col, 1); // Miss
        }
    }

    private void handleSink(Board board, Ship ship, boolean isPlayerShooter) {
        if (isPlayerShooter) {
            playerMoney += ship.getReward();
            showAlert("You sunk the " + ship.getName() + "!", "Reward: $" + ship.getReward());
            saveData();
        } else {
            playerMoney -= ship.getReward();
            showAlert("Your " + ship.getName() + " was sunk!", "Penalty: -$" + ship.getReward());
            saveData();
            // FIX: Clear the AI stack so it stops shooting around the sunk ship
            targetStack.clear();
        }
    }

    private void usePowerup(String item, char row, int col) {
        if (inventory.getOrDefault(item, 0) <= 0) return;

        decrementInventory(item);

        switch (item) {
            case "Nuke" -> {
                for(char r='A'; r<='J'; r++) {
                    for(int c=0; c<10; c++) {
                        if(cpuBoard.getStatus(r, c) == 3) processShot(cpuBoard, r, c, true);
                    }
                }
            }
            case "Confusion Ray" -> {
                skipCpuTurn = true;
                showAlert("Confusion Ray Used!", "CPU will skip this turn.");
            }
            case "Ship Finder" -> {
                for(char r='A'; r<='J'; r++) {
                    for(int c=0; c<10; c++) {
                        if(cpuBoard.getStatus(r, c) == 3) {
                            processShot(cpuBoard, r, c, true);
                            return;
                        }
                    }
                }
            }
            case "Torpedo" -> {
                boolean horizontal = new Random().nextBoolean();
                if (horizontal) {
                    for(int c=0; c<10; c++) processShot(cpuBoard, row, c, true);
                } else {
                    for(char r='A'; r<='J'; r++) processShot(cpuBoard, r, col, true);
                }
            }
            case "Frag Bomb" -> {
                Random r = new Random();
                for(int i=0; i<8; i++) {
                    processShot(cpuBoard, (char)('A'+r.nextInt(10)), r.nextInt(10), true);
                }
            }
            case "Cross Fire" -> {
                for(int i=0; i<10; i++) processShot(cpuBoard, row, i, true);
                for(char r='A'; r<='J'; r++) processShot(cpuBoard, r, col, true);
            }
            case "Bomb" -> {
                for(int rOff = -2; rOff <= 2; rOff++) {
                    for(int cOff = -2; cOff <= 2; cOff++) {
                        char r = (char)(row + rOff);
                        int c = col + cOff;
                        if(r >= 'A' && r <= 'J' && c >= 0 && c < 10) {
                            processShot(cpuBoard, r, c, true);
                        }
                    }
                }
            }
        }
    }

    private boolean skipCpuTurn = false;

    private void cpuTurn() {
        if (gameOver) return;
        if (skipCpuTurn) {
            skipCpuTurn = false;
            updateUI();
            return;
        }

        String targetStr;
        if (!targetStack.isEmpty()) {
            targetStr = targetStack.pop();
        } else if (availableShots.isEmpty()) {
            return;
        } else {
            targetStr = availableShots.remove(availableShots.size() - 1);
        }

        char r = targetStr.charAt(0);
        int c = Integer.parseInt(targetStr.substring(1));

        int prevStatus = playerBoard.getStatus(r, c);
        processShot(playerBoard, r, c, false);
        int newStatus = playerBoard.getStatus(r, c);

        // AI Logic: If Hit (3 -> 2)
        if (prevStatus == 3 && newStatus == 2) {
            addValidTarget(r, c + 1);
            addValidTarget(r, c - 1);
            addValidTarget((char)(r + 1), c);
            addValidTarget((char)(r - 1), c);
        }

        updateUI();
        if (playerBoard.allShipsSunk()) {
            gameOver = true;
            showAlert("Game Over", "Computer Won! You lost money. Click 'New Game' to restart.");
            SaveManager.save(inventory, playerMoney);
        }
    }

    private void addValidTarget(char r, int c) {
        String k = "" + r + c;
        if (r >= 'A' && r <= 'J' && c >= 0 && c < 10 && availableShots.contains(k)) {
            availableShots.remove(k);
            targetStack.push(k);
        }
    }

    public boolean buyItem(String item) {
        int cost = COSTS.get(item);
        if (playerMoney >= cost) {
            playerMoney -= cost;
            inventory.put(item, inventory.getOrDefault(item, 0) + 1);
            saveData();
            updateUI();
            return true;
        }
        return false;
    }

    private boolean checkWinCondition() {
        updateUI();
        if (cpuBoard.allShipsSunk()) {
            gameOver = true;
            playerMoney += 1200;
            saveData();
            showAlert("VICTORY!", "You defeated the Computer! Bonus: $1200. Click 'New Game' to restart.");
            updateUI();
            return true;
        }
        return false;
    }

    private void saveData() {
        SaveManager.save(inventory, playerMoney);
    }

    private void updateUI() {
        Platform.runLater(() -> ui.refresh(playerBoard, cpuBoard, playerMoney, inventory));
    }

    private void decrementInventory(String item) {
        inventory.put(item, inventory.get(item) - 1);
        saveData();
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}

