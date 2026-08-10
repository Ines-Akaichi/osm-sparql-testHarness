package wu.ac.at.sparql.evaluation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Single-JVM-per-measurement harness
 */
public class TestHarness {

    // Number of untimed warm-up passes run before the measured pass.
    private static final int WARMUP_ITERATIONS = 3;

    
    public static void main(String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

       // System.out.println("java.version: " + System.getProperty("java.version"));
       // System.out.println("java.vendor: " + System.getProperty("java.vendor"));
        //System.out.println("java.home: " + System.getProperty("java.home"));
       // System.out.println("java.runtime.version: " + System.getProperty("java.runtime.version"));
        
        
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }

        String aboxPath;
        String elapseTime = null;
        String queryKeyword;
        int iterationNb;

        if (args.length == 4) {
            aboxPath     = args[0];
            elapseTime   = args[1];
            queryKeyword = args[2];
            iterationNb  = Integer.parseInt(args[3]);
        } else if (args.length == 3) {
            aboxPath     = args[0];
            queryKeyword = args[1];
            iterationNb  = Integer.parseInt(args[2]);
        } else {
            printUsage();
            return;
        }

        String aboxFileName = aboxPath.replace(".ttl", "");
        String aboxSizeOblig = aboxFileName.replaceAll("[^0-9]", "");

        /* ---- Warm-up: untimed, discarded ---- */
        /*for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            PipelineRunner.runOnce(aboxPath, elapseTime, queryKeyword);
        }*/

        /* ---- Measured run ---- */
        PipelineMetrics m = PipelineRunner.runOnce(aboxPath, elapseTime, queryKeyword);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // CSV: iteration,timestamp,aboxSizeOblig,queryKeyword,elapsedTimeInMillis,memoryUsedInKB,
        //      tboxLoadMs,aboxLoadMs,contextBuildMs,inferenceMs,queryMs,gcCount,gcTimeMs
        System.out.printf("%d,%s,%s,%s,%d,%d,%s%n",
                iterationNb, timestamp, aboxSizeOblig, queryKeyword,
                m.totalMs, m.memoryUsedKB, m.toCsvFragment());
    }

    private static void printUsage() {
        System.err.println(
            "Usage:\n" +
            "  java -jar testHarness.jar <abox.ttl> [elapseTime] <QUERY_KEYWORD> <ITERATION_NUMBER>\n\n" +
            "CSV columns:\n" +
            "  iteration,timestamp,aboxSizeOblig,queryKeyword,elapsedTimeInMillis,memoryUsedInKB,\n" +
            "  " + PipelineMetrics.csvHeaderFragment() + "\n\n" +
            "Keywords:\n" +
            "  OBLIGATION-STATE\n" +
            "  REGULATED-ACTION_STATE\n" +
            "  TEMPORAL-ACTION-STATE\n" +
            "  EVENT-STATE\n" +
            "  ENTITY\n" +
            "  ACTION\n" +
            "  RESOURCE\n"
        );
    }
}