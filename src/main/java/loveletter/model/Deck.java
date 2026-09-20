package loveletter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a deck of Love Letter cards.
 *
 * <p>Supports shuffling the remaining cards and drawing cards
 * until the deck is empty.
 */
public class Deck {

    private final List<CardType> cards;


    /**
     * Creates an unshuffled deck containing 16 cards:
     * five Guards, two Priests, two Barons, two Handmaids,
     * two Princes, one King, one Countess and one Princess.
     */
    public Deck() {
        cards = new ArrayList<>();
        addCopies(CardType.GUARD,5);
        addCopies(CardType.PRIEST,2);
        addCopies(CardType.BARON,2);
        addCopies(CardType.HANDMAID, 2);
        addCopies(CardType.PRINCE,2);
        addCopies(CardType.KING,1);
        addCopies(CardType.COUNTESS,1);
        addCopies(CardType.PRINCESS,1);

    }

    /**
     * Adds the specified number of copies of a card type to the deck.
     *
     * @param cardType the card type to add
     * @param numberOfCopies the number of copies to add
     */
    private void addCopies(CardType cardType, int numberOfCopies) {
        for (int i = 0; i < numberOfCopies; i++) {
            cards.add(cardType);
        }
    }

    /**
     * Returns the number of cards remaining in the deck.
     *
     * @return the remaining card count
     */
    public int size(){
        return cards.size();
    }

    /**
     * Removes and returns the top card of the deck.
     *
     * <p>The last element of the internal list represents the top
     * of the deck.
     *
     * @return the drawn card
     * @throws IllegalStateException if the deck is empty
     */
    public CardType draw() {
        if(cards.isEmpty()) {
            throw new IllegalStateException("Cannot draw from an empty deck");
        }
        return cards.removeLast();
    }

    /**
     * Returns whether the deck contains no cards.
     *
     * @return true if the deck is empty, otherwise false
     */
    public boolean isEmpty() {
        return cards.isEmpty();

    }

    /**
     * Randomly rearranges the cards remaining in the deck.
     *
     * <p>The number and types of cards remain unchanged.
     */
    public void shuffle(){
        Collections.shuffle(cards);
    }



}
