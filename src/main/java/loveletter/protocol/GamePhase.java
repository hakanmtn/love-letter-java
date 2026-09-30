package loveletter.protocol;

/**
 * Describes the current phase of the shared game.
 */
public enum GamePhase {

    /** No game currently exists. */
    NO_GAME,

    /** A game exists and players can join before it starts. */
    WAITING_FOR_PLAYERS,

    /** A round is running. */
    ROUND_IN_PROGRESS,

    /** The round has ended and its winners have received their tokens. */
    ROUND_OVER,

    /** The game has ended because the winning score was reached. */
    GAME_OVER
}
