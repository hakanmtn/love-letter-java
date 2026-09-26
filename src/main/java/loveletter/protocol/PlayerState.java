package loveletter.protocol;

import loveletter.model.CardType;

import java.util.List;
import java.util.Objects;


/**
 * Contains a snapshot of publicly visible player information.
 *
 * <p>Hand card identities are deliberately excluded.
 *
 * @param name the player's nickname
 * @param affectionTokens the number of affection tokens
 * @param eliminated whether the player is eliminated from the round
 * @param protectedFromEffects whether the player is protected
 * @param handSize the number of cards in the player's hand
 * @param discardPile the discarded cards in discard order
 */
public record PlayerState(String name,
                          int affectionTokens,
                          boolean eliminated,
                          boolean protectedFromEffects,
                          int handSize,
                          List<CardType> discardPile) {


    /**
     * Creates a snapshot with an unmodifiable copy of the discard pile.
     *
     * @throws NullPointerException if the name, discard pile,
     *         or a discard pile entry is null
     */
    public PlayerState {
        Objects.requireNonNull(name, "name must not be null");
        discardPile = List.copyOf(discardPile);
    }
}
