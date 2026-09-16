package com.gym.workload.repository;

import com.gym.workload.model.TrainerWorkloadDocument;
import com.gym.workload.model.TrainerWorkloadDocument.MonthEntry;
import com.gym.workload.model.TrainerWorkloadDocument.YearEntry;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest
@EnableMongoRepositories(basePackageClasses = TrainerWorkloadRepository.class)
@EntityScan(basePackageClasses = TrainerWorkloadDocument.class)
class TrainerWorkloadRepositoryIT {

    @Container
    static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
    }

    @Autowired
    private TrainerWorkloadRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private TrainerWorkloadDocument newDocument(String username) {
        List<MonthEntry> months = new ArrayList<>(List.of(new MonthEntry(8, 60)));
        List<YearEntry> years = new ArrayList<>(List.of(new YearEntry(2026, months)));

        TrainerWorkloadDocument document = new TrainerWorkloadDocument();
        document.setTrainerUsername(username);
        document.setTrainerFirstName("John");
        document.setTrainerLastName("Doe");
        document.setTrainerStatus(true);
        document.setYears(years);
        return document;
    }

    @Test
    void save_thenFindByTrainerUsername_returnsDocumentWithNestedStructureIntact() {
        repository.save(newDocument("trainer.one"));

        Optional<TrainerWorkloadDocument> found = repository.findByTrainerUsername("trainer.one");

        assertThat(found)
                .as("A saved document must be retrievable from real MongoDB by its username")
                .isPresent();
        assertThat(found.get().getYears())
                .as("Nested year/month structure must round-trip through MongoDB unchanged")
                .hasSize(1);
        assertThat(found.get().getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("Nested training summary duration must round-trip through MongoDB unchanged")
                .isEqualTo(60);
    }

    @Test
    void findByTrainerUsername_forUnknownUsername_returnsEmpty() {
        Optional<TrainerWorkloadDocument> found = repository.findByTrainerUsername("does.not.exist");

        assertThat(found)
                .as("Looking up a username with no stored document should return an empty Optional")
                .isEmpty();
    }

    @Test
    void save_onExistingUsername_updatesDocumentInPlaceRatherThanDuplicating() {
        repository.save(newDocument("trainer.two"));

        TrainerWorkloadDocument toUpdate = repository.findByTrainerUsername("trainer.two").orElseThrow();
        toUpdate.getYears().get(0).getMonths().get(0).setTrainingSummaryDuration(90);
        repository.save(toUpdate);

        TrainerWorkloadDocument updated = repository.findByTrainerUsername("trainer.two").orElseThrow();

        assertThat(updated.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("Saving a document with an existing _id (trainerUsername) must update it, not create a duplicate")
                .isEqualTo(90);
        assertThat(repository.count())
                .as("Updating an existing document must not create a second document for the same username")
                .isEqualTo(1);
    }

    @Test
    void compoundNameIndex_isCreatedOnStartup() {
        List<Document> indexes = mongoTemplate.getCollection("trainer_workload_summary")
                .listIndexes()
                .into(new ArrayList<>());

        boolean hasNameIndex = indexes.stream()
                .anyMatch(index -> "idx_trainer_name".equals(index.getString("name")));

        assertThat(hasNameIndex)
                .as("The compound index on trainerFirstName/trainerLastName declared on the document "
                        + "must actually be created in MongoDB")
                .isTrue();
    }
}