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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Fabian Trautsch
 */
public class Main {
    public static void main(String[] args) throws IOException, MavenInvocationException {
        Path projectRoot = Paths.get(args[0]);
        SmartSHARKAdapter smartSHARKAdapter = SmartSHARKAdapter.getInstance();

        //Set<String> testStateNames = new HashSet<String>(){{add("com.zaxxer.hikari.metrics.prometheus.HikariCPCollectorTest.noConnectionWithoutPoolName");}};
        Set<String> testStateNames = new HashSet<String>(){{add("org.apache.commons.beanutils.BeanUtils2TestCase.testSeparateInstances");}};
        //Set<String> testStateNames = smartSHARKAdapter.getTestStateNames();
        for(String testName : testStateNames) {
            changePropertiesFile(args[1], args[2], args[3], testName);
            Path newMavenFile = changeMavenFile(projectRoot, testName);
            runMaven(newMavenFile);
        }
    }




    public static void changePropertiesFile(String projectName, String tagName, String instrumentationClassPattern,
                                            String testName) throws IOException {
        Path dcdProperties = Paths.get(System.getenv("DCD_HOME"), "defect-call-depth.properties");
        FileInputStream in = new FileInputStream(dcdProperties.toFile());
        Properties props = new Properties();
        props.load(in);
        in.close();


        FileOutputStream out = new FileOutputStream(dcdProperties.toFile());
        props.setProperty("projectName", projectName);
        props.setProperty("tagName", tagName);
        props.setProperty("instrumentationClassPattern", instrumentationClassPattern);

        props.setProperty("testStatePattern", testName);
        props.store(out, null);
        out.close();
    }

    public static String replaceLast(String string, String substring, String replacement)
    {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
                + string.substring(index+substring.length());
    }

    public static Path changeMavenFile(Path projectRoot, String testName) throws IOException {
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
        valuesMap.put("testToExecute",  replaceLast(testName, ".", "#"));

        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        String resolvedString = sub.replace(content);

        // Store as pom.xml
        Path pomFile = Paths.get(projectRoot.toString(), "dcdPom.xml");
        Files.write(pomFile, resolvedString.getBytes("UTF-8"));
        return pomFile;
    }

    public static void runMaven(Path newPomFile) throws IOException, MavenInvocationException {
        // Create request
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(newPomFile.toFile());
        request.setGoals(Collections.singletonList("test"));


        Invoker invoker = new DefaultInvoker();

        InvocationResult result = invoker.execute(request);

        // Check if it returned successfully
        if (result.getExitCode() != 0) {
            throw new IOException("Error in executing loader: Program did not terminate with code 0");
        }
    }
}
