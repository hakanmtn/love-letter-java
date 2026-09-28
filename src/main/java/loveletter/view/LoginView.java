package loveletter.view;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import loveletter.model.CardType;
import loveletter.protocol.GamePhase;
import loveletter.protocol.GameState;
import loveletter.viewmodel.LoginViewModel;

/** Displays nickname input and feedback for server registration. */
public class LoginView extends VBox {

  /**
   * Creates the login form and binds it to the supplied view model.
   *
   * @param viewModel the view model managing registration and feedback
   */
  public LoginView(LoginViewModel viewModel) {
    Label tittle = new Label("Love Letter");

    TextField nicknameField = new TextField();
    nicknameField.setPromptText("Enter your nickname");
    nicknameField.setMaxWidth(250);

    Button confirmButton = new Button("Connect");
    Label feedbackLabel = new Label();
    Label gamePhaseLabel = new Label();

    HBox handBox = new HBox(12);
    handBox.setAlignment(Pos.CENTER);

    updateHand(handBox, viewModel.gameStateProperty().get(), viewModel);

    viewModel
        .gameStateProperty()
        .addListener((observable, oldState, newState) -> updateHand(handBox, newState, viewModel));

    Label selectedCardLabel = new Label();
    selectedCardLabel
        .textProperty()
        .bind(
            Bindings.createStringBinding(
                () -> {
                  CardType card = viewModel.selectedCardProperty().get();

                  return card == null ? "No card selected" : "Selected card: " + card.name();
                },
                viewModel.selectedCardProperty()));


    Label revealedCardLabel = new Label();
    revealedCardLabel.textProperty().bind(
            viewModel.revealedCardProperty()
    );

    Button createGameButton = new Button("Create Game");

    createGameButton.setOnAction(event -> viewModel.createGame());

    createGameButton
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () -> {
                  GameState state = viewModel.gameStateProperty().get();

                  return state == null || state.phase() != GamePhase.NO_GAME;
                },
                viewModel.gameStateProperty()));

    Button joinGameButton = new Button("Join game");

    joinGameButton.setOnAction(EVENT -> viewModel.joinGame());

    joinGameButton
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () -> !viewModel.canJoinGame(), viewModel.gameStateProperty()));

    Button startGameButton = new Button("Start game");

    startGameButton.setOnAction(event -> viewModel.startGame());

    startGameButton
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () -> !viewModel.canStartGame(), viewModel.gameStateProperty()));

    Button nextRoundButton = new Button("Next round");

    nextRoundButton.setOnAction(event -> viewModel.startNextRound());

    nextRoundButton
        .disableProperty()
        .bind(
            Bindings.createBooleanBinding(
                () -> !viewModel.canStartNextRound(), viewModel.gameStateProperty()));

    nicknameField.disableProperty().bind(viewModel.activeProperty());
    confirmButton.disableProperty().bind(viewModel.activeProperty());

    feedbackLabel.textProperty().bind(viewModel.feedbackProperty());
    gamePhaseLabel
        .textProperty()
        .bind(
            Bindings.createStringBinding(
                () -> {
                  GameState state = viewModel.gameStateProperty().get();

                  if (state == null) {
                    return "";
                  }

                  return "Game phase: " + state.phase();
                },
                viewModel.gameStateProperty()));

    confirmButton.setOnAction(event -> viewModel.confirmNickname(nicknameField.getText()));

    ComboBox<String> targetBox = new ComboBox<>();
    targetBox.setPromptText("Choose a target");
    targetBox.setPrefWidth(200);

    targetBox.valueProperty().addListener(
            (observable,oldTarget, newTarget) -> {
                viewModel.selectTarget(newTarget);
            }
    );

    updateTargets(targetBox, viewModel);

    viewModel.selectedCardProperty().addListener(
            (observable, oldCard, newCard) ->
                    updateTargets(targetBox, viewModel)
    );

    viewModel.gameStateProperty().addListener(
            (observable, oldState, newState) -> {
                updateTargets(targetBox, viewModel);
            }
    );

    Button playCardButton = new Button("Play card");

    playCardButton.setOnAction(event -> viewModel.playSelectedCard());

    playCardButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !viewModel.canPlaySelectedCard(),
                        viewModel.gameStateProperty(),
                        viewModel.selectedCardProperty(),
                        viewModel.selectedTargetProperty()
                )
    );



    setSpacing(15);
    setAlignment(Pos.CENTER);
    setPadding(new Insets(30));

    getChildren()
        .addAll(
            tittle,
            nicknameField,
            confirmButton,
            feedbackLabel,
            revealedCardLabel,
            gamePhaseLabel,
            handBox,
            selectedCardLabel,
            targetBox,
            playCardButton,
            createGameButton,
            joinGameButton,
            startGameButton,
            nextRoundButton);
  }

/**
 * Refreshes the available target names and clears the previous choice.
 *
 * @param targetBox the target selection control
 * @param viewModel the view model providing eligible targets
 */
private void updateTargets(ComboBox<String> targetBox, LoginViewModel viewModel) {
    targetBox.getSelectionModel().clearSelection();
    targetBox.setValue(null);

    targetBox.getItems().setAll(viewModel.availableTargetNames());
    targetBox.setDisable(targetBox.getItems().isEmpty());

}




  /**
   * Rebuilds the hand display using the recipient's own cards.
   *
   * @param handBox the container for the card labels
   * @param state the current snapshot, or null if unavailable
   * @param viewModel the view model receiving card selections
   */
  private void updateHand(HBox handBox, GameState state, LoginViewModel viewModel) {
    handBox.getChildren().clear();

    if (state == null || state.ownHand().isEmpty()) {
      return;
    }

    ToggleGroup selectionGroup = new ToggleGroup();

    selectionGroup
        .selectedToggleProperty()
        .addListener(
            (observable, oldToggle, newToggle) -> {
              CardType selected = newToggle == null ? null : (CardType) newToggle.getUserData();
              viewModel.selectCard(selected);
            });

    boolean ownTurn =
        state.phase() == GamePhase.ROUND_IN_PROGRESS
            && state.recipientName().equals(state.currentPlayerName());

    for (CardType card : state.ownHand()) {
      ToggleButton cardButton = new ToggleButton(card.name());

      cardButton.setToggleGroup(selectionGroup);
      cardButton.setUserData(card);
      cardButton.setDisable(!ownTurn);

      handBox.getChildren().add(cardButton);
    }
  }
}
