package loveletter.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import loveletter.viewmodel.LoginViewModel;

/**
 * Displays nickname input and feedback for server registration.
 */
public class LoginView extends VBox {

    /**
     * Creates the login form and binds it to the supplied view model.
     *
     * @param viewModel the view model managing registration and feedback
     */
    public LoginView(LoginViewModel viewModel){
        Label tittle = new Label("Love Letter");

        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Enter your nickname");
        nicknameField.setMaxWidth(250);

        Button confirmButton = new Button("Connect");
        Label feedbackLabel = new Label();

        nicknameField.disableProperty().bind(viewModel.activeProperty());
        confirmButton.disableProperty().bind(viewModel.activeProperty());

        feedbackLabel.textProperty().bind(viewModel.feedbackProperty());

        confirmButton.setOnAction(event ->
            viewModel.confirmNickname(nicknameField.getText())

        );

        setSpacing(15);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(30));

        getChildren().addAll(tittle, nicknameField, confirmButton, feedbackLabel);
    }
}
