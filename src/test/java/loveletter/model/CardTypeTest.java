package loveletter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardTypeTest {

    @Test
    void shouldReturnValueOfPrince(){
        assertEquals(5,CardType.PRINCE.getValue());
    }
}
