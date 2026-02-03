package com.javamaster44.logic;

import com.javamaster44.model.Board;
import com.javamaster44.model.Ship;
import com.javamaster44.ui.GameUI;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.*;

public class GameController {
    public enum GameState {
        SETUP, PLAYING, GAME_OVER
    }

    private final Board playerBoard;
    private final Board cpuBoard;
    private final GameUI ui;

    private int playerMoney;
    private final Map<String, Integer> inventory;
    private GameState currentState = GameState.SETUP;

    // AI State
    private final List<String> availableShots = new ArrayList<>();
    private final Deque<String> targetStack = new ArrayDeque<>();

    // Ship Definitions (Name -> [Length, Reward])
    public static final List<ShipDef> SHIP_DEFS = List.of(
            new ShipDef("Carrier", 5, 350),
            new ShipDef("Battleship", 4, 600),
            new ShipDef("Submarine", 3, 800),
            new ShipDef("Destroyer", 3, 800),
            new ShipDef("Patrol Boat", 2, 1100)
    );

    public record ShipDef(String name, int length, int reward) {}

    // Powerup Costs
    public static final Map<String, Integer> COSTS = Map.of(
            "Nuke", 50000,
            "Confusion Ray", 1000,
            "Ship Finder", 3000,
            "Torpedo", 5000,
            "Frag Bomb", 2000,
            "Cross Fire", 5200,
            "Bomb", 8000
    );

    public GameController(GameUI ui) {
        this.ui = ui;
        this.playerBoard = new Board();
        this.cpuBoard = new Board();

        SaveManager.SaveData data = SaveManager.load();
        this.playerMoney = data.money;
        this.inventory = data.inventory;

        // Don't start immediately, wait for New Game interaction or set to blank
        initializeAI();
        updateUI();
    }

    public void promptNewGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("New Game");
        alert.setHeaderText("Choose Placement Mode");
        alert.setContentText("Do you want to manually place your ships or have them auto-placed?");

