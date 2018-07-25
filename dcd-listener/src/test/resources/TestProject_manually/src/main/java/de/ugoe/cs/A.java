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
public class A {
    public void method1(String name) {
        if(name.equals("INTEST")) {
            CallHelper.hitMutation("de.ugoe.cs.A", 25);
            System.out.println("IN");
        } else {
            System.out.println("NOTIN");
        }
        CallHelper.hitMutation("de.ugoe.cs.A", 30);
        while(name.equals("INTEST")) {
        	break;
        }
        CallHelper.raiseDepth();
        method2(name, 1);
        CallHelper.lowerDepth();
        CallHelper.raiseDepth();
        method2("asdsa", 2);
        CallHelper.lowerDepth();
        CallHelper.raiseDepth();
        Blub blub = new Blub();
        CallHelper.lowerDepth();
        CallHelper.raiseDepth();
        blub.method1();
        CallHelper.lowerDepth();
    }

    public static void method2(String name, int blub) {
        if(name.equals("INTEST")) {
            System.out.println("MUTATION");
        } else {
            CallHelper.hitMutation("de.ugoe.cs.A", 45);
            System.out.println("METHOD2");
        }
        return;
        	
    }

    public static void method3() {
        System.out.println("METHOD3");
    }

    public static void method4() {
        CallHelper.raiseDepth();
        B.method1();
        CallHelper.lowerDepth();
    }
    
    public static String metho5() {
        CallHelper.hitMutation("de.ugoe.cs.A", 60);
        return "method5";
    }

    class Blub {
        public void method1() {
            System.out.println("METHOD1 in BLUB");
        }
    }
}