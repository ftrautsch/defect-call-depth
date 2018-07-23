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

/**
 * @author Fabian Trautsch
 */
public class B {

    public static void method1() {
        de.ugoe.cs.listener.CallHelper.hitMutation("de.ugoe.cs.B", 25);
        de.ugoe.cs.listener.CallHelper.raiseDepth();
        method2();
        de.ugoe.cs.listener.CallHelper.lowerDepth();
    }

    public static void method2() {
        de.ugoe.cs.listener.CallHelper.raiseDepth();
        A.method3();
        de.ugoe.cs.listener.CallHelper.lowerDepth();
    }
}
