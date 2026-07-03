import java.util.Random;

public class DroneAltitudeSensor {

    public static final int MIN_VALID = 0;
    public static final int MAX_VALID = 200;

    private String id;
    private int baselineValue;
    private Random random;

    public DroneAltitudeSensor(String id, int baselineValue) {
        this.id = id;
        this.baselineValue = baselineValue;
        this.random = new Random();
    }

    // chance < 15: sensor failure, chance < 30: corrupted, else: valid reading
    public int readSensor() throws SensorReadException {
        int chance = random.nextInt(100);

        if (chance < 15) {
            throw new SensorReadException("Sensor " + id + " failure (chance=" + chance + ")");
        } else if (chance < 30) {
            // corrupted: value outside [0, 200]
            if (random.nextBoolean()) {
                return MAX_VALID + 1 + random.nextInt(300);
            } else {
                return -(1 + random.nextInt(100));
            }
        } else {
            // valid reading
            int raw = baselineValue + random.nextInt(20);
            return Math.max(MIN_VALID, Math.min(MAX_VALID, raw));
        }
    }

    public boolean isValidReading(int value) {
        return value >= MIN_VALID && value <= MAX_VALID;
    }

    public String getId() {
        return id;
    }
}
