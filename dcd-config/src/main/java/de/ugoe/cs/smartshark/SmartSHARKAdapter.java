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

package de.ugoe.cs.smartshark;

import static java.util.Arrays.copyOfRange;
import static org.mongodb.morphia.aggregation.Group.addToSet;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.sum;

import de.ugoe.cs.config.ConfigurationReader;
import com.github.danielfelgar.morphia.Log4JLoggerImplFactory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import de.ugoe.cs.smartshark.model.Mutation;
import de.ugoe.cs.smartshark.model.MutationResult;
import de.ugoe.cs.smartshark.model.Project;
import de.ugoe.cs.smartshark.model.Tag;
import de.ugoe.cs.smartshark.model.TestState;
import de.ugoe.cs.smartshark.model.VCSSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.query.Query;


/**
 * @author Fabian Trautsch
 */
public class SmartSHARKAdapter {
    private static Logger logger = LogManager.getLogger(SmartSHARKAdapter.class);

    private final Morphia morphia = new Morphia();
    private Datastore datastore;
    private final ConfigurationReader config = ConfigurationReader.getInstance();
    private ObjectId commitId;
    private Map<String, Set<MutationResult>> mutationResultMap = new HashMap<>();
    private Map<ObjectId, Mutation> mutationMap = new HashMap<>();


    private static SmartSHARKAdapter singleton;

    private SmartSHARKAdapter() {
        connectToDatabase();
        setCommitId();
        setMutations();
    }

    public static synchronized SmartSHARKAdapter getInstance() {
        if (singleton == null) {
            singleton = new SmartSHARKAdapter();
        }
        return singleton;
    }

    private void setCommitId() {
        ObjectId projectId = datastore.createQuery(Project.class)
                .field("name").equal(config.getProjectName()).get().getId();

        // Get vcsSystem
        ObjectId vcsSystemId = datastore.createQuery(VCSSystem.class)
                .field("project_id").equal(projectId).get().getId();

        // Get commitID from tag
        commitId = datastore.createQuery(Tag.class)
                .field("vcs_system_id").equal(vcsSystemId)
                .field("name").equal(config.getTagName()).get().getCommitId();
    }

    private void connectToDatabase() {
        // Set up log4j logging
        MorphiaLoggerFactory.reset();
        MorphiaLoggerFactory.registerLogger(Log4JLoggerImplFactory.class);

        // Map models
        morphia.mapPackage("de.ugoe.cs.smartshark.model");

        logger.debug("Connecting to database...");
        // Create database connection
        MongoClientURI uri = new MongoClientURI(de.ugoe.cs.smartshark.Utils.createMongoDBURI(
                config.getUsername(),
                config.getPassword(),
                config.getHostname(),
                String.valueOf(config.getPort()),
                config.getAuthenticationDB(),
                config.getSSLEnabled()));
        MongoClient mongoClient = new MongoClient(uri);
        datastore = morphia.createDatastore(mongoClient, config.getDatabase());
        datastore.ensureIndexes();
    }

    private void setMutations() {
        // Get all mutations in a map to access them easier later
        // TODO instead of only the fist, create OR with all instrumentation class patterns
        datastore.createQuery(Mutation.class)
                .field("l_num").greaterThan(0)
                .field("location").startsWithIgnoreCase(config.getFirstInstrumentationClassPatternAsString())
                .forEach(mutation -> mutationMap.put(mutation.getId(), mutation));

    }

    public Mutation getMutationById(ObjectId mutationId) {
        return mutationMap.get(mutationId);
    }

    public Set<MutationResult> getMutationResultsForTestState(String testName) {
        return mutationResultMap.get(testName);
    }

    public Map<String,SortedSet<Integer>> getMutationsWithLines() {
        Map<String, SortedSet<Integer>> insertMutationsWithLines = new HashMap<>();

        // First get all test states with results
        Set<ObjectId> testStatesWithMutationResults = new HashSet<>();
        datastore.createQuery(TestState.class)
                .field("commit_id").equal(commitId)
                .field("mutation_res").exists()
                .field("mutation_res").notEqual(null)
                .project("_id", true)
                .project("name", true)
                .project("mutation_res", true)
                .forEach(
                        testState ->  {
                            mutationResultMap.put(testState.getName(), testState.getMutationResults());
                            testStatesWithMutationResults.add(testState.getId());
                        }
                );

        // Now query all mutations that are generated
        Query<TestState> query = datastore.createQuery(TestState.class)
                .field("_id").in(testStatesWithMutationResults);
        Iterator<Mutation> aggregate = datastore.createAggregation(TestState.class)
                .match(query)
                .unwind("mutation_res")
                .group("mutation_res.mutation_id")
                .aggregate(Mutation.class);

        aggregate.forEachRemaining(mutation -> {
            Mutation mut = mutationMap.get(mutation.getId());

            if(mut != null) {
                String[] locationParts = mut.getLocation().split("\\.");
                String className = String.join(".", Arrays.copyOfRange(locationParts, 0, locationParts.length-1));
                SortedSet<Integer> lines = insertMutationsWithLines.getOrDefault(className, new TreeSet<>());
                lines.add(mut.getLineNumber());
                insertMutationsWithLines.put(className, lines);
            }
        });

        return insertMutationsWithLines;
    }

    /*
    public Set<String> getTestNames(String vcsSystemURL, String tagName) {
        // Get vcsSystem
        ObjectId vcsSystemId = datastore.createQuery(VCSSystem.class)
                .field("url").equal(vcsSystemURL).get().getId();
        // Get commitID from tag
        ObjectId commitId = datastore.createQuery(Tag.class)
                .field("vcs_system_id").equal(vcsSystemId)
                .field("name").equal(tagName).get().getCommitId();

        Set<String> testStatesWithMutationResults = new HashSet<>();

        datastore.createQuery(TestState.class)
                .field("commit_id").equal(commitId)
                .field("mutation_res").exists()
                .field("mutation_res").notEqual(null)
                .project("name", true)
                .forEach(
                        testState ->  {
                            testStatesWithMutationResults.add(testState.getName()+"()");
                        }
                );

        return testStatesWithMutationResults;
    }
     */
}
