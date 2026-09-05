package loveletter.model;

public enum CardType {

    GUARD(1),
    PRIEST(2),
    BARON(3),
    HANDMAID(4),
    PRINCE(5),
    KING(6),
    COUNTESS(7),
    PRINCESS(8);

    private final int value;


    CardType(int value) {
        this.value = value;
    }


    public int getValue() {
        return value;
    }

}
