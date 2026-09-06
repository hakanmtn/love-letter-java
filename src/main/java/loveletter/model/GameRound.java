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
  private boolean winnerTokensAwarded;

  public GameRound(List<Player> players) {
    if (players == null) {
      throw new IllegalArgumentException("players cannot be null");
    }
    if (players.size() < 2 || players.size() > 4) {
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

    if (players.size() == 2) {
      for (int i = 0; i < 3; i++) {
        faceUpRemovedCards.add(deck.draw());
      }
    }

    for (Player player : players) {
      player.receiveCard(deck.draw());
    }
  }

  public int getRemainingDeckSize() {
    return deck.size();
  }

  public boolean hasReserveCard() {
    return reserveCard != null;
  }

  public List<CardType> getFaceUpRemovedCards() {
    return List.copyOf(faceUpRemovedCards);
  }

  public Player getCurrentPlayer() {
    return players.get(currentPlayerIndex);
  }

  public void startCurrentTurn() {
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

  public void endCurrentTurn() {
    if (!turnInProgress) {
      throw new IllegalStateException("No turn is already in progress");
    }

    Player currentPlayer = getCurrentPlayer();

    if (!currentPlayer.isEliminated() && currentPlayer.getHand().size() != 1) {
      throw new IllegalStateException(
          "Current player must discard one card before ending the turn");
    }
    turnInProgress = false;
    moveToNextActivePlayer();
  }

  private void moveToNextActivePlayer() {
    for (int offset = 1; offset <= players.size(); offset++) {
      int candidateIndex = (currentPlayerIndex + offset) % players.size();

      if (!players.get(candidateIndex).isEliminated()) {
        currentPlayerIndex = candidateIndex;
        return;
      }
    }
    throw new IllegalStateException("No aktiv player remains");
  }

  public boolean isRoundOver() {
    long activePlayers = players.stream().filter(player -> !player.isEliminated()).count();
    return activePlayers <= 1 | (deck.isEmpty() && !turnInProgress);
  }

  public List<Player> determineWinners() {
    if (!isRoundOver()) {
      throw new IllegalStateException("Winner cannot be determined before the round is over");
    }

    List<Player> activePlayers = players.stream().filter(player -> !player.isEliminated()).toList();

    if (activePlayers.isEmpty()) {
      throw new IllegalStateException("No active player remains");
    }

    if (activePlayers.size() == 1) {
      return activePlayers;
    }

    int highestHandValue =
        activePlayers.stream()
            .mapToInt(player -> player.getHand().getFirst().getValue())
            .max()
            .orElseThrow();

    List<Player> highestHandPlayers =
        activePlayers.stream()
            .filter(player -> player.getHand().getFirst().getValue() == highestHandValue)
            .toList();

    if (highestHandPlayers.size() == 1) {
      return highestHandPlayers;
    }

    int highestDiscardValue =
        highestHandPlayers.stream().mapToInt(this::discardValueOf).max().orElseThrow();

    return highestHandPlayers.stream()
        .filter(player -> discardValueOf(player) == highestDiscardValue)
        .toList();
  }

  private int discardValueOf(Player player) {
    return player.getDiscardPile().stream().mapToInt(CardType::getValue).sum();
  }

  public List<Player> awardWinnerTokens() {
    if(winnerTokensAwarded){
      throw new IllegalStateException("Winner tokens have already been awarded");
    }

    List<Player> winners = determineWinners();

    winners.forEach(Player::awardAffectionTokens);
    winnerTokensAwarded = true;
    return winners;
  }

}
