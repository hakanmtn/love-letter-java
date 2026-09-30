package loveletter.viewmodel;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.property.*;
import loveletter.client.ServerConnection;
import loveletter.model.CardType;
import loveletter.protocol.GamePhase;
import loveletter.protocol.GameState;
import loveletter.protocol.GameStateCodec;
import loveletter.protocol.PlayerState;

/**
 * Coordinates nickname registration with the server.
 *
 * <p>Public methods must be called on the JavaFX application thread. Network operations run on a
 * background thread.
 */
public class LoginViewModel {

  private final ReadOnlyStringWrapper feedback = new ReadOnlyStringWrapper("");

  private final ReadOnlyBooleanWrapper active = new ReadOnlyBooleanWrapper(false);

  private ServerConnection connection;
  private boolean closed;
  private static final String GAME_STATE_PREFIX = "GAME_STATE ";
  private final GameStateCodec gameStateCodec = new GameStateCodec();

  private final ReadOnlyObjectWrapper<GameState> gameState = new ReadOnlyObjectWrapper<>();

  private final ExecutorService commandExecutor =
      Executors.newSingleThreadExecutor(
          task -> {
            Thread thread = new Thread(task, "gui-server-commands");
            thread.setDaemon(true);
            return thread;
          });

  private final ReadOnlyObjectWrapper<CardType> selectedCard = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyStringWrapper selectedTarget = new ReadOnlyStringWrapper();
  private final ReadOnlyObjectWrapper<CardType> selectedGuess = new ReadOnlyObjectWrapper<>();

  private static final String REVEALED_CARD_PREFIX = "Viewed card: ";

  private final ReadOnlyStringWrapper revealedCard = new ReadOnlyStringWrapper();

  /** Creates a view model with empty feedback. */
  public LoginViewModel() {

    gameState.addListener(
        (observable, oldState, newState) -> {
          selectedTarget.set(null);
          selectedCard.set(null);
          selectedGuess.set(null);

          if (newState == null || newState.phase() != GamePhase.ROUND_IN_PROGRESS) {
            revealedCard.set("");
          }
        });

    selectedCard.addListener(
        (observable, oldCard, newCard) -> {
          selectedTarget.set(null);
          selectedGuess.set(null);
        });


  }

  /**
   * Starts a connection attempt using the supplied nickname.
   *
   * @param input the nickname input; may be null
   */
  public void confirmNickname(String input) {
    if (closed || active.get()) {
      return;
    }

    String nickname = input == null ? "" : input.trim();

    if (nickname.isBlank()) {
      feedback.set("Please enter a nickname.");
      return;
    }

    if (nickname.contains("\n") || nickname.contains("\r")) {
      feedback.set("Nickname must not contain line breaks.");
      return;
    }

    ServerConnection attempt = new ServerConnection();
    connection = attempt;

    active.set(true);
    feedback.set("Connecting...");

    Thread networkThread =
        new Thread(() -> runConnection(attempt, nickname), "gui-server-connection");

    networkThread.setDaemon(true);
    networkThread.start();
  }

  /** Runs communication and reports its final outcome. */
  private void runConnection(ServerConnection attempt, String nickname) {
    String result;

    try (attempt) {
      result = communicate(attempt, nickname);
    } catch (IOException exception) {
      result = "Connection failed or interrupted: " + exception.getMessage();
    }

    String finalResult = result;

    Platform.runLater(
        () -> {
          if (closed || connection != attempt) {
            return;
          }

          connection = null;
          gameState.set(null);
          active.set(false);
          feedback.set(finalResult);
        });
  }

  /** Registers the nickname and continues reading until disconnection. */
  private String communicate(ServerConnection attempt, String nickname) throws IOException {
    attempt.connect("localhost", 5500);

    String prompt = attempt.readMessage();

    if (prompt == null) {
      return "Server closed the connection before login.";
    }

    if (!prompt.trim().equals("Enter your nickname:")) {
      return "Unexpected server response: " + prompt;
    }

    attempt.sendMessage(nickname);

    boolean registered = false;
    String message;

    while ((message = attempt.readMessage()) != null) {
      if (!registered) {
        if (message.equals("Nickname already in use. Enter another nickname:")) {
          return "Nickname already in use. Please try another.";
        }

        if (message.equals("Welcome, " + nickname + "!")) {
          registered = true;
          attempt.sendMessage("/subscribe-state");

          Platform.runLater(
              () -> {
                if (!closed && connection == attempt) {
                  feedback.set("Welcome, " + nickname + "!");
                }
              });
        }
      }

      if (registered && message.startsWith(GAME_STATE_PREFIX)) {
        String json = message.substring(GAME_STATE_PREFIX.length());

        GameState state;

        try {
          state = gameStateCodec.decode(json);
        } catch (RuntimeException exception) {
          throw new IOException("Invalid game state received.", exception);
        }

        Platform.runLater(
            () -> {
              if (!closed && connection == attempt) {
                gameState.set(state);
              }
            });
      } else if (registered && message.startsWith(REVEALED_CARD_PREFIX)) {
        String cardName = message.substring(REVEALED_CARD_PREFIX.length());

        Platform.runLater(
            () -> {
              if (!closed && connection == attempt) {
                revealedCard.set("Last card revealed by Priest: " + cardName);
              }
            });

      } else if (registered) {
        String serverMessage = message;

        Platform.runLater(
            () -> {
              if (!closed && connection == attempt) {
                feedback.set(serverMessage);
              }
            });
      }
    }
    return "Server closed the connection.";
  }

