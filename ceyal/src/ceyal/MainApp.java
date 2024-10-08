package ceyal;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.layout.Pane;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class MainApp extends Application {
    private TableView<EventLog> tableView;
    private ObservableList<EventLog> logData;
    private Pane petriNetPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Process Mining Software");

        logData = FXCollections.observableArrayList();
        tableView = new TableView<>(logData);

        // Setting up columns for the table
        TableColumn<EventLog, String> eventColumn = new TableColumn<>("Event");
        eventColumn.setCellValueFactory(cellData -> cellData.getValue().eventProperty());

        TableColumn<EventLog, String> timestampColumn = new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(cellData -> cellData.getValue().timestampProperty());

        tableView.getColumns().add(eventColumn);
        tableView.getColumns().add(timestampColumn);

        TextField filterField = new TextField();
        filterField.setPromptText("Filter Events...");
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            tableView.setItems(logData.filtered(log -> log.eventProperty().get().toLowerCase().contains(newValue.toLowerCase())));
        });

        Button loadButton = new Button("Load Event Log");
        loadButton.setOnAction(e -> loadEventLog(primaryStage));

        Button analyzeButton = new Button("Analyze Event Log");
        analyzeButton.setOnAction(e -> performAnalysis());

        Button visualizeButton = new Button("Visualize Process");
        visualizeButton.setOnAction(e -> visualizePetriNet());

        VBox buttonBox = new VBox(loadButton, analyzeButton, visualizeButton);
        buttonBox.setSpacing(10);

        petriNetPane = new Pane(); // Create a Pane for Petri net visualization
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(tableView);
        borderPane.setBottom(buttonBox);
        borderPane.setTop(filterField);
        borderPane.setRight(petriNetPane); // Place Pane to the right

        Scene scene = new Scene(borderPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadEventLog(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Event Log File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            readEventLog(file);
        }
    }

    private void readEventLog(File file) {
        logData.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    logData.add(new EventLog(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performAnalysis() {
        ProcessMiningAnalysis analysis = new ProcessMiningAnalysis();

        // Process Discovery
        Map<String, Integer> discoveredProcess = analysis.processDiscovery(logData);
        StringBuilder discoveryResults = new StringBuilder("Process Discovery Results:\n");
        discoveredProcess.forEach((event, count) -> discoveryResults.append(event).append(": ").append(count).append("\n"));

        // Conformance Checking
        boolean isConformant = analysis.conformanceCheck(logData, "Start,Process,End");
        String conformanceMessage = isConformant ? "The process log conforms to the expected model." : "The process log does not conform to the expected model.";

        // Performance Mining
        double averageDuration = analysis.calculateAverageEventDuration(logData);

        // Display Results
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Process Mining Analysis Results");
        alert.setHeaderText(null);
        alert.setContentText(discoveryResults.toString() + conformanceMessage + "\nAverage Event Duration: " + averageDuration);
        alert.showAndWait();
    }

    private void visualizePetriNet() {
        petriNetPane.getChildren().clear(); // Clear previous visualization

        double xOffset = 150; // Starting x position for the first place/transition
        double yOffset = 100; // Starting y position for the first place

        // Draw Places and Transitions based on event logs
        for (int i = 0; i < logData.size(); i++) {
            EventLog log = logData.get(i);
            String eventName = log.eventProperty().get();

            // Create a place (circle) for each event
            Circle place = new Circle(20, Color.LIGHTGREEN);
            place.setCenterX(xOffset);
            place.setCenterY(yOffset);
            petriNetPane.getChildren().add(place);

            // Create a transition (rectangle) for each event
            Rectangle transition = new Rectangle(xOffset - 30, yOffset + 40, 60, 20);
            transition.setFill(Color.LIGHTBLUE);
            petriNetPane.getChildren().add(transition);

            // Create lines (arcs) between places and transitions
            Line arc1 = new Line(xOffset, yOffset + 20, xOffset, yOffset + 40);
            petriNetPane.getChildren().add(arc1);

            // Add event name as text label
            Label eventLabel = new Label(eventName);
            eventLabel.setLayoutX(xOffset - 20);
            eventLabel.setLayoutY(yOffset + 45);
            petriNetPane.getChildren().add(eventLabel);

            // Set next position for the next event
            yOffset += 80; // Adjust spacing for next event (vertically)
        }

        // Connect last place to a final place (optional)
        Circle finalPlace = new Circle(20, Color.LIGHTCORAL);
        finalPlace.setCenterX(xOffset);
        finalPlace.setCenterY(yOffset);
        petriNetPane.getChildren().add(finalPlace);

        // Add final place label
        Label finalLabel = new Label("End");
        finalLabel.setLayoutX(xOffset - 10);
        finalLabel.setLayoutY(yOffset + 5);
        petriNetPane.getChildren().add(finalLabel);
    }
}
