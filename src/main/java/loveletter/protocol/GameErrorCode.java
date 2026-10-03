package loveletter.protocol;

/** Identifies the category of a game-action error. */
public enum GameErrorCode {

  /** The message could not be decoded as a valid game action. */
  INVALID_MESSAGE,
  /** The requested action could not be performed. */
  ACTION_REJECTED
}
