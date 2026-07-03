import java.io.IOException;

public class SensorReadException extends IOException {
    public SensorReadException(String message) {
        super(message);
    }
}
