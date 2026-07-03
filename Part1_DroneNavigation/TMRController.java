import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TMRController {

    private static final int MIN_ALTITUDE = 0;
    private static final int MAX_ALTITUDE = 200;

    private int consecutiveFailures = 0;
    private String reliabilityStatus = "OK";
    private Consumer<String> logger;

    public TMRController(Consumer<String> logger) {
        this.logger = logger;
    }

    public int applyVoting(Integer[] readings, int previousAltitude, String[] sensorIds)
            throws SystemReliabilityException {

        List<Integer> validReadings = new ArrayList<>();
        List<String> validIds = new ArrayList<>();
        List<String> outlierIds = new ArrayList<>();

        // separate valid readings from failed/corrupted
        for (int i = 0; i < readings.length; i++) {
            if (readings[i] != null && readings[i] >= MIN_ALTITUDE && readings[i] <= MAX_ALTITUDE) {
                validReadings.add(readings[i]);
                validIds.add(sensorIds[i]);
            } else {
                outlierIds.add(sensorIds[i]);
            }
        }

        if (!outlierIds.isEmpty()) {
            String msg = "OUTLIER_DETECTED | Sensors=" + outlierIds;
            System.out.println("  [TMR] " + msg);
            logger.accept(msg);
        }

        // need at least 2 valid readings
        if (validReadings.size() < 2) {
            return handleFailure(previousAltitude, "Only " + validReadings.size() + " valid reading(s)");
        }

        // majority voting - check all pairs
        for (int i = 0; i < validReadings.size(); i++) {
            for (int j = i + 1; j < validReadings.size(); j++) {
                if (validReadings.get(i).equals(validReadings.get(j))) {
                    int agreed = validReadings.get(i);
                    consecutiveFailures = 0;
                    reliabilityStatus = "OK";
                    String msg = "MAJORITY_DECISION | Sensors=[" + validIds.get(i) + "," + validIds.get(j) + "] altitude=" + agreed + " m";
                    System.out.println("  [TMR] " + msg);
                    logger.accept(msg);
                    return agreed;
                }
            }
        }

        // no majority found
        return handleFailure(previousAltitude, "No majority among readings " + validReadings);
    }

    private int handleFailure(int previousAltitude, String reason) throws SystemReliabilityException {
        consecutiveFailures++;
        reliabilityStatus = "DEGRADED (failures=" + consecutiveFailures + ")";

        String failMsg = "RELIABILITY_FAILURE | Consecutive=" + consecutiveFailures + " | " + reason;
        System.out.println("  [TMR] " + failMsg);
        logger.accept(failMsg);

        if (consecutiveFailures >= 2) {
            reliabilityStatus = "SAFE MODE";
            throw new SystemReliabilityException("Two consecutive failures: " + reason);
        }

        // first failure - use fallback
        String fallbackMsg = "FALLBACK_DECISION | Using previous altitude=" + previousAltitude + " m";
        System.out.println("  [TMR] " + fallbackMsg);
        logger.accept(fallbackMsg);
        return previousAltitude;
    }

    public String getReliabilityStatus() {
        return reliabilityStatus;
    }
}
