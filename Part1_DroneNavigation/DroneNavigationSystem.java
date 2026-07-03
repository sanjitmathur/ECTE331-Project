import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DroneNavigationSystem {

    private static final int ITERATIONS = 20;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static BufferedWriter logWriter;
    private static String logFileName;

    public static void main(String[] args) {

        logFileName = "log_" + System.currentTimeMillis() + ".txt";
        try {
            logWriter = new BufferedWriter(new FileWriter(logFileName));
        } catch (IOException e) {
            System.err.println("Cannot open log file: " + e.getMessage());
            return;
        }

        log("Drone Navigation System Started");
        System.out.println("Log file: " + logFileName);
        System.out.println("Simulating " + ITERATIONS + " cycles.\n");

        DroneAltitudeSensor sensorA = new DroneAltitudeSensor("A", 100);
        DroneAltitudeSensor sensorB = new DroneAltitudeSensor("B", 100);
        DroneAltitudeSensor sensorC = new DroneAltitudeSensor("C", 100);
        DroneAltitudeSensor[] sensors = {sensorA, sensorB, sensorC};
        String[] sensorIds = {"A", "B", "C"};

        TMRController controller = new TMRController(DroneNavigationSystem::log);
        int lastAltitude = 100;

        try {
            for (int cycle = 1; cycle <= ITERATIONS; cycle++) {
                System.out.println("--- Cycle " + cycle + " ---");
                log("--- Cycle " + cycle + " ---");

                Integer[] readings = new Integer[3];

                for (int s = 0; s < sensors.length; s++) {
                    try {
                        int val = sensors[s].readSensor();
                        readings[s] = val;
                        String tag = sensors[s].isValidReading(val) ? "VALID" : "CORRUPTED";
                        System.out.printf("Sensor %s -> %d m [%s]%n", sensorIds[s], val, tag);
                        if (!sensors[s].isValidReading(val)) {
                            log("CORRUPTED_READING | Sensor=" + sensorIds[s] + " | Value=" + val);
                        }
                    } catch (SensorReadException ex) {
                        readings[s] = null;
                        System.out.printf("Sensor %s -> FAILURE (%s)%n", sensorIds[s], ex.getMessage());
                        log("SENSOR_FAILURE | Sensor=" + sensorIds[s] + " | " + ex.getMessage());
                    }
                }

                lastAltitude = controller.applyVoting(readings, lastAltitude, sensorIds);
                System.out.println("Final Altitude: " + lastAltitude + " m");
                System.out.println("Reliability: " + controller.getReliabilityStatus());
                System.out.println();
            }

            log("Simulation completed normally.");

        } catch (SystemReliabilityException sre) {
            System.out.println("\n!!! SAFE MODE ACTIVATED !!!");
            System.out.println(sre.getMessage());
            log("SAFE_MODE | " + sre.getMessage());

        } finally {
            log("System Terminated");
            try {
                if (logWriter != null) logWriter.close();
            } catch (IOException e) {
                System.err.println("Error closing log: " + e.getMessage());
            }
            System.out.println("\nLog saved: " + logFileName);
        }
    }

    public static void log(String message) {
        String entry = "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;
        try {
            if (logWriter != null) {
                logWriter.write(entry);
                logWriter.newLine();
                logWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("Log error: " + e.getMessage());
        }
    }
}
