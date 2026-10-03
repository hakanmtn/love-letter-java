package loveletter.protocol;

import loveletter.model.CardType;

import java.util.Objects;

/**
 * Describes a game action requested by a client.
 *
 * @param type the requested action
 * @param card the card to play, or null for other actions
 * @param targetName the target player's name, or null if not needed
 * @param guess the guessed card type, or null if not needed
 */
public record GameAction(GameActionType type,
                         CardType card,
                         String targetName,
                         CardType guess) {
    /**
     * Creates an action with a required action type.
     *
     * @throws NullPointerException if type is null
     */
    public GameAction{
        Objects.requireNonNull(type, "type must not be null");
    }
}
