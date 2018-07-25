/*
 * Copyright (C) 2017 University of Goettingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.ugoe.cs.dcd.listener;

import de.ugoe.cs.dcd.smartshark.SmartSHARKAdapter;
import de.ugoe.cs.smartshark.model.Mutation;
import de.ugoe.cs.smartshark.model.MutationResult;
import de.ugoe.cs.smartshark.model.TestState;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * @author Fabian Trautsch
 */
public class CallController {
    private static final Logger logger = LogManager.getLogger();
    private boolean testStarted;

    private SmartSHARKAdapter smartSHARKAdapter = SmartSHARKAdapter.getInstance();

    // Visible for testing
    static CallController singleton;

    private CallController() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%d [%t] %-5p - %msg%n").build();
        Appender appender = FileAppender.newBuilder().withFileName("/tmp/dcd.log")
                .withName("File").withLayout(layout).build();
        appender.start();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, Level.WARN, null);
        ctx.updateLoggers();
    }

    public static synchronized CallController getInstance() {
        if (singleton == null) {
            singleton = new CallController();
        }
        return singleton;
    }

    public synchronized void onTestStart() {
        if (testStarted) {
            throw new CallControllerError("Looks like several tests executed in parallel in the same JVM, "
                    + "thus call depth and num calls can not be recorded.");
        }
        testStarted = true;
        CallHelper.initialize();
    }

    public synchronized void onTestFinish(String name) {
        testStarted = false;
        Map<String, SortedSet<Integer>> problems = new HashMap<>();
        TestState testState = smartSHARKAdapter.getTestStateForName(name);

        for(MutationResult res: testState.getMutationResults()) {
            // If the test do not cover this mutation, we can not store the numCalls or call depth
            if(res.getResult().equals("NO_COVERAGE") || res.getCallDepth() != null || res.getNumCalls() != null) {
                continue;
            }

            Mutation mutation = smartSHARKAdapter.getMutationById(res.getMutationId());

            String[] locationParts = mutation.getLocation().split("\\.");
            String mutationLocationClass = String.join(".", Arrays.copyOfRange(locationParts, 0, locationParts.length-1));


            List<Long> results = CallHelper.getHitMutations().get(mutationLocationClass+"%%"+mutation.getLineNumber());

            if(results == null) {
                if(res.getResult().equals("KILLED") || res.getResult().equals("SURVIVED")) {
                    SortedSet<Integer> problemsInTest = problems.getOrDefault(mutationLocationClass, new TreeSet<>());
                    problemsInTest.add(mutation.getLineNumber());
                    problems.put(mutationLocationClass, problemsInTest);
                }
            } else {
                res.setCallDepth(results.get(0));
                res.setNumCalls(results.get(1));
            }
        }
        smartSHARKAdapter.storeTestState(testState);
        logger.warn("Detected the following problems for test "+name+": "+problems);
    }


    public static class CallControllerError extends Error {
        public CallControllerError(String message) {
            super(message);
        }

        public CallControllerError(String message, Throwable cause) {
            super(message, cause);
        }

        public CallControllerError(Throwable cause) {
            super(cause);
        }
    }
}
