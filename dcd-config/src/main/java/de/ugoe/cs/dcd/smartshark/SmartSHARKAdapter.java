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

package de.ugoe.cs.dcd.smartshark;

import com.github.danielfelgar.morphia.Log4JLoggerImplFactory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import de.ugoe.cs.dcd.config.ConfigurationReader;
import de.ugoe.cs.smartshark.model.Mutation;
import de.ugoe.cs.smartshark.model.MutationResult;
import de.ugoe.cs.smartshark.model.Project;
import de.ugoe.cs.smartshark.model.Tag;
import de.ugoe.cs.smartshark.model.TestState;
import de.ugoe.cs.smartshark.model.VCSSystem;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.mongodb.morphia.query.UpdateOperations;


/**
 * @author Fabian Trautsch
 */
public class SmartSHARKAdapter {
    private final static Logger logger = LogManager.getLogger(SmartSHARKAdapter.class);

    private final Morphia morphia = new Morphia();
    private Datastore datastore;
    private final ConfigurationReader config = ConfigurationReader.getInstance();
    private ObjectId commitId;
    private final Map<String, TestState> testStates = new HashMap<>();
    private final Map<ObjectId, Mutation> mutationMap = new HashMap<>();
    private final Map<String, SortedSet<Integer>> insertMutationsWithLines = new HashMap<>();



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

    public Set<String> getTestStateNames() {
        Set<String> testStatesWithMutationResults = new HashSet<>();
        datastore.createQuery(TestState.class)
                .field("commit_id").equal(commitId)
                .field("mutation_res").exists()
                .field("mutation_res").notEqual(null)
                .project("_id", true)
                .project("name", true)
                .forEach(
                        testState -> testStatesWithMutationResults.add(testState.getName())
                );
        return testStatesWithMutationResults;
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
        // We only query for one test state by design
        Query<TestState> query = datastore.createQuery(TestState.class)
                .field("commit_id").equal(commitId)
                .field("name").equal(config.getTestStatePattern());


        Query<MutationResult> mutationResultQuery = datastore.createQuery(MutationResult.class)
                .field("result").notEqual("NO_COVERAGE");

        // Aggregate: Get all mutation_ids that are in the mutation_res array of this test state
        Set<ObjectId> mutationIds = new HashSet<>();
        datastore.createAggregation(TestState.class)
                .match(query)
                .unwind("mutation_res")
                .match(mutationResultQuery)
                .group("mutation_res.mutation_id")
                .aggregate(Mutation.class)
                .forEachRemaining(mutation -> mutationIds.add(mutation.getId()));

        if(mutationIds.size() == 0) {
            logger.warn("No mutations found for test "+config.getTestStatePattern());
        }
        // 1) Query the mutations that have thse mutation_ids and put them in a map for later use.
        // 2) Get the class name and affected line for each mutation and put them in the map
        datastore.createQuery(Mutation.class)
                .field("id").in(mutationIds)
                .forEach(mut -> {
                    mutationMap.put(mut.getId(), mut);
                    String[] locationParts = mut.getLocation().split("\\.");
                    String className = String.join(".", Arrays.copyOfRange(locationParts, 0, locationParts.length-1));
                    SortedSet<Integer> lines = insertMutationsWithLines.getOrDefault(className, new TreeSet<>());
                    lines.add(mut.getLineNumber());
                    insertMutationsWithLines.put(className, lines);
                });
    }

    public Mutation getMutationById(ObjectId mutationId) {
        return mutationMap.get(mutationId);
    }

    public TestState getTestStateForName(String testName) {
        return datastore.createQuery(TestState.class)
                .field("commit_id").equal(commitId)
                .field("name").equal(testName).get();
    }

    public void storeTestState(TestState testState) {
        UpdateOperations<TestState> ops = datastore.createUpdateOperations(TestState.class)
                .set("mutation_res", testState.getMutationResults());
        datastore.update(testState, ops);
    }

    public Map<String,SortedSet<Integer>> getMutationsWithLines() {
        return insertMutationsWithLines;

    }
}
