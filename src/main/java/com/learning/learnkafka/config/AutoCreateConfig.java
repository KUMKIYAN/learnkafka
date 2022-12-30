package com.learning.learnkafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("local")
public class AutoCreateConfig {

    @Bean
    public NewTopic libraryEvents(){
        return TopicBuilder.name("library-events")
                .partitions(4)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic libraryEventsRetry(){
        return TopicBuilder.name("library-events.RETRY")
                .partitions(4)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic libraryEventsDlt(){
        return TopicBuilder.name("library-events.DLT")
                .partitions(4)
                .replicas(1)
                .build();
    }

}