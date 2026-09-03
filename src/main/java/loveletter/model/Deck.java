package loveletter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<CardType> cards;

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

    private void addCopies(CardType cardType, int numberOfCopies) {
        for (int i = 0; i < numberOfCopies; i++) {
            cards.add(cardType);
        }
    }

    public int size(){
        return cards.size();
    }

    public CardType draw() {
        if(cards.isEmpty()) {
            throw new IllegalStateException("Cannot draw from an empty deck");
        }
        return cards.removeLast();
    }

    public boolean isEmpty() {
        return cards.isEmpty();

    }

    public void shuffle(){
        Collections.shuffle(cards);
    }



}