  /**
   * Returns the feedback displayed by the login view.
   *
   * @return the read-only feedback property
   */
  public ReadOnlyStringProperty feedbackProperty() {
    return feedback.getReadOnlyProperty();
  }

  /**
   * Indicates whether a connection attempt or session is active.
   *
   * @return the read-only active property
   */
  public ReadOnlyBooleanProperty activeProperty() {
    return active.getReadOnlyProperty();
  }

  /**
   * Returns the latest received game state.
   *
   * @return the read-only property; its value is null when no current snapshot is available
   */
  public ReadOnlyObjectProperty<GameState> gameStateProperty() {
    return gameState.getReadOnlyProperty();
  }

  /**
   * Returns the currently selected card type.
   *
   * @return the read-only property; null means no card is selected
   */
  public ReadOnlyObjectProperty<CardType> selectedCardProperty() {
    return selectedCard.getReadOnlyProperty();
  }

  /**
   * Returns the selected target name.
   *
   * @return the read-only property; null means no target is selected
   */
  public ReadOnlyStringProperty selectedTargetProperty() {
    return selectedTarget.getReadOnlyProperty();
  }

  /**
   * Returns the last card revealed by Priest during the current round.
   *
   * @return the read-only reveal text
   */
  public ReadOnlyStringProperty revealedCardProperty() {
    return revealedCard.getReadOnlyProperty();
  }

  /**
   * Returns the selected card guess as a read-only property.
   *
   * @return the selected guess property
   */
  public ReadOnlyObjectProperty<CardType> selectedGuessProperty() {
    return selectedGuess.getReadOnlyProperty();
  }

  /**
   * Requests game creation when no game exists.
   *
   * <p>Must be called on the JavaFX application thread.
   */
  public void createGame() {

    GameState state = gameState.get();

    if (state == null || state.phase() != GamePhase.NO_GAME) {
      return;
    }
    sendCommand("/create");
  }

  /**
   * Indicates whether the current user can request to join the game.
   *
   * @return true if the game is waiting, has space, and the current user has not joined
   */
  public boolean canJoinGame() {

    GameState state = gameState.get();
    if (state == null
        || state.phase() != GamePhase.WAITING_FOR_PLAYERS
        || state.players().size() >= 4) {
      return false;
    }

    return state.players().stream()
        .noneMatch(player -> player.name().equals(state.recipientName()));
  }

  /**
   * Requests participation in the current game.
   *
   * <p>Must be called on the JavaFX application thread.
   */
  public void joinGame() {
    if (!canJoinGame()) {
      return;
    }

    sendCommand("/join");
  }

  /**
   * Indicates whether the current user can request to start the game.
   *
   * @return true if at least two players have joined, the game is waiting, and the current user
   *     participates
   */
  public boolean canStartGame() {
    GameState state = gameState.get();

    if (state == null
        || state.phase() != GamePhase.WAITING_FOR_PLAYERS
        || state.players().size() < 2) {
      return false;
    }

    return state.players().stream().anyMatch(player -> player.name().equals(state.recipientName()));
  }

  /**
   * Requests the start of the current game.
   *
   * <p>Must be called on the JavaFX application thread.
   */
  public void startGame() {
    if (!canStartGame()) {
      return;
    }

    sendCommand("/start");
  }

  /**
   * Indicates whether the current user can request the next round.
   *
   * @return true if the round is over and the current user participates
   */
  public boolean canStartNextRound() {
    GameState state = gameState.get();

    if (state == null || state.phase() != GamePhase.ROUND_OVER) {
      return false;
    }

    return state.players().stream()
        .anyMatch(player -> player.name().equals((state.recipientName())));
  }

  /**
   * Requests the next round.
   *
   * <p>Must be called on the JavaFX application thread.
   */
  public void startNextRound() {
    if (!canStartNextRound()) {
      return;
    }
    sendCommand("/next");
  }

  /**
   * Selects a card from the current hand or clears the selection.
   *
   * <p>Must be called on the JavaFX application thread.
   *
   * @param card the card type to select, or null to clear the selection
   */
  public void selectCard(CardType card) {
    if (card == null) {
      selectedCard.set(null);
      return;
    }

    GameState state = gameState.get();

    if (state == null
        || state.phase() != GamePhase.ROUND_IN_PROGRESS
        || !state.recipientName().equals(state.currentPlayerName())
        || !state.ownHand().contains(card)) {
      return;
    }

    selectedCard.set(card);
  }

