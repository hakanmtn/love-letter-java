package loveletter.protocol;



/**
 * Identifies a game action requested by a client.
 */
public enum GameActionType {
    /** Creates a new game. */
    CREATE,

    /** Joins the current game. */
    JOIN,

    /** Starts the current game. */
    START,

    /** Plays a card from the player's hand. */
    PLAY,

    /** Starts the next round. */
    NEXT_ROUND

}
