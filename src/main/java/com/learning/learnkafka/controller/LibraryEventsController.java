package com.learning.learnkafka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learning.learnkafka.domain.LibraryEvent;
import com.learning.learnkafka.domain.LibraryEventType;
import com.learning.learnkafka.producer.LibraryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
public class LibraryEventsController {

    @Autowired
    LibraryEventProducer libraryEventProducer;

    @PostMapping("/v1/libraryevent")
    public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent)
            throws JsonProcessingException, ExecutionException, InterruptedException {
        libraryEvent.setLibraryEventType(LibraryEventType.NEW);
        log.info("Before sending the message");

//       sendLibraryEventSynchronously using get method
//        SendResult<Integer, String> sendResult = libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);
//        log.info("Send Result is {}", sendResult.toString());

//        sendLibraryEvent using sendDefault method
//        libraryEventProducer.sendLibraryEvent(libraryEvent);

//        sendLibraryEvent using send method
//        libraryEventProducer.sendLibraryEventUsingSend(libraryEvent);

//        sendLibraryEvent using send method using producer record
        libraryEventProducer.sendLibraryEventUsingSendProducerRecord(libraryEvent);


        log.info("After sending the message");
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
    }


    //put
    @PutMapping("/v1/libraryevent")
    public ResponseEntity<?> putLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent)
            throws JsonProcessingException {
        if(null == libraryEvent.getLibraryEventId()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the Library event id");
        }
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEventProducer.sendLibraryEventUsingSend(libraryEvent);
        return ResponseEntity.status(HttpStatus.OK).body(libraryEvent);
    }
}
