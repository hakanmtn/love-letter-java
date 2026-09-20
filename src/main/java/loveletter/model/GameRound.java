package loveletter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages a single Love Letter round, including setup, turn order,
 * card effects, winner determination and round token awards.
 *
 * <p>The round uses the supplied Player objects and modifies
 * their state as play progresses.
 */
public class GameRound {

  private final List<Player> players;
  private final Deck deck;
  private CardType reserveCard;
  private final List<CardType> faceUpRemovedCards;
  private int currentPlayerIndex;
  private boolean turnInProgress;
  private boolean winnerTokensAwarded;


  /**
   * Creates and prepares a round with the first participant
   * as the starting player.
   *
   * <p>Uses the same setup as {@link #GameRound(List, int)}
   * with a starting player index of zero.
   *
   * @param players the two to four participants in turn order;
   *                must not contain null
   * @throws IllegalArgumentException if players is null or
   *         contains fewer than two or more than four participants
   * @throws NullPointerException if a participant is null
   */
  public GameRound(List<Player> players){
    this(players,0);
  }


  /**
   * Creates and prepares a round with the specified starting player.
   *
   * <p>Resets each player's round state while preserving affection
   * tokens, shuffles a new deck and sets aside one reserve card.
   * In a two-player round, three additional cards are removed face up.
   * Each player then receives one card.
   *
   * <p>The participant order is copied, but the Player objects
   * themselves are shared with the caller.
   * The first turn is not started automatically.
   *
   * @param players the two to four participants in turn order;
   *                must not contain null
   * @param startingPlayerIndex the zero-based position of the
   *                            starting player in the participant list
   * @throws IllegalArgumentException if players is null, its size
   *         is outside the range of two to four, or the starting
   *         player index is outside the list bounds
   * @throws NullPointerException if a participant is null
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

  /**
   * Returns the participants in turn order as an unmodifiable list.
   *
   * <p>The list contains the actual Player objects used by this round;
   * their state can still change.
   *
   * @return the participants in turn order
   */
  public List<Player> getPlayers() {
    return players;
  }

  /**
   * Resets the players' round state, shuffles the deck, sets aside
   * the reserve card and deals one card to each player.
   *
   * <p>For two participants, also removes three cards face up
   * before dealing.
   */
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

  /**
   * Returns the number of cards remaining in the draw pile.
   *
   * <p>Excludes the reserve card, face-up removed cards,
   * hand cards and discarded cards.
   *
   * @return the remaining draw pile size
   */
  public int getRemainingDeckSize() {
    return deck.size();
  }

  /**
   * Returns whether the reserve card is still available.
   *
   * <p>The Prince effect can consume this card when the draw pile
   * is empty and a replacement card is needed.
   *
   * @return true if the reserve card is available, otherwise false
   */
  public boolean hasReserveCard() {
    return reserveCard != null;
  }

  /**
   * Returns an unmodifiable snapshot of the cards removed face up
   * during round setup.
   *
   * @return the three face-up removed cards in a two-player round,
   *         or an empty list with three or four players
   */
  public List<CardType> getFaceUpRemovedCards() {
    return List.copyOf(faceUpRemovedCards);
  }

  /**
   * Returns the player currently selected in the turn order.
   *
   * <p>This does not indicate whether a turn is in progress
   * or whether the round is over.
   *
   * @return the currently selected player
   */
  public Player getCurrentPlayer() {
    return players.get(currentPlayerIndex);
  }

  /**
   * Starts the current player's turn by removing their protection
   * and drawing one card into their hand.
   *
   * @throws IllegalStateException if a turn is already in progress,
   *         the round is over, or the player's hand already
   *         contains two cards
   */
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


  /**
   * Ends the current turn and advances to the next player
   * who has not been eliminated.
   *
   * <p>A player who has not been eliminated must hold exactly
   * one card before ending the turn.
   * The next player's turn is not started automatically.
   *
   * @throws IllegalStateException if no turn is in progress,
   *         the current player is not eliminated and does not
   *         hold exactly one card, or no active player remains
   */
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

  /**
   * Advances through the participant order to the next
   * non-eliminated player, wrapping around at the end.
   *
   * @throws IllegalStateException if no active player remains
   */
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

  /**
   * Returns whether the round has ended.
   *
   * <p>The round ends when at most one non-eliminated player
   * remains, or when the draw pile is empty and no turn
   * is in progress.
   *
   * @return true if the round is over, otherwise false
   */
  public boolean isRoundOver() {
    long activePlayers = players.stream().filter(player -> !player.isEliminated()).count();
    return activePlayers <= 1 | (deck.isEmpty() && !turnInProgress);
  }

  /**
   * Determines the winners of the completed round without
   * awarding affection tokens.
   *
   * <p>If only one non-eliminated player remains, that player wins.
   * Otherwise, the players with the highest hand card value
   * are compared by the sum of their discarded card values.
   * All players still tied after this comparison win.
   *
   * @return an unmodifiable list of winners in participant order
   * @throws IllegalStateException if the round is not over
   *         or no non-eliminated player remains
   */
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

  /**
   * Calculates the sum of the values of a player's discarded cards.
   *
   * @param player the player whose discard pile is evaluated
   * @return the total discard value, or zero if the pile is empty
   */
  private int discardValueOf(Player player) {
    return player.getDiscardPile().stream().mapToInt(CardType::getValue).sum();
  }

