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

package de.ugoe.cs;

import de.ugoe.cs.dcd.listener.CallHelper;

/**
 * @author Fabian Trautsch
 */
public class B {

    public static void method1() {
        CallHelper.hitMutation("de.ugoe.cs.B", 25);
        CallHelper.raiseDepth();
        method2();
        CallHelper.lowerDepth();
    }

    public static void method2() {
        CallHelper.raiseDepth();
        A.method3();
        CallHelper.lowerDepth();
    }
}
