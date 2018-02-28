package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("ImpedanceAnalyser");
        Scene mainScene = new Scene(root);
        mainScene.setFill(Color.DARKGREY);
        primaryStage.setScene(mainScene);
        primaryStage.setMaximized(true);
        Image appIcon = new Image("resources/ia_icon.png");
        primaryStage.getIcons().add(appIcon);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
