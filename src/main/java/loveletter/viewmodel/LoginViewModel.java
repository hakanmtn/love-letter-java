package loveletter.viewmodel;

import javafx.application.Platform;
import javafx.beans.property.*;
import loveletter.client.ServerConnection;
import loveletter.protocol.GameState;
import loveletter.protocol.GameStateCodec;

import java.io.IOException;

/**
 * Coordinates nickname registration with the server.
 *
 * <p>Public methods must be called on the JavaFX application thread.
 * Network operations run on a background thread.
 */
public class LoginViewModel {


    private final ReadOnlyStringWrapper feedback = new ReadOnlyStringWrapper("");

    private final ReadOnlyBooleanWrapper active = new ReadOnlyBooleanWrapper(false);

    private ServerConnection connection;
    private boolean closed;
    private static final String GAME_STATE_PREFIX = "GAME_STATE ";
    private final GameStateCodec gameStateCodec = new GameStateCodec();

    private final ReadOnlyObjectWrapper<GameState> gameState  = new ReadOnlyObjectWrapper<>();


    /**
     * Creates a view model with empty feedback.
     */
    public LoginViewModel(){

    }

    /**
     * Starts a connection attempt using the supplied nickname.
     *
     * @param input the nickname input; may be null
     */
    public void confirmNickname(String input){
        if(closed || active.get()){
            return;
        }

        String nickname = input == null ? "" : input.trim();

        if (nickname.isBlank()){
            feedback.set("Please enter a nickname.");
            return;
        }

        if(nickname.contains("\n") || nickname.contains("\r")) {
            feedback.set("Nickname must not contain line breaks.");
            return;
        }

        ServerConnection attempt = new ServerConnection();
        connection = attempt;

        active.set(true);
        feedback.set("Connecting...");

        Thread networkThread = new Thread(() -> runConnection(attempt,nickname), "gui-server-connection");

        networkThread.setDaemon(true);
        networkThread.start();
    }


    /**
     * Runs communication and reports its final outcome.
     */
    private void runConnection(ServerConnection attempt, String nickname){
        String result;

        try(attempt){
            result = communicate(attempt, nickname);
        }catch (IOException exception){
            result = "Connection failed or interrupted: "
                    + exception.getMessage();
        }

        String finalResult = result;

        Platform.runLater(() -> {
            if(closed || connection != attempt){
                return;
            }

            connection = null;
            gameState.set(null);
            active.set(false);
            feedback.set(finalResult);
        });
    }

    /**
     * Registers the nickname and continues reading until disconnection.
     */
    private String communicate(ServerConnection attempt, String nickname) throws IOException{
        attempt.connect("localhost", 5500);

        String prompt = attempt.readMessage();

        if(prompt == null){
            return "Server closed the connection before login.";
        }

        if(!prompt.trim().equals("Enter your nickname:")){
            return "Unexpected server response: " + prompt;
        }

        attempt.sendMessage(nickname);

        boolean registered = false;
        String message;

        while((message = attempt.readMessage()) != null) {
            if(!registered) {
                if (message.equals("Nickname already in use. Enter another nickname:")){
                    return "Nickname already in use. Please try another.";
                }

                if(message.equals("Welcome, " + nickname + "!")){
                    registered = true;
                    attempt.sendMessage("/subscribe-state");

                    Platform.runLater(() -> {
                        if(!closed && connection == attempt) {
                            feedback.set("Welcome, " + nickname + "!");
                        }
                    });
                }

            }

            if(registered && message.startsWith(GAME_STATE_PREFIX)){
                String json = message.substring(GAME_STATE_PREFIX.length());

                GameState state;

                try {
                    state = gameStateCodec.decode(json);
                }catch (RuntimeException exception){
                    throw new IOException("Invalid game state received.", exception);
                }

                Platform.runLater(() -> {
                    if(!closed && connection == attempt){
                        gameState.set(state);
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
  public ReadOnlyBooleanProperty activeProperty(){
      return active.getReadOnlyProperty();
  }

    /**
     * Returns the latest received game state.
     *
     * @return the read-only property; its value is null
     *         when no current snapshot is available
     */
  public ReadOnlyObjectProperty<GameState> gameStateProperty(){
      return gameState.getReadOnlyProperty();
  }

    /**
     * Permanently closes this view model and its connection.
     */
  public void close(){
      closed = true;

      ServerConnection current = connection;
      connection = null;
      gameState.set(null);
      active.set(false);

      if(current != null){
          try{
              current.close();
          }catch(IOException exception){
              System.err.println("Could not connection: " + exception.getMessage());
          }
      }
  }
}
