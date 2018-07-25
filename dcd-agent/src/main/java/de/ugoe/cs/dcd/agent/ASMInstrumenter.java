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

import de.ugoe.cs.dcd.config.ConfigurationReader;
import de.ugoe.cs.dcd.smartshark.SmartSHARKAdapter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mutabilitydetector.asm.NonClassloadingClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * @author Fabian Trautsch
 */
public class ASMInstrumenter implements ClassFileTransformer {
    private final static Logger logger = LogManager.getLogger(ASMInstrumenter.class);

    private final ConfigurationReader configuration = ConfigurationReader.getInstance();
    private final Map<String, SortedSet<Integer>> insertMutationProbes;
    private final Map<Integer, String> methodInformation = new HashMap<>();

    //private SmartSHARKAdapter sharkAdapter = new SmartSHARKAdapter(configuration);

    private ASMInstrumenter() {
        insertMutationProbes = SmartSHARKAdapter.getInstance().getMutationsWithLines();

        /*
        insertMutationProbes.put("de.ugoe.cs.testproject.A.<init>", new TreeSet<Integer>(){{add(2);}});
        insertMutationProbes.put("de.ugoe.cs.testproject.A.method1", new TreeSet<Integer>(){{add(25); add(29);}});
        insertMutationProbes.put("de.ugoe.cs.testproject.A.method2", new TreeSet<Integer>(){{add(44);}});
        insertMutationProbes.put("de.ugoe.cs.testproject.A.metho5", new TreeSet<Integer>(){{add(59);}});
        insertMutationProbes.put("de.ugoe.cs.testproject.B.method1", new TreeSet<Integer>(){{add(25);}});
        */
        //System.out.println(insertMutationProbes);
    }

    public static void premain(String instrumentationClassPattern, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ASMInstrumenter());
    }

    private Boolean classNameMatchesAny(String className, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(className.toLowerCase()).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] bytes) {

        boolean enhanceClass = false;

        Boolean includePatternMatches = classNameMatchesAny(className, configuration.getInstrumentationClassPattern());
        Boolean excludePatternMatches = classNameMatchesAny(className, configuration.getExcludeClassPattern());

        if((!configuration.getInstrumentationClassPattern().isEmpty() &&
                !configuration.getExcludeClassPattern().isEmpty() && includePatternMatches && !excludePatternMatches) ||
                !configuration.getInstrumentationClassPattern().isEmpty() &&
                        configuration.getExcludeClassPattern().isEmpty() && includePatternMatches) {
            enhanceClass = true;
        }

        if (enhanceClass) {
            // Gather information on lines and methods
            methodInformation.clear();
            gatherInformation(className, bytes);
            // Enhance Class

            return enhanceClass(className, bytes);

            //return bytes;
        } else {
            return bytes;
        }
    }

    private void gatherInformation(String className, byte[] bytes) {
        //System.out.println("VISITING CLASS: " + className);
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new NonClassloadingClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new ClassInformationAdapter(writer, methodInformation);
        reader.accept(visitor, 0);
    }


    private byte[] enhanceClass(String className, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new NonClassloadingClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new ClassAdapter(writer, className,
                methodInformation, insertMutationProbes.get(className.replace("/", ".")));
        reader.accept(visitor, 0);
        byte[] b = writer.toByteArray();

        if (configuration.isDebugEnabled()) {
            File output = new File(configuration.getDebugOut() + "/" + className + ".class");
            try(FileOutputStream fos = new FileOutputStream(output)){
                // Create Directory path for debugging
                System.out.println("Storing: " + className);
                boolean dirCreated = output.getParentFile().mkdirs();
                logger.debug("Directory Created?: " + dirCreated);
                fos.write(b);
            } catch(IOException e){
                System.out.println("Could not store class: " + className + ". Error:" + e);
                logger.error("Could not store class: " + className + ". Error:" + e);
            }
        }

        return writer.toByteArray();
    }
}
