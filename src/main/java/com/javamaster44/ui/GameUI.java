package com.javamaster44.ui;

import com.javamaster44.logic.GameController;
import com.javamaster44.model.Board;
import com.javamaster44.model.Ship;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.Map;

public class GameUI {
    private final BorderPane root;
    private final GridPane playerGrid;
    private final GridPane cpuGrid;
    private final Label statusLabel;
    private final MenuButton powerupMenu;
    private GameController controller;
    private String selectedPowerup = null;

    public GameUI() {
        root = new BorderPane();
        playerGrid = createGrid(false);
        cpuGrid = createGrid(true);
        statusLabel = new Label("Balance: $0");
        powerupMenu = new MenuButton("Powerups");

        initializeLayout();
        this.controller = new GameController(this);
    }

    public Parent getRoot() { return root; }

    private void initializeLayout() {
        // Top Bar
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button newGameBtn = new Button("New Game");
        newGameBtn.setOnAction(e -> controller.startNewGame());

        Button shopBtn = new Button("Shop");
        shopBtn.setOnAction(e -> showShop());

        // Add spacer to push money to the right or keep elements together
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(newGameBtn, shopBtn, powerupMenu, spacer, statusLabel);
        root.setTop(topBar);

        // Boards
        HBox boards = new HBox(50);
        boards.setPadding(new Insets(20));
        boards.setAlignment(Pos.CENTER);

        VBox pBox = new VBox(10, new Label("Player Board"), playerGrid);
        VBox cBox = new VBox(10, new Label("CPU Board"), cpuGrid);
        pBox.setAlignment(Pos.CENTER);
        cBox.setAlignment(Pos.CENTER);

        boards.getChildren().addAll(pBox, cBox);
        root.setCenter(boards);
    }

    private GridPane createGrid(boolean isCpu) {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-background-color: black; -fx-border-color: black;");

        // Headers
        for (int i = 0; i < 10; i++) {
            Label l = new Label(String.valueOf(i + 1));
            l.setPrefSize(30, 30);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill: white;"); // Improve visibility on black bg
            grid.add(l, i + 1, 0);

            Label r = new Label(String.valueOf((char)('A' + i)));
            r.setPrefSize(30, 30);
            r.setAlignment(Pos.CENTER);
            r.setStyle("-fx-text-fill: white;");
            grid.add(r, 0, i + 1);
        }

        // Cells
        for (char row = 'A'; row <= 'J'; row++) {
            for (int col = 0; col < 10; col++) {
                Button btn = new Button();
                btn.setPrefSize(30, 30);
                char r = row;
                int c = col;

                if (isCpu) {
                    btn.setOnAction(e -> controller.handlePlayerShot(r, c, selectedPowerup));
                } else {
                    btn.setDisable(true);
                    btn.setOpacity(1.0);
                }

                grid.add(btn, col + 1, (row - 'A') + 1);
            }
        }
        return grid;
    }

    public void refresh(Board pBoard, Board cBoard, int money, Map<String, Integer> inventory) {
        statusLabel.setText("Balance: $" + money);

        updateGrid(playerGrid, pBoard, false);
        updateGrid(cpuGrid, cBoard, true);

        powerupMenu.getItems().clear();
        for (String item : GameController.COSTS.keySet()) {
            int count = inventory.getOrDefault(item, 0);
            if (count > 0) {
                MenuItem mi = new MenuItem(item + " (" + count + ")");
                mi.setOnAction(e -> {
                    selectedPowerup = item;
                    powerupMenu.setText("Selected: " + item);
                });
                powerupMenu.getItems().add(mi);
            }
        }
        if (selectedPowerup == null) powerupMenu.setText("Powerups");
    }

    private void updateGrid(GridPane grid, Board board, boolean hideShips) {
        for (javafx.scene.Node node : grid.getChildren()) {
            if (!(node instanceof Button)) continue;
            Integer col = GridPane.getColumnIndex(node);
            Integer row = GridPane.getRowIndex(node);
            if (col == null || row == null || col == 0 || row == 0) continue;

            char rChar = (char) ('A' + (row - 1));
            int cInt = col - 1;
            int status = board.getStatus(rChar, cInt);
            Button btn = (Button) node;

            String color = "lightgray"; // Default water

            Ship s = board.getShipAt(rChar, cInt);
            boolean isSunk = (s != null && s.isSunk());

            if (isSunk) {
                color = "black";
            } else {
                switch (status) {
                    case 0: // Empty
                        color = "lightblue";
                        break;
                    case 1: // Miss
                        color = "blue";
                        break;
                    case 2: // Hit
                        color = "red";
                        break;
                    case 3: // Ship
                        if (!hideShips) color = "gray";
                        else color = "lightblue";
                        break;
                }
            }
            btn.setStyle("-fx-background-color: " + color + "; -fx-border-color: darkgray;");
        }
    }

    public void clearSelection() {
        selectedPowerup = null;
        powerupMenu.setText("Powerups");
    }

    private void showShop() {
        Alert shop = new Alert(Alert.AlertType.NONE);
        shop.setTitle("Shop");
        shop.setHeaderText("Buy Powerups");

        GridPane content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);

        int row = 0;
        for (Map.Entry<String, Integer> entry : GameController.COSTS.entrySet()) {
            String name = entry.getKey();
            int cost = entry.getValue();

            Label nameLbl = new Label(name);
            Label costLbl = new Label("$" + cost);
            Button buyBtn = new Button("Buy");
            buyBtn.setOnAction(e -> {
                if (!controller.buyItem(name)) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Not enough money!");
                    err.show();
                }
            });

            content.add(nameLbl, 0, row);
            content.add(costLbl, 1, row);
            content.add(buyBtn, 2, row);
            row++;
        }

        shop.getDialogPane().setContent(content);
        shop.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        shop.show();
    }
}
