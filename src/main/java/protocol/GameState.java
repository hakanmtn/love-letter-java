package protocol;

import loveletter.model.CardType;

import java.util.List;
import java.util.Objects;

/**
 * Contains a game snapshot prepared for one specific recipient.
 *
 * <p>Only the recipient's hand is included. Opponents' hand card
 * identities and the face-down reserve card are excluded.
 *
 * @param phase the current game phase
 * @param recipientName the nickname of the receiving client
 * @param players public player information in participant order
 * @param currentPlayerName the active player's nickname;
 *                          null outside a running round
 * @param ownHand the recipient's hand; empty before the game starts,
 *                for spectators, or when the recipient has no cards
 * @param remainingDeckSize the draw pile size; zero before a round exists
 * @param faceUpRemovedCards cards removed face up during round setup;
 *                           empty before a round exists
 * @param roundWinners winner nicknames after round scoring;
 *                     otherwise empty
 * @param gameWinners overall winner nicknames when the game is over;
 *                    otherwise empty
 */
public record GameState(GamePhase phase,
                        String recipientName,
                        List<PlayerState> players,
                        String currentPlayerName,
                        List<CardType> ownHand,
                        int remainingDeckSize,
                        List<CardType> faceUpRemovedCards,
                        List<String> roundWinners,
                        List<String> gameWinners
                        ) {
    public GameState {
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(recipientName, "recipientName must not be null");

        players = List.copyOf(players);
        ownHand = List.copyOf(ownHand);
        faceUpRemovedCards = List.copyOf(faceUpRemovedCards);
        roundWinners = List.copyOf(roundWinners);
        gameWinners = List.copyOf(gameWinners);
    }


}
