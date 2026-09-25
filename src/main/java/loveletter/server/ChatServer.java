package loveletter.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import loveletter.model.CardType;
import loveletter.model.Game;
import loveletter.model.GameRound;
import loveletter.model.Player;
import protocol.GamePhase;
import protocol.GameState;
import protocol.PlayerState;

/**
 * Provides a TCP chat server with private messaging and
 * support for one shared Love Letter game at a time.
 *
 * <p>Each accepted connection is handled by a dedicated
 * {@link ClientHandler} thread.
 * Game commands connect client requests to the game model.
 */
public class ChatServer {
  private static final int DEFAULT_PORT = 5500;
  private final int port;
  private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
  private final Set<String> nicknames = ConcurrentHashMap.newKeySet();
  private Game game;
  private final Map<ClientHandler, Player> gamePlayers = new HashMap<>();

  /**
   * Creates a server configured to listen on the specified port.
   *
   * <p>The listening socket is opened by {@link #start()}.
   *
   * @param port the TCP port to bind to, from 0 to 65535;
   *             zero requests an automatically assigned port
   */
  public ChatServer(int port) {
    this.port = port;
  }

  /**
   * Creates a server using the supplied game instance.
   *
   * <p>The game is shared with the caller, not copied.
   * Client-to-player mappings are initially empty.
   *
   * @param port the TCP port to bind to, from 0 to 65535;
   *             zero requests an automatically assigned port
   * @param game the game instance to use
   * @throws NullPointerException if game is null
   */
  ChatServer(int port, Game game) {
    this(port);
    this.game = Objects.requireNonNull(game, "game must not be null");
  }

  /**
   * Opens the listening socket and accepts client connections,
   * starting a dedicated handler thread for each connection.
   *
   * <p>This method blocks while waiting for connections.
   * An IOException is logged and ends the accept loop.
   * The listening socket is closed when the try block is exited.
   *
   * <p>Existing client connections are not explicitly closed
   * by this method when the accept loop ends.
   *
   * @throws IllegalArgumentException if the configured port
   *         is outside the range of 0 to 65535
   */
  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Chat server started on port " + port);

