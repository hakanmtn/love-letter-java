package loveletter.viewmodel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.property.*;
import loveletter.client.ServerConnection;
import loveletter.model.CardType;
import loveletter.protocol.GamePhase;
import loveletter.protocol.GameState;
import loveletter.protocol.GameStateCodec;

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

  /** Creates a view model with empty feedback. */
  public LoginViewModel() {

    gameState.addListener((observable, oldState, newState) ->
            selectedCard.set(null));
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
