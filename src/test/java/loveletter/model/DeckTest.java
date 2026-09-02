package loveletter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeckTest {

    @Test
    void newDeckShouldContainSixteenCards(){
        Deck deck = new Deck();
        assertEquals(16, deck.size());
    }
}
