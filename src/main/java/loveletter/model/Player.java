package loveletter.model;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player and stores their cards, affection tokens,
 * elimination status and protection status.
 *
 * <p>Round-specific state is cleared by {@link #resetForNewRound()},
 * while the player's name and affection tokens are preserved.
 */
public class Player {

    private final String name;
    private int affectionTokens;
    private final List<CardType> hand;
    private final List<CardType> discardPile;
    private boolean eliminated;
    private boolean protectedFromEffects;


    /**
     * Creates a player with empty hands and discard pile, zero affection
     * tokens, and no elimination or protection status.
     *
     * @param name the player's name; must not be null or blank
     * @throws IllegalArgumentException if the name is null or blank
     */
    public Player(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        this.name = name;
        this.affectionTokens = 0;
        this.hand = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.eliminated = false;
        this.protectedFromEffects = false;
    }

    /**
     * Returns the player's name.
     *
     * @return the player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of affection tokens earned by the player.
     *
     * @return the current affection token count
     */
    public int getAffectionTokens() {
        return affectionTokens;
    }

    /**
     * Increases the player's affection token count by one.
     */
    public void awardAffectionTokens() {
        affectionTokens++;
    }

    /**
     * Adds a card to the player's hand.
     *
     * @param card the card to receive; must not be null
     * @throws IllegalArgumentException if the card is null
     * @throws IllegalStateException if the player already holds two cards
     */
    public void receiveCard(CardType card){
        if(card == null){
            throw new IllegalArgumentException("Card must not be null");
        }

        if(hand.size() >= 2){
            throw new IllegalStateException("Player cannot hold more than two cards");
        }
        hand.add(card);

    }

    /**
     * Returns an unmodifiable snapshot of the player's hand.
     *
     * <p>Later changes to the player's hand are not reflected in
     * the returned list.
     *
     * @return an unmodifiable snapshot of the hand
     */
    public List<CardType> getHand() {
        return List.copyOf(hand);
    }

    /**
     * Moves one occurrence of the specified card from the player's
     * hand to the discard pile without applying its effect.
     *
     * @param card the card to discard
     * @throws IllegalArgumentException if the card is null or
     *         the player does not hold it
     */
    public void discardCard(CardType card){
        if(card == null){
            throw new IllegalArgumentException("Card must not be null");
        }
        if(!hand.remove(card)){
            throw new IllegalArgumentException("Player does not hold this card");
        }
        discardPile.add(card);
    }

    /**
     * Returns an unmodifiable snapshot of the player's discard pile.
     *
     * <p>Later changes to the discard pile are not reflected in
     * the returned list.
     *
     * @return an unmodifiable snapshot of the discard pile
     */
    public List<CardType> getDiscardPile() {
        return List.copyOf(discardPile);
    }

    /**
     * Returns whether the player has been eliminated from the round.
     *
     * @return true if the player is eliminated, otherwise false
     */
    public boolean isEliminated() {
        return eliminated;
    }

    /**
     * Eliminates the player from the round, moves all remaining hand
     * cards to the discard pile and removes protection.
     */
    public void eliminate(){
        discardPile.addAll(hand);
        hand.clear();
        protectedFromEffects = false;
        eliminated = true;
    }

    /**
     * Returns whether the player's protection flag is set.
     *
     * @return true if protection is active, otherwise false
     */
    public boolean isProtectedFromEffects() {
        return protectedFromEffects;
    }

    /**
     * Activates the player's protection flag.
     */
    public void protectFromEffects() {
        protectedFromEffects = true;
    }

    /**
     * Removes the player's protection.
     */
    public void removeProtection(){
        protectedFromEffects = false;
    }

    /**
     * Clears the hand and discard pile and resets elimination
     * and protection status for a new round.
     *
     * <p>The player's name and affection tokens are preserved.
     * This method does not deal new cards.
     */
    public void resetForNewRound(){
        hand.clear();
        discardPile.clear();
        eliminated = false;
        protectedFromEffects = false;
    }

    /**
     * Exchanges the player's single hand card with another player's
     * single hand card.
     *
     * <p>Discard piles, affection tokens, elimination status and
     * protection status remain unchanged.
     *
     * @param other the player to exchange hand cards with
     * @throws IllegalArgumentException if other is null or
     *         refers to this player
     * @throws IllegalStateException if either player does not hold
     *         exactly one card
     */
    public void swapHandWith(Player other){
        if  (other == null){
            throw new IllegalArgumentException("Other player must not be null");
        }

        if(other == this){
            throw new IllegalArgumentException("Player cannot swap hands with themselves");
        }
        if(hand.size() !=1 || other.hand.size() !=1){
            throw new IllegalStateException("Both players must hold exactly one card");
        }

        CardType ownCard = hand.getFirst();
        CardType otherCard = other.hand.getFirst();

        hand.set(0,otherCard);
        other.hand.set(0, ownCard);

    }
}
