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

public class ChatServer {
    private static final int DEFAULT_PORT = 5500;
    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Set<String> nicknames = ConcurrentHashMap.newKeySet();
    private Game game;
    private final Map<ClientHandler, Player> gamePlayers = new HashMap<>();

    public ChatServer(int port){
        this.port = port;
    }

    ChatServer(int port, Game game){
        this(port);
        this.game = Objects.requireNonNull(game,"game must not be null");
    }

    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server started on port " + port);

            while(true){
                Socket clientSocket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket,this);

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        }catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    void addClient(ClientHandler clientHandler){
        clients.add(clientHandler);
    }

    synchronized void removeClient(ClientHandler clientHandler){

        clients.remove(clientHandler);

        Player leavingPlayer = gamePlayers.remove(clientHandler);

        if(leavingPlayer == null){
            return;
        }

        game = null;
        gamePlayers.clear();

        broadcast("The game was closed because " + leavingPlayer.getName() +
                " disconnected. Use /create to start a new game.");
    }

    void broadcast(String message){
        for(ClientHandler client: clients){
            client.sendMessage(message);
        }
    }

    boolean registerNickname(String nickname){
        if(nickname == null || nickname.isBlank()){
            return false;
        }

        String normalizedName = nickname.toLowerCase(Locale.ROOT);

        return nicknames.add(normalizedName);
    }
    void unregisterNickname(String nickname){
        String normalizedNickname = nickname.toLowerCase(Locale.ROOT);
        nicknames.remove(normalizedNickname);
    }

    void broadcastToOthers(String message, ClientHandler excludedClient){
        for(ClientHandler client : clients){
            if(client != excludedClient){
                client.sendMessage(message);
            }
        }

    }

    synchronized boolean handleCommand(ClientHandler sender, String message){
        String command = message.trim();

        if(!command.startsWith("/")){
            return false;
        }

        if("/help".equalsIgnoreCase(command)){
            sender.sendMessage("Available commands: /help, /create, /join, /start, /hand, /score, /help, "
                    + "/play CARD. Use bye to disconnect.");
            sender.sendMessage(
                    "Play: /play CARD [TARGET]. "
                            + "Guard: /play GUARD TARGET GUESS. "
                            + "Example: /play GUARD nati KING");

        }else if("/create".equalsIgnoreCase(command)){
            if(game != null){
                sender.sendMessage("A game already exists.");
                return true;
            }

            game = new Game();
            broadcast("A new Love Letter game has been created.");

        }else if("/join".equalsIgnoreCase(command)){
            if(game == null){
                sender.sendMessage("Create a game first with /create.");
                return true;
            }

            if(gamePlayers.containsKey(sender)){
                sender.sendMessage("You have already joined the game.");
                return true;
            }

            Player player = new Player(sender.getNickname());

            try{
                game.join(player);
            }catch(IllegalArgumentException | IllegalStateException e){
                sender.sendMessage("Cannot join game: " + e.getMessage());
                return true;
            }

            gamePlayers.put(sender, player);
            broadcast(player.getName() + " joined the game. Players: " +
                    game.getPlayers().size() + "/4");

        }else if("/start".equalsIgnoreCase(command)){
            if(game == null){
                sender.sendMessage("Create a game first with /create.");
                return true;
            }

            if(!gamePlayers.containsKey(sender)){
                sender.sendMessage("Join the game first with /join.");
                return true;
            }

            try{
                game.start();
            }catch(IllegalStateException e){
                sender.sendMessage("Cannot start game: " + e.getMessage());
                return true;
            }

            GameRound round = game.getCurrentRound();
            round.startCurrentTurn();

            broadcast("The Love Letter game has started.");
            broadcast("Current Player: " + round.getCurrentPlayer().getName());

            for(Map.Entry<ClientHandler,Player> entry : gamePlayers.entrySet()) {
                ClientHandler client = entry.getKey();
                Player player = entry.getValue();

                client.sendMessage("Your hand: " + player.getHand());
            }
        }else if("/hand".equalsIgnoreCase(command)){
            if(game == null)  {
                sender.sendMessage("Create a game first with /create.");
                return true;
            }

            Player player = gamePlayers.get(sender);

            if(player == null){
                sender.sendMessage("Join the game first with /join.");
                return true;
            }

            if(!game.isStarted()) {
                sender.sendMessage("The game has not started yet.");
                return true;
            }

            if(player.isEliminated()){
                sender.sendMessage("You are eliminated from this round.");
                return true;
            }

            sender.sendMessage("Your hand: " + player.getHand());
        }else if ("/play".equalsIgnoreCase(command.split("\\s+",2)[0])) {
            handlePlayCommand(sender,command);

        }else if("/score".equalsIgnoreCase(command)){
            if(game == null){
                sender.sendMessage("Create a game first with /create.");
                return true;
            }

            if(game.getPlayers().isEmpty()){
                sender.sendMessage("No players have joined the game yet.");
                return true;
            }

            sender.sendMessage("Score:");

            for (Player participant : game.getPlayers()){
                sender.sendMessage(participant.getName() + ": "
                        + participant.getAffectionTokens()
                        + " affection Token(s)");
            }
        }else if("/next".equalsIgnoreCase(command)) {
            if(game == null){
                sender.sendMessage("Create a game first with /create");
                return true;
            }

            if(!gamePlayers.containsKey(sender)){
                sender.sendMessage("Join the game first with /join.");
                return true;
            }

            try{
                game.startNextRound();
            }catch (IllegalStateException e){
                sender.sendMessage("Cannot start next round: " + e.getMessage());
                return true;
            }

            GameRound round = game.getCurrentRound();
            round.startCurrentTurn();

            broadcast("A new round has started.");
            broadcast("Current player: " + round.getCurrentPlayer().getName());

            for(Map.Entry<ClientHandler,Player> entry : gamePlayers.entrySet()){
                entry.getKey().sendMessage("Your hand: " + entry.getValue().getHand());
            }
        }else {
            sender.sendMessage("Unknown command. Use /help to see available commands");
        }
        return true;

    }

    private void handlePlayCommand(ClientHandler sender, String command){
        if(game == null){
            sender.sendMessage("Create a game first with /create.");
            return;
        }

        Player player = gamePlayers.get(sender);

        if(player == null){
            sender.sendMessage("Join the game first with /join.");
            return;
        }

        if(!game.isStarted()){
            sender.sendMessage("The game hast not started yet.");
            return;
        }

        GameRound round = game.getCurrentRound();

        if(round.isRoundOver()){
            sender.sendMessage("This round is over.");
            return;
        }

        String[] parts = command.split("\\s+", 3);

        if(parts.length < 2){
            sender.sendMessage("Usage: /play CARD [TARGET] [GUESS]");
            return;
        }

        CardType card;
        Player target = null;
        CardType guess = null;
        Optional<CardType> revealedCard;


        try {
            card = parseCardType(parts[1]);

            if(parts.length == 3){
                String targetName = parts[2].trim();

                if(card == CardType.GUARD){
                    int separator = targetName.lastIndexOf(' ');

                    if(separator < 0){
                        throw new IllegalArgumentException("Usage: /play GUARD TARGET GUESS");
                    }
                    String guessText = targetName.substring(separator+1);
                    targetName = targetName.substring(0,separator).trim();

                    guess = parseCardType(guessText);

                }
                String searchedName = targetName;

                target = game.getPlayers().stream()
                        .filter(candidate ->
                                candidate.getName().equalsIgnoreCase(searchedName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown player: " + searchedName
                        ));
            }
            revealedCard = round.playCard(player, card, target, guess);
        }catch (IllegalArgumentException | IllegalStateException e){
            sender.sendMessage("Cannot play cards: " + e.getMessage());
            return;
        }

        revealedCard.ifPresent(cardType -> sender.sendMessage("Viewed card: " + cardType));

        String playMessage = player.getName() +" played " + card;
        if(target != null) {
            playMessage += " targeting " + target.getName();
        }

        if(guess != null){
            playMessage += " and guessed " + guess;
        }

        broadcast(playMessage + " .");

        if(card == CardType.GUARD && target != null){
            if(target.isEliminated()){
                broadcast(target.getName() + " was eliminated: the guess was correct.");
            }else {
                broadcast("The guess was incorrect. Nobody was eliminated.");
            }
        }

        if(card == CardType.PRINCE && target != null){
            CardType discardedCard = target.getDiscardPile().getLast();

            broadcast(target.getName() + " discarded " + discardedCard + ".");

            if(target.isEliminated()) {
                broadcast(target.getName() + " was eliminated after discarding PRINCESS.");
            }else{
                broadcast(target.getName() + " drew a replacement card.");
            }
        }

        if(card == CardType.KING && target != null){
            broadcast(player.getName() + " and " + target.getName() + " swapped their hand cards.");
        }

        if(card == CardType.BARON && target != null){
            if(player.isEliminated()){
                broadcast(player.getName() + " was eliminated after comparing hand cards.");
            }else if(target.isEliminated()){
                broadcast(target.getName() + " was eliminated after comparing hand cards.");
            }else {
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

        if(round.isRoundOver()){
            List<Player> winners = game.finishCurrentRound();

            String winnersNames = winners.stream()
                    .map(Player::getName)
                    .collect(java.util.stream.Collectors.joining(", "));

            broadcast("Round over. Winners: " + winnersNames);
            broadcast("Each round winner receives one affection token.");

            if(game.isGameOver()){
                String gameWinnerNames = game.getWinners().stream()
                        .map(Player::getName)
                        .collect(java.util.stream.Collectors.joining(", "));

                broadcast("Game over. Overall winners: " + gameWinnerNames);
                broadcast("Use /score to see the final scores.");

            }else {
                broadcast("Use /next to start the next round.");
            }

            return;

        }

        round.startCurrentTurn();

        broadcast("Current player: " + round.getCurrentPlayer().getName());

        for(Map.Entry<ClientHandler,Player> entry: gamePlayers.entrySet()){
            Player participant = entry.getValue();

            if(!participant.isEliminated()){
                entry.getKey().sendMessage("Your hand: " + participant.getHand());
            }
        }
    }

    CardType parseCardType(String text){
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Card name must not be empty");
        }

        try{
            return CardType.valueOf(text.trim().toUpperCase(Locale.ROOT));

        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Unknown card: " + text + ". Use GUARD, PRIEST, BARON, HANDMAID, "
                    + "PRINCE, KING, COUNTESS or PRINCESS.");
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(DEFAULT_PORT);
        server.start();
    }
}