  /**
   * Awards one affection token to each winner of the completed
   * round and marks the round as scored.
   *
   * <p>Tokens can be awarded only once per round.
   *
   * @return an unmodifiable list of round winners in participant order
   * @throws IllegalStateException if tokens have already been awarded,
   *         the round is not over, or no non-eliminated player remains
   */
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
   * Returns whether affection tokens have been awarded
   * for this round.
   *
   * @return true if this round has been scored, otherwise false
   */
  public boolean isWinnerTokensAwarded(){
    return winnerTokensAwarded;
  }

  /**
   * Plays a card without a target or a guess.
   *
   * <p>Suitable for Handmaid, Princess and Countess.
   * Priest, Baron, King and Guard can also be played this way
   * when no eligible opponent is available.
   * Prince always requires a target.
   *
   * @param player the current player
   * @param card the card to play
   * @throws IllegalArgumentException if an argument or the card
   *         choice is invalid, or a target or guess is required
   * @throws IllegalStateException if no turn is in progress,
   *         the player is eliminated or does not hold exactly two cards
   * @see #playCard(Player, CardType, Player, CardType)
   */
  public void playCard(Player player, CardType card){
    playCard(player,card,null);
  }

  /**
   * Plays a card with the specified target and no guess.
   *
   * <p>Uses the same rules as
   * {@link #playCard(Player, CardType, Player, CardType)},
   * with a null guess.
   * Guard requires the four-argument overload when an eligible
   * opponent is available.
   *
   * @param player the current player
   * @param card the card to play
   * @param target the selected target, or null when no target is required
   * @return the card revealed by Priest, or an empty Optional
   *         if no card is revealed
   * @throws IllegalArgumentException if an argument, card choice
   *         or target is invalid, or a guess is required
   * @throws IllegalStateException if no turn is in progress,
   *         the player is eliminated or does not hold exactly two cards
   */
  public Optional<CardType> playCard(Player player, CardType card, Player target){
    return playCard(player,card,target, null);
  }

  /**
   * Plays a card from the current player's hand and applies its effect.
   *
   * <p>The player must be the current, non-eliminated participant,
   * hold exactly two cards and own the selected card.
   * Countess must be played when held together with King or Prince.
   *
   * <p>Priest, Baron, King and Guard require an active, unprotected
   * opponent. If no such opponent exists, the card is discarded
   * without effect and the target must be null.
   * Guard also requires a null guess in that case.
   *
   * <p>Prince requires either the player themselves or an active,
   * unprotected opponent as its target. Handmaid, Princess and
   * Countess require a null target.
   *
   * <p>When Guard has a target, the guess must be a card type
   * other than Guard. All other cards require a null guess.
   *
   * <p>This method does not end the turn or award round tokens.
   *
   * @param player the current player
   * @param card the card to play
   * @param target the selected target, or null when no target is required
   * @param guess the card type guessed with Guard, or null when
   *              no guess is required
   * @return the target's hand card revealed by Priest,
   *         or an empty Optional if no card is revealed
   * @throws IllegalArgumentException if player or card is null,
   *         the player is not the current player, the card is not
   *         held, the Countess rule is violated, or the target
   *         or guess is invalid
   * @throws IllegalStateException if no turn is in progress,
   *         the player is eliminated or does not hold exactly two cards
   */
  public Optional<CardType> playCard(Player player, CardType card, Player target, CardType guess){
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

    if(card != CardType.PRIEST && target != null &&
            card != CardType.BARON && card !=CardType.KING &&
            card != CardType.PRINCE && card != CardType.GUARD){
      throw new IllegalArgumentException("This Card does not require a target");
    }

    if(card != CardType.GUARD && guess !=null){
      throw new IllegalArgumentException("Only Guard accepts a guessed card");
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
      case KING -> {
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
        player.swapHandWith(target);
        return Optional.empty();

      }
      case PRINCE -> {
        if(target == null){
          throw new IllegalArgumentException("Prince requires a target");
        }

        if(target != player && !getAvailableOpponents(player).contains(target)){
          throw new IllegalArgumentException("Choose yourself or an active, unprotected opponent");
        }

        player.discardCard(card);

        CardType discardedCard = target.getHand().getFirst();
        target.discardCard(discardedCard);

        if (discardedCard == CardType.PRINCESS){
          target.eliminate();
          return Optional.empty();
        }

        if(deck.isEmpty()){
          target.receiveCard(reserveCard);
          reserveCard = null;
        }else{
          target.receiveCard(deck.draw());
        }

        return Optional.empty();
      }
      case GUARD -> {
        List<Player> opponents = getAvailableOpponents(player);

        if(opponents.isEmpty()){
          if(target !=null || guess != null){
            throw new IllegalArgumentException("No opponent is available, omit target and guess");

          }

          player.discardCard(card);
          return Optional.empty();
        }

        if(target == null || !opponents.contains(target)){
          throw new IllegalArgumentException("Choose an active, unprotected opponent");

        }
        if(guess == null || guess == CardType.GUARD){
          throw new IllegalArgumentException("Guess a card other than Guard");

        }
        player.discardCard(card);

        if(target.getHand().getFirst() == guess) {
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
   * Returns the non-eliminated, unprotected opponents of a participant.
   *
   * <p>Excludes the supplied player and preserves participant order.
   * This method does not check whose turn it is.
   *
   * @param player the participant whose opponents are requested
   * @return an unmodifiable list of eligible opponents,
   *         or an empty list if none are available
   * @throws IllegalArgumentException if player is null
   *         or is not a participant in this round
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
