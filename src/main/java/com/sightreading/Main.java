package com.sightreading;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Scene scene;
    public static String playerName = null;
    public static String selectedSongId = "twinkle"; 

    @Override
    public void start(Stage primaryStage) throws Exception {
        scene = new Scene(loadFXML("home"), 1200, 900); 

        primaryStage.setTitle("Sight Reading Game");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/sightreading/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // getter for scene, to set root from other controllers
    public static Scene getScene() {
        return scene;
    }
}