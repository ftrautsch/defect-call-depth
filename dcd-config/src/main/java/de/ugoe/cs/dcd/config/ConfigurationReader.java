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

package de.ugoe.cs.dcd.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * @author Fabian Trautsch
 */
public class ConfigurationReader {
    private String projectName;
    private String tagName;
    private String username;
    private String password;
    private String hostname;
    private Integer port;
    private String authenticationDB;
    private String database;
    private Boolean sslEnabled;
    private Boolean debugEnabled;
    private String debugOut;
    private List<Pattern> instrumentationClassPattern;
    private List<Pattern> excludeClassPattern;
    private String testStatePattern;

    private static ConfigurationReader singleton;

    private ConfigurationReader() {
        try(FileInputStream fis = new FileInputStream(getDefectCallDepthHome() + "/defect-call-depth.properties")){
            java.util.Properties properties = new java.util.Properties();
            properties.load(fis);
            projectName = (String) properties.get("projectName");
            tagName = (String) properties.get("tagName");
            username = (String) properties.get("username");
            password = (String) properties.get("password");
            hostname = (String) properties.get("hostname");
            port = Integer.valueOf((String) properties.get("port"));
            authenticationDB = (String) properties.get("authenticationDB");
            database = (String) properties.get("database");
            sslEnabled = Boolean.parseBoolean((String) properties.get("sslEnabled"));
            debugEnabled = Boolean.parseBoolean((String) properties.get("debugEnabled"));
            debugOut = (String) properties.get("debugOut");
            instrumentationClassPattern = loadClassPattern((String) properties.get("instrumentationClassPattern"));
            excludeClassPattern = loadClassPattern((String) properties.get("excludeClassPattern"));
            testStatePattern = (String) properties.get("testStatePattern");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't load defect-call-depth.properties");
        }
    }

    public static synchronized ConfigurationReader getInstance() {
        if (singleton == null) {
            singleton = new ConfigurationReader();
        }
        return singleton;
    }

    public String getFirstInstrumentationClassPatternAsString() {
        Pattern firstPattern = instrumentationClassPattern.get(0);
        return firstPattern.toString().substring(0, firstPattern.toString().length()-1);
    }

    public String getTestStatePattern() {
        return testStatePattern;
    }

    private String getDefectCallDepthHome() {
        return System.getenv("DCD_HOME");
    }

    private ArrayList<Pattern> loadClassPattern(String instrumentationClassPattern) {
        ArrayList<Pattern> result = new ArrayList<>();

        if (instrumentationClassPattern == null || instrumentationClassPattern.isEmpty()) {
            return result;
        }

        StringTokenizer tokens = new StringTokenizer(instrumentationClassPattern, ",");
        while (tokens.hasMoreElements()) {
            result.add(Pattern.compile(tokens.nextToken().trim().toLowerCase()));
        }
        return result;
    }

    public Boolean isDebugEnabled() {
        return debugEnabled;
    }

    public String getDebugOut() {
        return debugOut;
    }

    public List<Pattern> getInstrumentationClassPattern() {
        return instrumentationClassPattern;
    }

    public List<Pattern> getExcludeClassPattern() {
        return excludeClassPattern;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port;
    }

    public String getAuthenticationDB() {
        return authenticationDB;
    }

    public String getDatabase() {
        return database;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getTagName() {
        return tagName;
    }

    public boolean getSSLEnabled() {
        return sslEnabled;
    }

    @Override
    public String toString() {
        return "projectName: "+getProjectName()+"; tagName: "+getTagName()
                +"; username: "+getUsername()+"; password: "+getPassword()+"; hostname: "
                +getHostname()+"; port: "+getPort()+"; authenticationDB: "+getAuthenticationDB()
                +"; database: "+getDatabase()+"; sslEnabled: "+getSSLEnabled()
                +"; instrumentationClassPattern: "+getInstrumentationClassPattern();
    }
}
