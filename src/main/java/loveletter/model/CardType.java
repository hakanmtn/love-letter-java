package loveletter.model;
/**
 * Defines the eight card types and their numeric values.
 *
 * <p>Card effects are implemented in {@link GameRound}.
 */
public enum CardType {
    /** Guard, with a value of 1. */
    GUARD(1),

    /** Priest, with a value of 2. */
    PRIEST(2),

    /** Baron, with a value of 3. */
    BARON(3),

    /** Handmaid, with a value of 4. */
    HANDMAID(4),

    /** Prince, with a value of 5. */
    PRINCE(5),

    /** King, with a value of 6. */
    KING(6),

    /** Countess, with a value of 7. */
    COUNTESS(7),

    /** Princess, with a value of 8. */
    PRINCESS(8);

    /** The numeric value used when comparing and scoring cards. */
    private final int value;

    /**
     * Creates a card type with its fixed numeric value.
     *
     * @param value the numeric value assigned to this card type
     */
    CardType(int value) {
        this.value = value;
    }

    /**
     * Returns the numeric value of this card type.
     *
     * @return the card value, ranging from 1 for GUARD to 8 for PRINCESS
     */
    public int getValue() {
        return value;
    }

}
