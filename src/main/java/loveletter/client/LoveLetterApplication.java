package loveletter.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import loveletter.view.LoginView;
import loveletter.viewmodel.LoginViewModel;

/**
 * Entry point for the graphical Love Letter client.
 */
public class LoveLetterApplication extends Application {

    /**
     * Creates the application instance used by JavaFX.
     */
    public LoveLetterApplication(){

    }

    /**
     * Creates and displays the login window.
     *
     * @param stage the primary application window
     */
    @Override
    public void start(Stage stage){
        LoginViewModel viewModel = new LoginViewModel();
        LoginView view = new LoginView(viewModel);

        Scene scene = new Scene(view, 900,600);

        stage.setTitle("Love Letter");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args){
        launch(args);
    }
}
