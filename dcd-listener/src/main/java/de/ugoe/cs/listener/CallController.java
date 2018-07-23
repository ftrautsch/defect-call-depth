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

package de.ugoe.cs.listener;

import de.ugoe.cs.smartshark.SmartSHARKAdapter;
import de.ugoe.cs.smartshark.model.Mutation;
import de.ugoe.cs.smartshark.model.MutationResult;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Fabian Trautsch
 */
public class CallController {
    private static Logger logger = LogManager.getLogger(CallController.class);
    private boolean testStarted;

    private SmartSHARKAdapter smartSHARKAdapter = SmartSHARKAdapter.getInstance();

    // Visible for testing
    static CallController singleton;

    private CallController() {
        // TODO: Get all mutations for project in hashmap Map<ObjectId, Mutation>
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
                    + "thus coverage per test can't be recorded correctly.");
        }
        testStarted = true;
        CallHelper.initialize();
    }

    public synchronized void onTestFinish(String name) {
        testStarted = false;
        //System.out.println(CallHelper.getHitMutations());

        for(MutationResult res: smartSHARKAdapter.getMutationResultsForTestState(name)) {
            // If the test do not cover this mutation, we can not store the numCalls or call depth
            if(res.getResult().equals("NO_COVERAGE")) {
                continue;
            }

            Mutation mutation = smartSHARKAdapter.getMutationById(res.getMutationId());


            List<Long> results = CallHelper.getHitMutations().get(mutation.getLocation()+"%%"+mutation.getLineNumber());
            if(results == null) {
                System.out.println("RESULTS NULL FOR "+name+" with results:" +res);
            }

            if(res.getResult().equals("KILLED")) {
                System.out.println("INVALID RESULT FOR "+name+" with results:" +res);
            } else {
                //System.out.println("RESULT FOR "+name+ ": "+String.valueOf(results.get(0))+", "+String.valueOf(results.get(1)));
                //res.setCallDepth(results.get(0));
                //res.setNumCalls(results.get(1));
            }

        }

        //TODO: Store results in database
        // Get test by name
        // go through all mutation results
        // for each mutationresult:
        // if result: no coverage => continue
        // lookup mutation by id in hashmap
        // lookup location in CallHelper.getHitMutations()
        // lookup line number
        // store call_depth, numCalls in mutation_res for test
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
