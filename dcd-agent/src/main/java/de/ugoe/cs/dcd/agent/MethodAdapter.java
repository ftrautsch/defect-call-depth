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

import java.util.List;
import java.util.SortedSet;
import java.util.regex.Pattern;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Fabian Trautsch
 */
class MethodAdapter extends MethodVisitor implements Opcodes {
    private final SortedSet<Integer> mutationLines;
    private final SortedSet<Integer> linesWithMutationsWithoutMethod;
    private final String fqn;
    private final String className;
    private final List<Pattern> instrumentationPattern;

    public MethodAdapter(final MethodVisitor mv, SortedSet<Integer> mutationLines, String fqn,
                         SortedSet<Integer> linesWithMutationsWithoutMethod, String className,
                         List<Pattern> instrumentationPattern) {
        super(ASM6, mv);
        this.mutationLines = mutationLines;
        this.fqn = fqn;
        this.linesWithMutationsWithoutMethod = linesWithMutationsWithoutMethod;
        this.className = className.replace("/", ".");
        this.instrumentationPattern = instrumentationPattern;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        // Only trace calls that are within the pattern (e.g., excluding calls to external libraries)
        String calleeClassName = owner.replace("/", ".");
        if(classNameMatchesAny(calleeClassName)) {
            mv.visitMethodInsn(INVOKESTATIC, "de/ugoe/cs/dcd/listener/CallHelper", "raiseDepth", "()V", false);
        }

        mv.visitMethodInsn(opcode, owner, name, desc, itf);

        if(classNameMatchesAny(calleeClassName)) {
            mv.visitMethodInsn(INVOKESTATIC, "de/ugoe/cs/dcd/listener/CallHelper", "lowerDepth", "()V", false);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if(!linesWithMutationsWithoutMethod.isEmpty() && (fqn.endsWith("<init>") || fqn.endsWith("<clinit>"))) {
            for(Integer insertLine: linesWithMutationsWithoutMethod) {
                enhanceMethod(line);
            }
        }

        if(mutationLines != null && mutationLines.contains(line)) {
            enhanceMethod(line);
        }

        mv.visitLineNumber(line, start);
    }

    private Boolean classNameMatchesAny(String className) {
        for (Pattern pattern : instrumentationPattern) {
            if (pattern.matcher(className.toLowerCase()).matches()) {
                return true;
            }
        }
        return false;
    }


    private void enhanceMethod(int line) {
        //System.out.println("Enhancing: " + fqn + " at line: " + line);
        mv.visitLdcInsn(className);
        mv.visitLdcInsn(new Integer(line));
        mv.visitMethodInsn(INVOKESTATIC, "de/ugoe/cs/dcd/listener/CallHelper", "hitMutation", "(Ljava/lang/String;I)V", false);
    }
}
