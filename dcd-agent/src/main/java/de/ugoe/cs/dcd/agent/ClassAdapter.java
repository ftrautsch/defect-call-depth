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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Fabian Trautsch
 */
class ClassAdapter extends ClassVisitor implements Opcodes {
    private final String className;
    private final Map<Integer, String> methodInformation;
    private final SortedSet<Integer> insertMutationProbes;
    private final List<Pattern> instrumentationPattern;

    public ClassAdapter(final ClassVisitor cv, String className, Map<Integer, String> methodInformation,
                        SortedSet<Integer> insertMutationProbes, List<Pattern> instrumentationPatterns) {
        super(ASM6, cv);
        this.className = className;
        this.methodInformation = methodInformation;
        this.insertMutationProbes = insertMutationProbes;
        this.instrumentationPattern = instrumentationPatterns;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
                                     final String desc, final String signature, final String[] exceptions) {
        String fqn = className.replace("/", ".") + "." + name;
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        // We create two sets here: First set is just the set of lines that have a mutation and
        // the second set contains mutation lines that are not part of any method (i.e., mutations

        SortedSet<Integer> linesWithMutations = insertMutationProbes;
        SortedSet<Integer> linesWithMutationsWithoutMethod = new TreeSet<>();
        if(linesWithMutations != null) {
            linesWithMutationsWithoutMethod.addAll(linesWithMutations);
            linesWithMutationsWithoutMethod.removeAll(methodInformation.keySet());
        }

        return mv == null ? null : new MethodAdapter(mv, insertMutationProbes,
                fqn, linesWithMutationsWithoutMethod, className,
                instrumentationPattern);
    }
}