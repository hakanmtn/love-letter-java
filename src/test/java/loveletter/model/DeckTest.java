package loveletter.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void newDeckShouldContainSixteenCards(){
        Deck deck = new Deck();
        assertEquals(16, deck.size());
    }

    @Test
    void drawingCardShouldReturnCardReduceDeckSize(){
        Deck deck = new Deck();
        CardType drawnCard = deck.draw();

        assertNotNull(drawnCard);
        assertEquals(15, deck.size());
    }

    @Test
    void drawingFromEmptyDeckShouldThrowException(){
        Deck deck = new Deck();


        for(int i = 0; i < 16; i++){
            deck.draw();
        }

        assertTrue(deck.isEmpty());
        assertThrows(IllegalStateException.class, deck::draw);
    }


    @Test
    void newDeckShouldContainCorrectCardDistribution(){
        Deck deck = new Deck();
        deck.shuffle();

        List<CardType> drawnCards = new ArrayList<>();

        while(!deck.isEmpty()){
            drawnCards.add(deck.draw());
        }
        assertEquals(5, Collections.frequency(drawnCards, CardType.GUARD));
        assertEquals(2, Collections.frequency(drawnCards, CardType.PRIEST));
        assertEquals(2, Collections.frequency(drawnCards, CardType.BARON));
        assertEquals(2, Collections.frequency(drawnCards, CardType.HANDMAID));
        assertEquals(2, Collections.frequency(drawnCards, CardType.PRINCE));
        assertEquals(1, Collections.frequency(drawnCards, CardType.KING));
        assertEquals(1, Collections.frequency(drawnCards, CardType.COUNTESS));
        assertEquals(1, Collections.frequency(drawnCards, CardType.PRINCESS));

    }

}
