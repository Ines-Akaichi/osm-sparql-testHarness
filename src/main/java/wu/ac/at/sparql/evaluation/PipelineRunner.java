package wu.ac.at.sparql.evaluation;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import wu.ac.at.sparql.ContextManager;
import wu.ac.at.sparql.InputManager;
import wu.ac.at.sparql.QueryService;
import wu.ac.at.sparql.StateInferenceEngine;

/**
 * Runs the load -> context -> inference -> query pipeline exactly once,
 * from scratch, with per-phase timing and GC/iteration instrumentation.
 * Shared by TestHarness (single measured run per JVM invocation) and
 * MultiIterationHarness (many timed runs inside a single JVM invocation).
 */
public final class PipelineRunner {

    private PipelineRunner() { }

    public static PipelineMetrics runOnce(String aboxPath, String elapseTime, String queryKeyword) {
        PipelineMetrics m = new PipelineMetrics();

        Runtime runtime = Runtime.getRuntime();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long gcCountBefore = sumGcCount(gcBeans);
        long gcTimeBefore = sumGcTime(gcBeans);
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long totalStart = System.nanoTime();

        /* ---- 1. Load TBox (background, classpath) ---- */
        long t0 = System.nanoTime();
        InputManager inputManager = new InputManager(elapseTime);
        inputManager.loadTboxFromClasspath("ontology-ocm.ttl");
        long t1 = System.nanoTime();
        m.tboxLoadMs = (t1 - t0) / 1_000_000;

        /* ---- 2. Load ABox (input) ---- */
        inputManager.loadAbox(aboxPath);
        long t2 = System.nanoTime();
        m.aboxLoadMs = (t2 - t1) / 1_000_000;

        /* ---- 3. Build context (merge models, extract prefixes) ---- */
        ContextManager ctx = new ContextManager(inputManager);
        long t3 = System.nanoTime();
        m.contextBuildMs = (t3 - t2) / 1_000_000;

        /* ---- 4. Run SPARQL state-detection pipeline ---- */
        StateInferenceEngine engine = new StateInferenceEngine(ctx);
        engine.run();
        long t4 = System.nanoTime();
        m.inferenceMs = (t4 - t3) / 1_000_000;
        //m.fixpointIterations = engine.getLastIterationCount();

        /* ---- 5. Query ---- */
        QueryService queryService = new QueryService(ctx);
        queryService.queryByKeyword(queryKeyword);
        long t5 = System.nanoTime();
        m.queryMs = (t5 - t4) / 1_000_000;

        long totalEnd = System.nanoTime();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long gcCountAfter = sumGcCount(gcBeans);
        long gcTimeAfter = sumGcTime(gcBeans);

        m.totalMs = (totalEnd - totalStart) / 1_000_000;
        m.memoryUsedKB = Math.abs(memAfter - memBefore) / 1024;
        m.gcCountDelta = gcCountAfter - gcCountBefore;
        m.gcTimeMsDelta = gcTimeAfter - gcTimeBefore;

        return m;
    }

    private static long sumGcCount(List<GarbageCollectorMXBean> beans) {
        long sum = 0;
        for (GarbageCollectorMXBean bean : beans) {
            long c = bean.getCollectionCount();
            if (c > 0) sum += c;
        }
        return sum;
    }

    private static long sumGcTime(List<GarbageCollectorMXBean> beans) {
        long sum = 0;
        for (GarbageCollectorMXBean bean : beans) {
            long t = bean.getCollectionTime();
            if (t > 0) sum += t;
        }
        return sum;
    }
}