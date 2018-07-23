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

package de.ugoe.cs.testproject;

/**
 * @author Fabian Trautsch
 */
public class A {
    public void method1(String name) {
        if(name.equals("INTEST")) {
            System.out.println("IN");
        } else {
            System.out.println("NOTIN");
        }
        while(name.equals("INTEST")) {
        	break;
        }
        method2(name, 1);
        method2("asdsa", 2);

        Blub blub = new Blub("bla");
        blub.method1();
        blub.method2();
    }

    public static void method2(String name, int blub) {
        if(name.equals("INTEST")) {
            System.out.println("MUTATION");
        } else {
            System.out.println("METHOD2");
        }
        return;
        	
    }

    public static void method3() {
        System.out.println("METHOD3");
    }

    public static void method4() {
        B.method1();
    }
    
    public static String metho5() {
        return "method5";
    }



    class Blub {
        private String bla;

        public Blub(String bla) {
            this.bla = bla;
        }
        public void method1() {
            System.out.println("METHOD1 in BLUB");
        }

        public String method2() {
            return this.bla;
        }
    }
}