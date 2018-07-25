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

import de.ugoe.cs.dcd.listener.CallHelper;
import org.junit.Test;

/**
 * @author Fabian Trautsch
 */
public class ATest {

    
    @Test
    public void test1() {
        CallHelper.raiseDepth();
    	A newA = new A();
        CallHelper.lowerDepth();
        CallHelper.raiseDepth();
        newA.method1("INTEST");
        CallHelper.lowerDepth();
    }
    

    @Test
    public void test2() {
        CallHelper.raiseDepth();
        A.method4();
        CallHelper.lowerDepth();
    }
    
    @Test
    public void test3() {
        CallHelper.raiseDepth();
    	assertEquals(A.metho5(), "method5");
        CallHelper.lowerDepth();
    }

}
