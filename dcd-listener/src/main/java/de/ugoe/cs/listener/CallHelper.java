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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fabian Trautsch
 */
public class CallHelper {
    private static long callDepth = -1;
    private static long numCalls = -1;

    private static Map<String, List<Long>> hitMutations = new HashMap<>();

    public static synchronized void initialize() {
        callDepth = -1;
        numCalls = -1;
        hitMutations.clear();
    }

    public static synchronized void raiseDepth() {
        callDepth++;
        numCalls++;
    }

    public static synchronized void lowerDepth() {
        callDepth--;
    }

    public static synchronized void hitMutation(String className, int lineNumber) {
        List<Long> callDepthAndCalls = new ArrayList<>();
        callDepthAndCalls.add(callDepth);
        callDepthAndCalls.add(numCalls);
        System.out.println("HIT "+className+" on line: "+lineNumber);

        hitMutations.put(className+"%%"+lineNumber, callDepthAndCalls);
    }

    public synchronized static Map<String, List<Long>> getHitMutations() {
        return hitMutations;
    }
}
