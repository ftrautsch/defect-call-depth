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

package de.ugoe.cs.dcd.wrapper;

import de.ugoe.cs.dcd.smartshark.SmartSHARKAdapter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * @author Fabian Trautsch
 */
public class Main {
    public static void main(String[] args) {
        Path projectRoot = Paths.get(args[0]);

        Map<String, String> propertyValuesToChange = new HashMap<>();
        propertyValuesToChange.put("projectName", args[1]);
        propertyValuesToChange.put("tagName", args[2]);
        propertyValuesToChange.put("instrumentationClassPattern", args[3]);
        // We need to set the properties here first, because otherwise the adapter would have the wrong project
        try {

            changePropertiesFile(propertyValuesToChange);
        } catch (IOException e) {
            System.out.println("ERROR: "+e);
            return;
        }


        SmartSHARKAdapter smartSHARKAdapter = SmartSHARKAdapter.getInstance();


        //Set<String> testStateNames = new HashSet<String>(){{add("org.apache.commons.lang3.time.FastDateParserSDFTest.testUpperCase[19: KK 11 en_GB]");}};
        /*
        Set<String> testStateNames = new HashSet<String>(){{
            add("com.google.zxing.oned.Code128BlackBox1TestCase.testBlackBox");
        }};
        */


        Set<String> testsToSkip = new HashSet<String>() {{
            }};
        Set<String> testStateNames = smartSHARKAdapter.getTestStateNames();

        for(String testName : testStateNames) {
            if(testsToSkip.contains(testName)) {
                System.out.println("Skipping test: " + testName);
            }
            System.out.println("Executing maven for test: "+ testName);

            propertyValuesToChange.put("testStatePattern", testName);
            try {
                changePropertiesFile(propertyValuesToChange);

                if(testName.contains(",")) {
                    testName = testName.replace(",", "?");
                }


                Path newMavenFile = changeMavenFile(projectRoot, testName);
                runMaven(newMavenFile);
            } catch (IOException | MavenInvocationException e) {
                System.out.println("ERROR: " + e);
            }
        }
    }




    private static void changePropertiesFile(Map<String, String> values) throws IOException {
        Path dcdProperties = Paths.get(System.getenv("DCD_HOME"), "defect-call-depth.properties");
        Properties props = new Properties();
        try(FileInputStream in = new FileInputStream(dcdProperties.toFile())) {
            props.load(in);
        } catch (IOException e) {
            throw new IOException("Could not load properties from "+dcdProperties);
        }


        try(FileOutputStream out = new FileOutputStream(dcdProperties.toFile())) {
            for(Map.Entry<String, String> entry: values.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }
            props.store(out, null);
        } catch (IOException e) {
            throw new IOException("Could not store properties at "+dcdProperties);
        }

    }

    private static String replaceLast(String string, String substring, String replacement)
    {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
                + string.substring(index+substring.length());
    }

    private static Path changeMavenFile(Path projectRoot, String testName) throws IOException {
        Path template;
        if(projectRoot != null) {
            template = Paths.get(projectRoot.toString(), "pom_template2.xml");
        } else {
            throw new IOException("Could not create pit reports folder");
        }

        // Read template
        String content = new String(Files.readAllBytes(template), StandardCharsets.UTF_8);

        // Substitute placeholder with the correct test name that should be tested
        Map<String, String> valuesMap = new HashMap<>();

        // We need to do this here, as we can have parameterized tests with a "." in it
        String[] testNameParts = testName.split("\\[");
        String classAndMethodPart = replaceLast(testNameParts[0], ".", "#");

        StringBuilder rest = new StringBuilder();
        rest.append(classAndMethodPart);
        for(int i=1; i<=testNameParts.length-1; i++) {
            rest.append("[");
            rest.append(testNameParts[i]);
        }
        valuesMap.put("testToExecute",  rest.toString());

        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        String resolvedString = sub.replace(content);

        // Store as pom.xml
        Path pomFile = Paths.get(projectRoot.toString(), "dcdPom.xml");
        Files.write(pomFile, resolvedString.getBytes("UTF-8"));
        return pomFile;
    }

    private static void runMaven(Path newPomFile) throws IOException, MavenInvocationException {
        // Create request
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(newPomFile.toFile());
        request.setGoals(Collections.singletonList("test"));


        Invoker invoker = new DefaultInvoker();
        // We only want to print errored lines, other lines are not of interest
        invoker.setOutputHandler(line -> {
            if(line.startsWith("[ERROR]") || line.startsWith("[WARN]"))
                System.out.println(line);
        });
        InvocationResult result = invoker.execute(request);

        // Check if it returned successfully
        if (result.getExitCode() != 0) {
            throw new IOException("Error in executing loader: Program did not terminate with code 0");
        }
    }
}
