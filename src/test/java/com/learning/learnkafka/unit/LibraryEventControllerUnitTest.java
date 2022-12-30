package com.learning.learnkafka.unit;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.learnkafka.domain.Book;
import com.learning.learnkafka.domain.LibraryEvent;
import com.learning.learnkafka.producer.LibraryEventProducer;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureWebMvc
public class LibraryEventControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    @MockBean
    LibraryEventProducer libraryEventProducer;

    @Test
    void postLibraryEvent() throws Exception {
        Book book = Book.builder().bookAuthor("Dilip").bookId(123).bookName("programming with Big C 1").build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(null).book(book).build();

        String json = objectMapper.writeValueAsString(libraryEvent);
        System.out.println(json);
      //  doNothing().when(libraryEventProducer).sendLibraryEventUsingSendProducerRecord(isA(LibraryEvent.class));
        mockMvc.perform(post("/v1/libraryevent")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void postLibraryEvent_4XX() throws Exception {
        Book book = Book.builder().bookAuthor(null).bookId(null).bookName("programming with Big C 1").build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(null).book(book).build();

        String json = objectMapper.writeValueAsString(libraryEvent);
        System.out.println(json);
        doNothing().when(libraryEventProducer).sendLibraryEventUsingSendProducerRecord(isA(LibraryEvent.class));

        String expectedErrorMessage = "book.bookAuthor - must not be blank, book.bookId - must not be null";

        mockMvc.perform(post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(expectedErrorMessage));

    }

    @Test
    void putLibraryEvent() throws Exception {
        Book book = Book.builder().bookAuthor("Dilip").bookId(123).bookName("programming with Big C 1").build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(123).book(book).build();

        String json = objectMapper.writeValueAsString(libraryEvent);
        System.out.println(json);
        doNothing().when(libraryEventProducer).sendLibraryEventUsingSendProducerRecord(isA(LibraryEvent.class));

        mockMvc.perform(put("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void putLibraryEvent_4XX() throws Exception {
        Book book = Book.builder().bookAuthor("Dilip").bookId(123).bookName("programming with Big C 1").build();
        LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(null).book(book).build();

        String json = objectMapper.writeValueAsString(libraryEvent);
        System.out.println(json);
        doNothing().when(libraryEventProducer).sendLibraryEventUsingSendProducerRecord(isA(LibraryEvent.class));

        mockMvc.perform(put("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Please pass the Library event id"));
    }

}
