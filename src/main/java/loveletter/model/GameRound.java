package loveletter.model;

import java.lang.foreign.PaddingLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameRound {

  private final List<Player> players;
  private final Deck deck;
  private CardType reserveCard;
  private final List<CardType> faceUpRemovedCards;
  private int currentPlayerIndex;
  private boolean turnInProgress;
  private boolean winnerTokensAwarded;

  public GameRound(List<Player> players){
    this(players,0);
  }


  /**
   * Creates a round with the specified starting player index.
   *
   * @param players the participants in their turn order
   * @param startingPlayerIndex the position of the starting player
   */
  public GameRound(List<Player> players, int startingPlayerIndex) {
    if (players == null) {
      throw new IllegalArgumentException("players cannot be null");
    }
    if (players.size() < 2 || players.size() > 4) {
      throw new IllegalArgumentException("A game round requires betwenn two and four players");
    }

    if(startingPlayerIndex < 0 || startingPlayerIndex >= players.size()){
      throw new IllegalArgumentException("Starting player index is out of bound");
    }

    this.players = List.copyOf(players);
    this.deck = new Deck();
    this.faceUpRemovedCards = new ArrayList<>();
    setupRound();
    this.currentPlayerIndex = startingPlayerIndex;
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

  /**
   * Indicates whether the winners have received their tokens.
   *
   * @return true if this round has already been scored
   */
  public boolean isWinnerTokensAwarded(){
    return winnerTokensAwarded;
  }

  /**
   * Plays a card without selecting a target.
   *
   * @param player the player playing the card
   * @param card the card to play
   */
  public void playCard(Player player, CardType card){
    playCard(player,card,null);
  }
  public Optional<CardType> playCard(Player player, CardType card, Player target){
    if(player == null || card == null){
      throw new IllegalArgumentException("Player and card must not be null");
    }

    if(!turnInProgress){
      throw new IllegalStateException("No turn is in progress");
    }

    if(player !=getCurrentPlayer()){
      throw new IllegalArgumentException("It is not this player's turn");
    }

    if(player.isEliminated()){
      throw new IllegalStateException("Player is eliminated");
    }

    if(player.getHand().size() !=2){
      throw new IllegalStateException("Player must hold two cards before playing");

    }
    if(!player.getHand().contains(card)){
      throw new IllegalArgumentException("Player does not hold this card");
    }

    boolean holdsCountess = player.getHand().contains(CardType.COUNTESS);

    boolean holdsKingOrPrice = player.getHand().contains(CardType.KING)
            || player.getHand().contains(CardType.PRINCE);

    if(holdsCountess && holdsKingOrPrice && card !=CardType.COUNTESS){
      throw new IllegalArgumentException(
              "Countess must be played when holding King or Prince");
    }

    if(card != CardType.PRIEST && target != null && card != CardType.BARON){
      throw new IllegalArgumentException("This Card does not require a target");
    }


    switch (card){
      case HANDMAID -> {
        player.discardCard(card);
        player.protectFromEffects();
      }
      case PRINCESS -> {
        player.discardCard(card);
        player.eliminate();
      }
      case COUNTESS -> {
        player.discardCard(card);
      }
      case PRIEST -> {
        List<Player> opponents = getAvailableOpponents(player);

        if(opponents.isEmpty()){
          if(target != null){
            throw new IllegalArgumentException(
                    "No opponent is available; omit the target"
            );
          }

          player.discardCard(card);
          return Optional.empty();
        }

        if(target == null || !opponents.contains(target)){
          throw new IllegalArgumentException("Choose an active, unprotected opponent");
        }

        CardType revealedCard = target.getHand().getFirst();
        player.discardCard(card);

        return Optional.of(revealedCard);
      }
      case BARON -> {
        List<Player> opponents = getAvailableOpponents(player);

        if(opponents.isEmpty()){
          if(target != null){
            throw new IllegalArgumentException("No opponent is available; omit the target");

          }

          player.discardCard(card);
          return Optional.empty();
        }

        if(target == null || !opponents.contains(target)){
          throw new IllegalArgumentException("Choose an active, unprotected opponent");
        }

        player.discardCard(card);

        int playerValue = player.getHand().getFirst().getValue();
        int targetValue = target.getHand().getFirst().getValue();

        if(playerValue < targetValue){
          player.eliminate();
        }else if (targetValue<playerValue){
          target.eliminate();
        }

        return Optional.empty();
      }
        default -> throw new UnsupportedOperationException(
                "This card effect is not implemented yet"
        );
    }
    return Optional.empty();
  }

  /**
   * Returns the active, unprotected opponents of a player.
   *
   * @param player the player selecting an opponent
   * @return the opponents available for selection
   * @throws IllegalArgumentException if the player is not a participant
   */
  public List<Player> getAvailableOpponents(Player player){
    if(player == null || !players.contains(player)){
      throw new IllegalArgumentException("Player must participate in this round");
    }

    return players.stream()
            .filter(opponent -> opponent != player)
            .filter(opponent -> !opponent.isEliminated())
            .filter(opponent -> !opponent.isProtectedFromEffects())
            .toList();
  }




}
