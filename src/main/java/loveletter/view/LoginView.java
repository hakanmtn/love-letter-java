package loveletter.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import loveletter.viewmodel.LoginViewModel;

public class LoginView extends VBox {

    public LoginView(LoginViewModel viewModel){
        Label tittle = new Label("Love Letter");

        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Enter your nickname");
        nicknameField.setMaxWidth(250);

        Button confirmButton = new Button("Confirm");
        Label feedbackLabel = new Label();

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
