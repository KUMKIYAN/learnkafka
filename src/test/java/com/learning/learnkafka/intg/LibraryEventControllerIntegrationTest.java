package com.learning.learnkafka.intg;

import com.learning.learnkafka.domain.Book;
import com.learning.learnkafka.domain.LibraryEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.KafkaUtils;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
                    "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"
                    })
public class LibraryEventControllerIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setup(){
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown(){
        consumer.close();
    }

    @Test
    @Timeout(5)
    void postLibraryEvent() throws InterruptedException {
        Book book = Book.builder().bookAuthor("Dilip").bookId(123).bookName("programming with Big C 1").build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(null).book(book).build();
        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", MediaType.APPLICATION_JSON.toString());
        HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent, headers);
        ResponseEntity<LibraryEvent> responseEntity = restTemplate.exchange(
                "/v1/libraryevent", HttpMethod.POST,request, LibraryEvent.class
        );
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        Thread.sleep(3000);
        ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.getSingleRecord( consumer, "library-events");
        String expectedRecord = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":123," +
                "\"bookName\":\"programming with Big C 1\",\"bookAuthor\":\"Dilip\"}}";
        String value = consumerRecord.value();
        assertEquals(expectedRecord, value);

    }


    @Test
    @Timeout(5)
    void putLibraryEvent() throws InterruptedException {
        Book book = Book.builder().bookAuthor("Dilip").bookId(123).bookName("programming with Big C 1").build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(123).book(book).build();
        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", MediaType.APPLICATION_JSON.toString());
        HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent, headers);
        ResponseEntity<LibraryEvent> responseEntity = restTemplate.exchange(
                "/v1/libraryevent", HttpMethod.PUT,request, LibraryEvent.class
        );
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Thread.sleep(3000);
        ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.getSingleRecord( consumer, "library-events");
        String expectedRecord = "{\"libraryEventId\":123,\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":123," +
                "\"bookName\":\"programming with Big C 1\",\"bookAuthor\":\"Dilip\"}}";
        String value = consumerRecord.value();
        assertEquals(expectedRecord, value);
    }

}
