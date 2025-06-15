package org.example.eventbookingsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.eventbookingsystem.utilities.DBUtil;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DBUtil.initializeDatabase();
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/org/example/eventbookingsystem/login_signup.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Event Booking System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