      while (true) {
        Socket clientSocket = serverSocket.accept();

        ClientHandler clientHandler = new ClientHandler(clientSocket, this);

        Thread clientThread = new Thread(clientHandler);
        clientThread.start();
      }
    } catch (IOException e) {
      System.err.println("Server Error: " + e.getMessage());
    }
  }

  /**
   * Adds a client handler to the recipients of server broadcasts.
   *
   * <p>This method does not register a nickname or join a game.
   *
   * @param clientHandler the client handler to add
   */
  void addClient(ClientHandler clientHandler) {
    clients.add(clientHandler);
  }

  /**
   * Removes a client handler from the broadcast recipients
   * and removes its player mapping, if present.
   *
   * <p>If the client was a game participant, discards the current
   * game, clears all client-to-player mappings and announces
   * the game closure to the remaining clients.
   * Removing a non-participant leaves the game unchanged.
   *
   * <p>This method does not close the client socket or
   * unregister the nickname.
   *
   * @param clientHandler the client handler to remove
   */
  synchronized void removeClient(ClientHandler clientHandler) {

    clients.remove(clientHandler);

    Player leavingPlayer = gamePlayers.remove(clientHandler);

    if (leavingPlayer == null) {
      return;
    }

    game = null;
    gamePlayers.clear();

    broadcast(
        "The game was closed because "
            + leavingPlayer.getName()
            + " disconnected. Use /create to start a new game.");
  }

  /**
   * Sends a message to all handlers currently in the client list.
   *
   * <p>Recipients include clients who have not joined the game.
   * No sender is excluded.
   *
   * @param message the message to send
   */
  void broadcast(String message) {
    for (ClientHandler client : clients) {
      client.sendMessage(message);
    }
  }

  /**
   * Attempts to reserve a nickname using case-insensitive uniqueness.
   *
   * <p>Names are normalized to lowercase using Locale.ROOT.
   * Leading and trailing whitespace is not removed by this method.
   *
   * @param nickname the nickname to reserve
   * @return true if the nickname was reserved; false if it is null,
   *         blank or already registered ignoring case
   */
  boolean registerNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return false;
    }

    String normalizedName = nickname.toLowerCase(Locale.ROOT);

    return nicknames.add(normalizedName);
  }

  /**
   * Releases a nickname using the same lowercase normalization
   * as registration.
   *
   * <p>Does nothing if the nickname is not registered.
   *
   * @param nickname the nickname to release; must not be null
   * @throws NullPointerException if nickname is null
   */
  void unregisterNickname(String nickname) {
    String normalizedNickname = nickname.toLowerCase(Locale.ROOT);
    nicknames.remove(normalizedNickname);
  }

  /**
   * Sends a message to all handlers in the client list except
   * the specified handler.
   *
   * <p>The excluded handler is identified by object identity.
   *
   * @param message the message to send
   * @param excludedClient the handler to exclude,
   *                       or null to exclude none
   */
  void broadcastToOthers(String message, ClientHandler excludedClient) {
    for (ClientHandler client : clients) {
      if (client != excludedClient) {
        client.sendMessage(message);
      }
    }
  }

  /**
   * Sends a private message to a client selected by nickname.
   *
   * <p>Recipient lookup ignores case and trims surrounding
   * whitespace from the requested name.
   * Neither client needs to participate in a game.
   *
   * <p>The recipient receives the message with the sender's nickname.
   * A separate sender confirmation is sent unless sender and
   * recipient are the same handler. Other clients receive nothing.
   *
   * <p>A null or blank recipient name, a null or blank message,
   * or an unknown recipient produces an error response to the sender.
   * The message body is otherwise preserved unchanged.
   *
   * <p>The sender confirmation does not acknowledge successful
   * receipt by the recipient.
   *
   * @param sender the sending client handler; must not be null
   * @param recipientName the nickname of the intended recipient
   * @param message the private message body
   */
  synchronized void sendDirectMessage(ClientHandler sender, String recipientName, String message) {
    if (recipientName == null || recipientName.isBlank()) {
      sender.sendMessage("Recipient must not be empty.");
      return;
    }

    if (message == null || message.isBlank()) {
      sender.sendMessage("Private message must not be empty.");
      return;
    }

    String searchedName = recipientName.trim();

    ClientHandler recipient =
        clients.stream()
            .filter(client -> searchedName.equalsIgnoreCase((client.getNickname())))
            .findFirst()
            .orElse(null);

    if (recipient == null) {
      sender.sendMessage("Unknown recipient: " + searchedName);
      return;
    }

    recipient.sendMessage("[Private from " + sender.getNickname() + "] " + message);

    if (recipient != sender) {
      sender.sendMessage("[Private to " + recipient.getNickname() + "] " + message);
    }
  }

  /**
   * Processes slash commands received from a client.
   *
   * <p>Supports help, private messages, game creation, joining,
   * starting, hand inspection, card play, scores and subsequent rounds.
   * Command names are matched without regard to case.
   *
   * <p>Input is trimmed before processing. Messages that do not
   * start with a slash are left for ordinary chat handling.
   * Unknown or rejected commands produce a response to the sender.
   *
   * <p>Calls are synchronized on this server instance.
   *
   * @param sender the client issuing the command; must not be null
   * @param message the input message
   * @return true if the input starts with a slash and is handled
   *         as a command, including unknown or rejected commands;
   *         false if it is ordinary chat input
   * @throws NullPointerException if message is null
   */
  synchronized boolean handleCommand(ClientHandler sender, String message) {
    String command = message.trim();

    if (!command.startsWith("/")) {
      return false;
    }
    if ("/msg".equalsIgnoreCase(command.split("\\s+", 2)[0])) {
      String[] parts = command.split("\\s+", 3);

      if (parts.length < 3) {
        sender.sendMessage("Usage: /msg RECIPIENT MESSAGE");
        return true;
      }

      sendDirectMessage(sender, parts[1], parts[2]);
      return true;
    }

    if ("/help".equalsIgnoreCase(command)) {
      sender.sendMessage(
          "Available commands: /help, /create, /join, /start, /hand, /score, /help, "
              + "/play CARD. Use bye to disconnect.");
      sender.sendMessage(
          "Play: /play CARD [TARGET]. "
              + "Guard: /play GUARD TARGET GUESS. "
              + "Example: /play GUARD nati KING");

      sender.sendMessage(
                "Private message: /msg RECIPIENT MESSAGE. "
                        + "Example: /msg nati Hallo!");

    } else if ("/create".equalsIgnoreCase(command)) {
      if (game != null) {
        sender.sendMessage("A game already exists.");
        return true;
      }

      game = new Game();
      broadcast("A new Love Letter game has been created.");

    } else if ("/join".equalsIgnoreCase(command)) {
      if (game == null) {
        sender.sendMessage("Create a game first with /create.");
        return true;
      }

      if (gamePlayers.containsKey(sender)) {
        sender.sendMessage("You have already joined the game.");
        return true;
      }

      Player player = new Player(sender.getNickname());

      try {
        game.join(player);
      } catch (IllegalArgumentException | IllegalStateException e) {
        sender.sendMessage("Cannot join game: " + e.getMessage());
        return true;
      }

      gamePlayers.put(sender, player);
      broadcast(player.getName() + " joined the game. Players: " + game.getPlayers().size() + "/4");

    } else if ("/start".equalsIgnoreCase(command)) {
      if (game == null) {
        sender.sendMessage("Create a game first with /create.");
        return true;
      }

      if (!gamePlayers.containsKey(sender)) {
        sender.sendMessage("Join the game first with /join.");
        return true;
      }

      try {
        game.start();
      } catch (IllegalStateException e) {
        sender.sendMessage("Cannot start game: " + e.getMessage());
        return true;
      }

      GameRound round = game.getCurrentRound();
      round.startCurrentTurn();

      broadcast("The Love Letter game has started.");
      broadcast("Current Player: " + round.getCurrentPlayer().getName());

      for (Map.Entry<ClientHandler, Player> entry : gamePlayers.entrySet()) {
        ClientHandler client = entry.getKey();
        Player player = entry.getValue();

        client.sendMessage("Your hand: " + player.getHand());
      }
    } else if ("/hand".equalsIgnoreCase(command)) {
      if (game == null) {
        sender.sendMessage("Create a game first with /create.");
        return true;
      }

      Player player = gamePlayers.get(sender);

      if (player == null) {
        sender.sendMessage("Join the game first with /join.");
        return true;
      }

      if (!game.isStarted()) {
        sender.sendMessage("The game has not started yet.");
        return true;
      }

      if (player.isEliminated()) {
        sender.sendMessage("You are eliminated from this round.");
        return true;
      }

      sender.sendMessage("Your hand: " + player.getHand());
    } else if ("/play".equalsIgnoreCase(command.split("\\s+", 2)[0])) {
      handlePlayCommand(sender, command);

    } else if ("/score".equalsIgnoreCase(command)) {
      if (game == null) {
        sender.sendMessage("Create a game first with /create.");
        return true;
      }

      if (game.getPlayers().isEmpty()) {
        sender.sendMessage("No players have joined the game yet.");
        return true;
      }

      sender.sendMessage("Score:");

      for (Player participant : game.getPlayers()) {
        sender.sendMessage(
            participant.getName()
                + ": "
                + participant.getAffectionTokens()
                + " affection Token(s)");
      }
    } else if ("/next".equalsIgnoreCase(command)) {
      if (game == null) {
        sender.sendMessage("Create a game first with /create");
        return true;
      }

      if (!gamePlayers.containsKey(sender)) {
        sender.sendMessage("Join the game first with /join.");
        return true;
      }

      try {
        game.startNextRound();
      } catch (IllegalStateException e) {
        sender.sendMessage("Cannot start next round: " + e.getMessage());
        return true;
      }

      GameRound round = game.getCurrentRound();
      round.startCurrentTurn();

      broadcast("A new round has started.");
      broadcast("Current player: " + round.getCurrentPlayer().getName());

      for (Map.Entry<ClientHandler, Player> entry : gamePlayers.entrySet()) {
        entry.getKey().sendMessage("Your hand: " + entry.getValue().getHand());
      }
    } else {
      sender.sendMessage("Unknown command. Use /help to see available commands");
    }
    return true;
  }

  /**
   * Processes a card-play command for a game participant.
   *
   * <p>Accepts /play CARD [TARGET], or /play GUARD TARGET GUESS.
   * Resolves card names and the optional target, then delegates
   * rule validation and card effects to the current round.
   *
   * <p>Missing game prerequisites, invalid arguments and rejected
   * plays are reported to the sender.
   * A card revealed by Priest is sent only to the sender.
   * Public play events are broadcast to all registered clients.
   *
   * <p>After a successful play, ends the turn. If the round is over,
   * awards round tokens and announces round and possible game winners.
   * Otherwise, starts the next turn and sends updated hands privately
   * to non-eliminated participants.
   *
   * <p>Called from handleCommand while holding the server monitor.
   *
   * @param sender the client playing a card
   * @param command the trimmed /play command to process
   */
  private void handlePlayCommand(ClientHandler sender, String command) {
    if (game == null) {
      sender.sendMessage("Create a game first with /create.");
      return;
    }

    Player player = gamePlayers.get(sender);

    if (player == null) {
      sender.sendMessage("Join the game first with /join.");
      return;
    }

    if (!game.isStarted()) {
      sender.sendMessage("The game hast not started yet.");
      return;
    }

    GameRound round = game.getCurrentRound();

    if (round.isRoundOver()) {
      sender.sendMessage("This round is over.");
      return;
    }

    String[] parts = command.split("\\s+", 3);

    if (parts.length < 2) {
      sender.sendMessage("Usage: /play CARD [TARGET] [GUESS]");
      return;
    }

    CardType card;
    Player target = null;
    CardType guess = null;
    Optional<CardType> revealedCard;

    try {
      card = parseCardType(parts[1]);

      if (parts.length == 3) {
        String targetName = parts[2].trim();

        if (card == CardType.GUARD) {
          int separator = targetName.lastIndexOf(' ');

          if (separator < 0) {
            throw new IllegalArgumentException("Usage: /play GUARD TARGET GUESS");
          }
          String guessText = targetName.substring(separator + 1);
          targetName = targetName.substring(0, separator).trim();

          guess = parseCardType(guessText);
        }
        String searchedName = targetName;

        target =
            game.getPlayers().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase(searchedName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + searchedName));
      }
      revealedCard = round.playCard(player, card, target, guess);
    } catch (IllegalArgumentException | IllegalStateException e) {
      sender.sendMessage("Cannot play cards: " + e.getMessage());
      return;
    }

    revealedCard.ifPresent(cardType -> sender.sendMessage("Viewed card: " + cardType));

    String playMessage = player.getName() + " played " + card;
    if (target != null) {
      playMessage += " targeting " + target.getName();
    }

    if (guess != null) {
      playMessage += " and guessed " + guess;
    }

    broadcast(playMessage + " .");

    if (card == CardType.GUARD && target != null) {
      if (target.isEliminated()) {
        broadcast(target.getName() + " was eliminated: the guess was correct.");
      } else {
        broadcast("The guess was incorrect. Nobody was eliminated.");
      }
    }

    if (card == CardType.PRINCE && target != null) {
      CardType discardedCard = target.getDiscardPile().getLast();

      broadcast(target.getName() + " discarded " + discardedCard + ".");

      if (target.isEliminated()) {
        broadcast(target.getName() + " was eliminated after discarding PRINCESS.");
      } else {
        broadcast(target.getName() + " drew a replacement card.");
      }
    }

    if (card == CardType.KING && target != null) {
      broadcast(player.getName() + " and " + target.getName() + " swapped their hand cards.");
    }

    if (card == CardType.BARON && target != null) {
      if (player.isEliminated()) {
        broadcast(player.getName() + " was eliminated after comparing hand cards.");
      } else if (target.isEliminated()) {
        broadcast(target.getName() + " was eliminated after comparing hand cards.");
      } else {
        broadcast("The hand values were equal. Nobody was eliminated.");
      }
    }

    if (target == null
        && (card == CardType.GUARD
            || card == CardType.PRIEST
            || card == CardType.BARON
            || card == CardType.KING)) {

      broadcast("No opponent was available. The card had no effect.");
    }

    round.endCurrentTurn();

    if (round.isRoundOver()) {
      List<Player> winners = game.finishCurrentRound();

      String winnersNames =
          winners.stream().map(Player::getName).collect(java.util.stream.Collectors.joining(", "));

      broadcast("Round over. Winners: " + winnersNames);
      broadcast("Each round winner receives one affection token.");

      if (game.isGameOver()) {
        String gameWinnerNames =
            game.getWinners().stream()
                .map(Player::getName)
                .collect(java.util.stream.Collectors.joining(", "));

        broadcast("Game over. Overall winners: " + gameWinnerNames);
        broadcast("Use /score to see the final scores.");

      } else {
        broadcast("Use /next to start the next round.");
      }

      return;
    }

    round.startCurrentTurn();

    broadcast("Current player: " + round.getCurrentPlayer().getName());

    for (Map.Entry<ClientHandler, Player> entry : gamePlayers.entrySet()) {
      Player participant = entry.getValue();

      if (!participant.isEliminated()) {
        entry.getKey().sendMessage("Your hand: " + participant.getHand());
      }
    }
  }

  /**
   * Converts a card name to its corresponding card type.
   *
   * <p>Trims surrounding whitespace and converts the name
   * to uppercase using Locale.ROOT before lookup.
   *
   * @param text the card name to parse
   * @return the matching card type
   * @throws IllegalArgumentException if text is null, blank
   *         or does not name a known card type
   */
  CardType parseCardType(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Card name must not be empty");
    }

    try {
      return CardType.valueOf(text.trim().toUpperCase(Locale.ROOT));

    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Unknown card: "
              + text
              + ". Use GUARD, PRIEST, BARON, HANDMAID, "
              + "PRINCE, KING, COUNTESS or PRINCESS.");
    }
  }

  synchronized GameState createGameState(ClientHandler recipient){
    Objects.requireNonNull(recipient, "recipient must not be null");

    String recipientName = Objects.requireNonNull(recipient.getNickname(), "recipient must have a nickname");

    if(game == null){
      return new GameState(GamePhase.NO_GAME,
              recipientName,
              List.of(),
              null,
              List.of(),
              0,
              List.of(),
              List.of(),
              List.of());

    }

    List<PlayerState> players = game.getPlayers().stream()
            .map(player -> new PlayerState(
                    player.getName(),
                    player.getAffectionTokens(),
                    player.isEliminated(),
                    player.isProtectedFromEffects(),
                    player.getHand().size(),
                    player.getDiscardPile()
            )).toList();

    if(!game.isStarted()){
      return new GameState(
              GamePhase.WAITING_FOR_PLAYERS,
              recipientName,
              players,
              null,
              List.of(),
              0,
              List.of(),
              List.of(),
              List.of()
      );
    }


    GameRound round = game.getCurrentRound();
    boolean roundOver = round.isRoundOver();
    boolean gameOver = game.isGameOver();

    GamePhase phase;

    if(gameOver){
      phase = GamePhase.GAME_OVER;

    }else if(roundOver){
      phase = GamePhase.ROUND_OVER;

    }else {
      phase = GamePhase.ROUND_IN_PROGRESS;
    }

    Player ownPlayer = gamePlayers.get(recipient);

    List<CardType> ownHand = ownPlayer == null ? List.of() : ownPlayer.getHand();

    String currentPlayerName = phase == GamePhase.ROUND_IN_PROGRESS ? round.getCurrentPlayer().getName() :null;

    List<String> roundWinners = round.isWinnerTokensAwarded() ? round.determineWinners().stream()
            .map(Player::getName)
            .toList()
            :List.of();

    List<String> gameWinners = gameOver ? game.getWinners().stream()
            .map(Player::getName)
            .toList() : List.of();

    return new GameState( phase,
            recipientName,
            players,
            currentPlayerName,
            ownHand,
            round.getRemainingDeckSize(),
            round.getFaceUpRemovedCards(),
            roundWinners,
            gameWinners
    );

  }






  /**
   * Starts the chat server on the default TCP port 5500.
   *
   * @param args command-line arguments; currently ignored
   */
  public static void main(String[] args) {
    ChatServer server = new ChatServer(DEFAULT_PORT);
    server.start();
  }
}