  /**
   * Returns the available targets for the selected card.
   *
   * @return the names of eligible target players
   */
  public List<String> availableTargetNames() {
    GameState state = gameState.get();
    CardType card = selectedCard.get();

    if (state == null
        || state.phase() != GamePhase.ROUND_IN_PROGRESS
        || !state.recipientName().equals(state.currentPlayerName())) {
      return List.of();
    }

    if (!usesOpponentTarget(card) && card != CardType.PRINCE) {
      return List.of();
    }

    return state.players().stream()
        .filter(player -> !player.eliminated())
        .filter(player -> !player.protectedFromEffects())
        .map(PlayerState::name)
        .filter(name -> card == CardType.PRINCE
                || !name.equals(state.recipientName()))
        .toList();
  }

  /**
   * Indicates whether the selected card can be submitted with the currently available input.
   *
   * @return true if the selected card and target are ready to send
   */
  public boolean canPlaySelectedCard() {
    GameState state = gameState.get();
    CardType card = selectedCard.get();

    if (closed
        || state == null
        || card == null
        || state.phase() != GamePhase.ROUND_IN_PROGRESS
        || !state.recipientName().equals(state.currentPlayerName())
        || !state.ownHand().contains(card)) {
      return false;
    }

    boolean mustPlayCountess = state.ownHand().contains(CardType.COUNTESS)
            && (state.ownHand().contains(CardType.KING)
            || state.ownHand().contains(CardType.PRINCE));

    if(mustPlayCountess && card != CardType.COUNTESS) {
        return false;
    }

    if(card == CardType.PRINCE) {
      String target = selectedTarget.get();

      return target != null && availableTargetNames().contains(target);
    }

    if(card == CardType.GUARD) {
      List<String> targets = availableTargetNames();

      if(targets.isEmpty()) {
        return true;
      }

      String target = selectedTarget.get();
      CardType guess = selectedGuess.get();

      return target != null && targets.contains(target) && guess != null && guess != CardType.GUARD;
    }

    if (usesOpponentTarget(card)) {
      List<String> targets = availableTargetNames();
      String target = selectedTarget.get();

      return targets.isEmpty() || (target != null && targets.contains(target));
    }

    return card == CardType.HANDMAID || card == CardType.COUNTESS || card == CardType.PRINCESS;
  }

  /**
   * Requests play of the selected card with its required target.
   *
   * <p>Must be called on the JavaFX application thread.
   */
  public void playSelectedCard() {
    if (!canPlaySelectedCard()) {
      return;
    }

    CardType card = selectedCard.get();
    String command = "/play " + card.name();

    if ( card == CardType.PRINCE||(usesOpponentTarget(card) && !availableTargetNames().isEmpty())) {
      command += " " + selectedTarget.get();
    }

    if(card == CardType.GUARD && !availableTargetNames().isEmpty()) {
      command += " " + selectedGuess.get().name();
    }

    sendCommand(command);
    selectedCard.set(null);
  }

  /**
   * Selects an eligible target or clears the target selection.
   *
   * <p>Must be called on the JavaFX application thread.
   *
   * @param targetName the target name, or null to clear the selection
   */
  public void selectTarget(String targetName) {
    if (targetName == null) {
      selectedCard.set(null);
      return;
    }

    if (!availableTargetNames().contains(targetName)) {
      return;
    }

    selectedTarget.set(targetName);
  }

  /**
   * Checks whether the card uses an opposing player as its target.
   *
   * @param card the card to check
   * @return true for Guard, Priest, Baron, or King
   */
  private boolean usesOpponentTarget(CardType card) {
    return card == CardType.GUARD || card == CardType.PRIEST || card == CardType.BARON || card == CardType.KING;
  }

  /**
   * Selects a card guess for the Guard.
   *
   * @param guess the guessed card, or null to clear the selection
   */
  public void selectGuess(CardType guess){
    if(guess == null){
      selectedGuess.set(null);
      return;
    }

    if(selectedCard.get() != CardType.GUARD || guess == CardType.GUARD){
      return;
    }

    selectedGuess.set(guess);
  }
  /**
   * Queues a command for the current connection.
   *
   * <p>Must be called on the JavaFX application thread.
   *
   * @param command the command to send
   */
  private void sendCommand(String command) {
    if (closed || connection == null || gameState.get() == null) {
      return;
    }

    ServerConnection current = connection;

    commandExecutor.execute(
        () -> {
          try {
            current.sendMessage(command);
          } catch (IOException exception) {
            Platform.runLater(
                () -> {
                  if (!closed && connection == current) {
                    feedback.set("Could not send command: " + exception.getMessage());
                  }
                });
          }
        });
  }

  /** Permanently closes this view model and its connection. */
  public void close() {
    closed = true;
    commandExecutor.shutdownNow();

    ServerConnection current = connection;
    connection = null;
    gameState.set(null);
    active.set(false);

    if (current != null) {
      try {
        current.close();
      } catch (IOException exception) {
        System.err.println("Could not connection: " + exception.getMessage());
      }
    }
  }
}
