package loveletter.model;

import java.util.ArrayList;
import java.util.List;

public class GameRound {

    private final List<Player> players;
    private final Deck deck;
    private CardType reserveCard;
    private final List<CardType> faceUpRemovedCards;
    private int currentPlayerIndex;
    private boolean turnInProgress;

    public GameRound(List<Player> players) {
        if (players == null) {
            throw new IllegalArgumentException("players cannot be null");
        }
        if (players.size()< 2 || players.size()>4) {
            throw new IllegalArgumentException("A game round requires betwenn two and four players");
        }
        this.players = List.copyOf(players);
        this.deck = new Deck();
        this.faceUpRemovedCards = new ArrayList<>();
        setupRound();
        this.currentPlayerIndex = 0;
        turnInProgress = false;
    }

    public List<Player> getPlayers() {
        return players;
    }

    private void setupRound() {
        for (Player player : players) {
            player.resetForNewRound();
        }
        deck.shuffle();
        reserveCard = deck.draw();

        if(players.size() ==2) {
            for(int i= 0 ; i < 3; i++){
                faceUpRemovedCards.add(deck.draw());
            }
        }

        for (Player player : players) {
            player.receiveCard(deck.draw());
        }
    }

    public int getRemainingDeckSize(){
        return deck.size();
    }

    public boolean hasReserveCard(){
        return reserveCard != null;
    }

    public List<CardType> getFaceUpRemovedCards(){
        return List.copyOf(faceUpRemovedCards);
    }

    public Player getCurrentPlayer(){
        return players.get(currentPlayerIndex);
    }

    public void startCurrentTurn(){
        if (turnInProgress) {
            throw new IllegalStateException("A turn is already in progress");
        }
        if (isRoundOver()) {
            throw new IllegalStateException("Cannot start a new round because the deck is empty");
        }
        Player currentPlayer = getCurrentPlayer();

        currentPlayer.removeProtection();
        currentPlayer.receiveCard(deck.draw());
        turnInProgress = true;
    }

    public void endCurrentTurn(){
        if(!turnInProgress) {
            throw new IllegalStateException("No turn is already in progress");
        }

        Player currentPlayer = getCurrentPlayer();

        if (!currentPlayer.isEliminated() && currentPlayer.getHand().size() != 1) {
            throw new IllegalStateException("Current player must discard one card before ending the turn");
        }
        turnInProgress = false;
        moveToNextActivePlayer();
    }

    private void moveToNextActivePlayer(){
        for (int offset = 1; offset <= players.size(); offset++) {
            int candidateIndex = (currentPlayerIndex + offset) % players.size();

            if(!players.get(candidateIndex).isEliminated()) {
                currentPlayerIndex = candidateIndex;
                return;
            }
        }
        throw new IllegalStateException("No aktiv player remains");
    }

    public boolean isRoundOver(){
        int activePlayers = 0;
        for (Player player : players) {
            if (!player.isEliminated()) {
                activePlayers++;
            }
        }
        return activePlayers <= 1 | deck.isEmpty() ;
    }


}
