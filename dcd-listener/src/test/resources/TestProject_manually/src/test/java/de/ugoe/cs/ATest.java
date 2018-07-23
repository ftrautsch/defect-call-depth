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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Fabian Trautsch
 */
public class ATest {

    
    @Test
    public void test1() {
        de.ugoe.cs.listener.CallHelper.raiseDepth();
    	A newA = new A();
        de.ugoe.cs.listener.CallHelper.lowerDepth();
        de.ugoe.cs.listener.CallHelper.raiseDepth();
        newA.method1("INTEST");
        de.ugoe.cs.listener.CallHelper.lowerDepth();
    }
    

    @Test
    public void test2() {
        de.ugoe.cs.listener.CallHelper.raiseDepth();
        A.method4();
        de.ugoe.cs.listener.CallHelper.lowerDepth();
    }
    
    @Test
    public void test3() {
        de.ugoe.cs.listener.CallHelper.raiseDepth();
    	assertEquals(A.metho5(), "method5");
        de.ugoe.cs.listener.CallHelper.lowerDepth();
    }

}
