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

package de.ugoe.cs.agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Fabian Trautsch
 */
class MethodAdapter extends MethodVisitor implements Opcodes {
    private SortedSet<Integer> mutationLines = new TreeSet<>();
    private SortedSet<Integer> methodLines = new TreeSet<>();
    private SortedSet<Integer> linesWithMutationsWithoutMethod = new TreeSet<>();
    private String fqn;
    private String className;

    public MethodAdapter(final MethodVisitor mv, SortedSet<Integer> mutationLines, String fqn, SortedSet<Integer> methodLines,
                         SortedSet<Integer> linesWithMutationsWithoutMethod, String className) {
        super(ASM6, mv);
        this.mutationLines = mutationLines;
        this.fqn = fqn;
        this.methodLines = methodLines;
        this.linesWithMutationsWithoutMethod = linesWithMutationsWithoutMethod;
        this.className = className.replace("/", ".");
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if(methodLines.isEmpty() || methodLines.first().equals(line)) {
            mv.visitMethodInsn(INVOKESTATIC, "de/ugoe/cs/listener/CallHelper", "raiseDepth", "()V", false);
        }

        if(!linesWithMutationsWithoutMethod.isEmpty() && (fqn.endsWith("<init>") || fqn.endsWith("<clinit>"))) {
            for(Integer insertLine: linesWithMutationsWithoutMethod) {
                enhanceMethod(line);
            }
        }

        if(mutationLines != null && mutationLines.contains(line)) {
            enhanceMethod(line);
        }

        mv.visitLineNumber(line, start);

        if( methodLines.isEmpty() || methodLines.last().equals(line)) {
            mv.visitMethodInsn(INVOKESTATIC, "de/ugoe/cs/listener/CallHelper", "lowerDepth", "()V", false);
        }
    }


    private void enhanceMethod(int line) {
        System.out.println("Enhancing: " + fqn + " at line: " + line);
        mv.visitLdcInsn(className);
        mv.visitLdcInsn(new Integer(line));
        mv.visitMethodInsn(INVOKESTATIC, "de/ugoe/cs/listener/CallHelper", "hitMutation", "(Ljava/lang/String;I)V", false);
    }
}
