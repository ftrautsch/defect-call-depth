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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Fabian Trautsch
 */
class ClassAdapter extends ClassVisitor implements Opcodes {
    private String className;
    private Map<Integer, String> methodInformation = new HashMap<>();
    private Map<String, SortedSet<Integer>> insertMutationProbes = new HashMap<>();

    public ClassAdapter(final ClassVisitor cv, String className, Map<Integer, String> methodInformation,
                        Map<String, SortedSet<Integer>> insertMutationProbes) {
        super(ASM6, cv);
        this.className = className;
        this.methodInformation = methodInformation;
        this.insertMutationProbes = insertMutationProbes;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
                                     final String desc, final String signature, final String[] exceptions) {
        String fqn = className.replace("/", ".") + "." + name;
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        SortedSet<Integer> lines = new TreeSet<>();
        for(Map.Entry<Integer, String> entry: methodInformation.entrySet()) {
            if(entry.getValue().equals(name + "%%" + desc))
                lines.add(entry.getKey());
        }

        SortedSet<Integer> linesWithMutations = insertMutationProbes.getOrDefault(fqn, null);
        SortedSet<Integer> linesWithMutationsWithoutMethod = new TreeSet<>();
        if(linesWithMutations != null) {
            linesWithMutationsWithoutMethod.addAll(linesWithMutations);
            linesWithMutationsWithoutMethod.removeAll(methodInformation.keySet());
        }

        return mv == null ? null : new MethodAdapter(mv, linesWithMutations,
                fqn, lines, linesWithMutationsWithoutMethod);
    }
}