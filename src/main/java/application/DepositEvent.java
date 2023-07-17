package application;

public class DepositEvent {
    private final String message;

    public DepositEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
