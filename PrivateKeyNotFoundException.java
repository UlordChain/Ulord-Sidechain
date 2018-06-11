package tools;

public class PrivateKeyNotFoundException extends Exception {
    public PrivateKeyNotFoundException() {
    }

    @Override
    public String getMessage() {
        return "Private key not found.";
    }
}
