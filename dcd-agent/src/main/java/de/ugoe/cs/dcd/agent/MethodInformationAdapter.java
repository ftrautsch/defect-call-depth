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

package de.ugoe.cs.dcd.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Fabian Trautsch
 */
public class MethodInformationAdapter extends MethodVisitor implements Opcodes {
    private Map<Integer, String> classInformation = new HashMap<>();
    private String method;

    public MethodInformationAdapter(final MethodVisitor mv, Map<Integer, String> classInformation, String method) {
        super(ASM6, mv);
        this.classInformation = classInformation;
        this.method = method;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        classInformation.put(line, method);
        mv.visitLineNumber(line, start);
    }

}