        ButtonType btnAuto = new ButtonType("Auto Place");
        ButtonType btnManual = new ButtonType("Manual Place");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());

        alert.getButtonTypes().setAll(btnAuto, btnManual, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnAuto) {
                startAutoGame();
            } else if (result.get() == btnManual) {
                startManualSetup();
            }
        }
    }

    private void startAutoGame() {
        resetGame();
        playerBoard.placeShipsRandomly();
        currentState = GameState.PLAYING;
        ui.setSetupMode(false);
        updateUI();
    }

    private void startManualSetup() {
        resetGame();
        currentState = GameState.SETUP;
        ui.setSetupMode(true);
        updateUI();
    }

    private void resetGame() {
        playerBoard.reset();
        cpuBoard.reset();
        cpuBoard.placeShipsRandomly(); // CPU always auto-places
        initializeAI();
        currentState = GameState.SETUP;
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

    // Called when Player clicks THEIR OWN board
    public void handlePlayerSetupClick(char row, int col, String shipName, boolean horizontal) {
        if (currentState != GameState.SETUP) return;

        ShipDef def = SHIP_DEFS.stream().filter(s -> s.name().equals(shipName)).findFirst().orElse(null);
        if (def == null) return;

        // Overwrite logic: Remove existing ship of same name
        playerBoard.removeShip(shipName);

        // Try to place new one
        if (!playerBoard.placeShip(def.name(), def.length(), def.reward(), row, col, horizontal)) {
            // Placement failed (out of bounds or overlap with OTHER ships)
            // If it failed, the old ship is already gone.
            // Optional: Restore old ship if we wanted to be nice, but "pick up and fail to drop" implies removing it usually.
            ui.showError("Invalid Placement", "Cannot place ship there.");
        }
        updateUI();
    }

    public void finishSetup() {
        if (playerBoard.getShips().size() < 5) {
            ui.showError("Setup Incomplete", "You must place all 5 ships before starting.");
            return;
        }
        currentState = GameState.PLAYING;
        ui.setSetupMode(false);
        updateUI();
    }

    // Called when Player clicks CPU board
    public void handlePlayerShot(char row, int col, String activePowerup) {
        if (currentState != GameState.PLAYING) return;

        if (activePowerup != null) {
            usePowerup(activePowerup, row, col);
            ui.clearSelection();
        } else {
            if (cpuBoard.getStatus(row, col) == 1 || cpuBoard.getStatus(row, col) == 2) {
                return;
            }
            processShot(cpuBoard, row, col, true);
        }

        if (!checkWinCondition()) {
            cpuTurn();
        }
    }

    private void processShot(Board targetBoard, char row, int col, boolean isPlayerShooter) {
        int status = targetBoard.getStatus(row, col);
        if (status == 1 || status == 2) return;

        if (status == 3) {
            targetBoard.setStatus(row, col, 2);
            Ship s = targetBoard.getShipAt(row, col);
            if (s != null) {
                s.hit();
                if (s.isSunk()) {
                    handleSink(targetBoard, s, isPlayerShooter);
                }
            }
        } else {
            targetBoard.setStatus(row, col, 1);
        }
    }

    private void handleSink(Board board, Ship ship, boolean isPlayerShooter) {
        if (isPlayerShooter) {
            playerMoney += ship.getReward();
            showAlert("You sunk the " + ship.getName() + "!", "Reward: $" + ship.getReward());
            saveData();
        } else {
            playerMoney -= ship.getReward()/3;
            showAlert("Your " + ship.getName() + " was sunk!", "Penalty: -$" + (ship.getReward()/3));
            saveData();
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
                        processShot(cpuBoard, r, c, true); //shoot the whole board
                    }
                }
            }
            case "Confusion Ray" -> {
                skipCpuTurn = true;
                showAlert("Confusion Ray Used!", "CPU will skip this turn.");
                processShot(cpuBoard, row, col, true);
            }
            case "Ship Finder" -> {
                for(char r='A'; r<='J'; r++)
                    for(int c=0; c<10; c++)
                        if(cpuBoard.getStatus(r, c) == 3) {
                            processShot(cpuBoard, r, c, true);
                            return;
                        }
            }
            case "Torpedo" -> {
                boolean horizontal = new Random().nextBoolean();
                if (horizontal)
                    for(int c=0; c<10; c++) processShot(cpuBoard, row, c, true);
                else
                    for(char r='A'; r<='J'; r++) processShot(cpuBoard, r, col, true);
            }
            case "Frag Bomb" -> {
                Random r = new Random();
                for(int i=0; i<8; i++)
                    processShot(cpuBoard, (char)('A'+r.nextInt(10)), r.nextInt(10), true);
            }
            case "Cross Fire" -> {
                for(int i=0; i<10; i++) processShot(cpuBoard, row, i, true);
                for(char r='A'; r<='J'; r++) processShot(cpuBoard, r, col, true);
            }
            case "Bomb" -> {
                for(int rOff = -2; rOff <= 2; rOff++)
                    for(int cOff = -2; cOff <= 2; cOff++) {
                        char r = (char)(row + rOff);
                        int c = col + cOff;
                        if(r >= 'A' && r <= 'J' && c >= 0 && c < 10) processShot(cpuBoard, r, c, true);
                    }
            }
        }
    }

    private boolean skipCpuTurn = false;

    private void cpuTurn() {
        if (currentState == GameState.GAME_OVER) return;
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

        if (prevStatus == 3 && newStatus == 2) {
            addValidTarget(r, c + 1);
            addValidTarget(r, c - 1);
            addValidTarget((char)(r + 1), c);
            addValidTarget((char)(r - 1), c);
        }

        updateUI();
        if (playerBoard.allShipsSunk()) {
            currentState = GameState.GAME_OVER;
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
            currentState = GameState.GAME_OVER;
            playerMoney += 2000;
            saveData();
            showAlert("VICTORY!", "You defeated the Computer! Bonus: $2000. Click 'New Game' to restart.");
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

    public GameState getGameState() {
        return currentState;
    }
}

