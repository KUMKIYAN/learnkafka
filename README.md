# learnkafka

Stores events based on a retention time. Events are immutable. Consumer responsibility to keep track of consumed message.
Any consumer can access a message from the broker. Its a distributed streaming system. 
terminology producerAPI, consumerAPI, streamAPI, connectAPI
Topic is an entity in the kafka.
Each topic can have more than one Partition and they are independent 
Each Partition is an ordered, immutable sequence of record
Each record assigned a sequential number called offset.


to start zookeeper : HYD-C02DR61UMD6N:bin kikumar$ ./zookeeper-server-start.sh ../config/zookeeper.properties
add the below properties in /Users/kikumar/Documents/kafka_2.12-3.3.1/config/server.properties
listeners=PLAINTEXT://localhost:9092
auto.create.topics.enable=fals
to start kafka server: HYD-C02DR61UMD6N:bin kikumar$ ./kafka-server-start.sh ../config/server.properties
to create topic : ./kafka-topics.sh --create --topic test-topic --replication-factor 1 --partitions 4 --bootstrap-server localhost:9092

to start producer : ./kafka-console-producer.sh --broker-list localhost:9092 --topic test-topic 
to start consumer : ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning


Kafka message with two properties:
Each message will be sent to the partitioner part of the kafka producer. Because these messages do not have any key, they will be processed in round rabbit technique.
The partitioner calculates partition value using hashing algorithm by taking the message key as input.
Same key always goto the same producer.

Consumer Offset : Consumer 1 read data till offset 3 and crashed. After that some data is there in the partition. 
After sometime Consumer 1 up. Now it reads data from offset 4 only by using the property  __consumer_offsets.
consumer offsets behave like a bookmark for consumer to start reading message from the point it left off.


to see the list of topics: ./kafka-topics.sh --bootstrap-server localhost:9092 --list 

consumer groups: group.id is mandatory and plays major role when it comes to scalable message consumption. 

Since Consumer polling is single thread; it introduces some delay for consumer to read data from all partitions. So it is not real time processing.

Now let us scale up consumer A. ( we need to specify (same group id if we have to scale up same consumer) Group 1 here). It reduces delay to some extent.


consumer groups are used for scalable message consumption

each different application will have a unique consumer group

who manages the consumer group ?

	Kafka broker manages the consumer groups
	Kafka broker acts as a group co-ordinator

It is mandatory for developers to give group id in case we do not want to create a new group id automatically / if we want group consumers under one group id.

To see list of consumer group list : ./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

If we want to consume data using consumer group, create consumers under one group id.
Created a topic with 4 partitions Let us create two consumers with same group id.  
Since we have 2 consumers and the data willl be equally consuming.

How to create spring boot kafka producer

create spring boot project with web, spring for apache-kafka, lombok 
enable annotation processing
create domains
create controller
when we try with controller we should get response (at this point we have not implemented kafka)
kafka template will be created based on some property in application yml file.

  kafka:
    template:
      default-topic: "library-events"
    producer:
      bootstrap-servers: localhost:9092,localhost:9093,localhost:9094
      key-serializer: org.apache.kafka.common.serialization.IntegerSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    admin:
      properties:
        bootstrap.servers: localhost:9092,localhost:9093,localhost:9094


now create a component class with autowire kafkaTemplate and call sendDefautl method.
it creates ListenableFuture<SendResult<Integer, String>> listenableFuture.
call addCallback method from listenableFuture which overrid onFailure and onSuccess.

We can create topic from the configuration class using TopicBuilder as well. this is not recommened for prod and preprod. 
This is becuase for prod and preprod some one should create topic for us. we specify that in the yml file.
For local testing we can follow this or we can create manually and give topic name we have to give in yml file.

kafka template sendDefault method automatically read topic from yml file and send data to that topic. 
there are two chances one is sucess or error which are handled in the addCallback method.

Behind the scenes: Partitioner follows the round robin approach to publish messages into the partition in the particular topic. 
The app behaves like an asynchronous call.

We can also use send method in place of SendDefault. 
The difference between send and sendDefault method is for sendDefault we have to specify topic name in the yml file. 
For the send method we can pass the topic name as a parameter. So we can use multiple topics in a context.

Send method has many overloaded methods. One among them is Producer Record. producer record contains 5 things topic-name, partition, key, value and headers.

In order to make synchronous create a method which return SearchResult object. in this method apply get on top of sendDefault method on Kafka Template.
SendResult<Integer, String> sendResult = libraryEventProducer.sendLibraryEventSynchronous(libraryEvent); // call it in the controller
        
Create SpringBoot test: to test this controller. the path in test package should be same as main package still controller. otherwise test are not recongnized.
https://stackoverflow.com/questions/47487609/unable-to-find-a-springbootconfiguration-you-need-to-use-contextconfiguration

If brokers are down the test can not run independently. For that we need embedded kafka dependency.
Once the test is completed , embedded kafka will shutdown. We do not need external kafka.
We have to override some Test properties and add @EmbededKafka, @SpringBootTest @TestPropertySource

Create a @PutMapping to updated the LibraryEvent. becuase it is put method - the LibraryEventId should not be null.


Inside integrationTest create restTemplate, EmbededKafkaBroker and consumer and call URL

for controller unit testing create @WebMvcTest,  @AutoConfigureWebMvc call the endpoint

for LibraryEventProducerUnitTest extend ExtendWith(MockitoExtension.class). one catch is our method was void so we can only test success scenario. in order to test
both succes and failure scenario do return SendResult<Integer, String>  on usage of kafka Template.
in unit test use SettableListenableFuture future.


Kafka producer configurations acks=1 means message is written to leader, all means replica as well and 0 means no gaurantee (not recommended)
retries = 10
retry.backoff.ms = 100
override in application yml file


consumer: consumes message and store in DB. same dependancies like producer and enable annotation processing

consumers are two type : MessageListenerContainer (KafkaMessageListenerContainer and ConcurrentMessageListenerContainer) and 
@KafkaListern which uses ConcurrentMessageListenerContainer (most easy way)

 @KafkaListener(topics = {"library-events"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        log.info("Consumer Record : {}  ", consumerRecord);
        libraryEventsService.processLibraryEvent(consumerRecord);
    }


Kafka Consumer Config

bootstrap-servers
key-deserializer
value-deserializer
group-id

All these properties are read by @EnableKafka

From producer post a record and can see in the spring kafka consumer. behind the scene it work as follow
class KafkaAnnotationDrivenConfiguration is reponsible for create consumer with the help of KafkaProperties class.
kafkaListenerContainerFactory which returns ConcurrentKafkaListenerContainerFactory

consumer group and Rebalancing : 
When we have a single instance the all partitions are assigned to this single instance. When we whave mulitiple instance of same applicaiton 
with same consumer group id then patition ownership distributed between apps using group co-ordinator. Whenever there is a new instance 
then partition distribution will happen again. Group co-ordinator triggers rebalance.
Shut down a consumer and start it again. We notice that consumer wont read the messages that were already have read by another consumer. 
It happens with Committing offsets. When a poll is happening again it know from where it has to read data.


@Configuration
@EnableKafka
public class LibraryEventConsumerConfiguration {
    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(4); 
        // Another way of scale the application is setCurrency as shown below. This is useful
        // Now a dedicated thread is allocated a partition. This is useful when you are not running your consumer in a cloud like environment.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

}

@Component
@Slf4j
public class LibraryEventConsumerManualOffset implements AcknowledgingMessageListener<Integer, String> {

    @Override
    @KafkaListener(topics = {"library-events"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord, Acknowledgment acknowledgment) {
        log.info("Consumer Record : {}  ", consumerRecord);
        acknowledgment.acknowledge();
    }

}

Any exception happens in the code, the Kafka Listener logging Error Handler will capture and print that in the console.
Consumer Integration testing : for consumer integration testing require producer and vice versa. So include producer data in the yml file

https://www.youtube.com/watch?v=5sjFn9BsAds - Testing Spring Boot Applications by Andy Wilkinson @ Spring I/O 2019
https://www.youtube.com/watch?v=HUZppOYDoXs ( Test containers )


@Configuration
@EnableKafka
@Slf4j
public class LibraryEventConsumerConfiguration {

//Error hanlder method is very important method. in this method we have two approaches one is fixed back off and the other one is exponential back off. we can choose eitehr one
//In this method we have used exceptionsToRetryList or exceptionsToIgnoreList for retry mechanisum we can use either one here.
//We use setRetryListeners to log what is happening for each retry and setting this errorhandler in below kafkaListenerContainerFactory

    public DefaultErrorHandler errorHandler(){
        var fixedBackOff = new FixedBackOff(1000L, 2);
        var exceptionsToIgnoreList = List.of(IllegalArgumentException.class);
        var exceptionsToRetryList = List.of(IllegalArgumentException.class);
        var expBackOff = new ExponentialBackOff();
        expBackOff.setInitialInterval(1_000L);
        expBackOff.setMultiplier(2.0);
        expBackOff.setMaxInterval(2_000L);
        //var errorHandler =  new DefaultErrorHandler(fixedBackOff);
        var errorHandler =  new DefaultErrorHandler(expBackOff);
        //exceptionsToIgnoreList.forEach(errorHandler::addNotRetryableExceptions);
        exceptionsToRetryList.forEach(errorHandler::addRetryableExceptions);
        errorHandler.setRetryListeners(((record, ex, deliveryAttempt) -> {
            log.info("Failed Record in Retry Listener, Exception : {} , deliveryAttempt : {} ", ex.getMessage(), deliveryAttempt);

        }));
        return errorHandler;
    }
    
    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(4);
        factory.setCommonErrorHandler(errorHandler());
      //  factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}



Recovery Types:  

Approach 1: Reprocess the failed record again. eg: service the consumer interacts with is temporarily down
	
	option 1: publish failed message to Retry topic

	option 2: save the failed message in a DB and retry with a scheduler 

Approach 2: Discard the message and move on eg: invalide message : parsing error, Invalid Event

	option 1: publish the fialied record into DeadLetter Topic for tracking purpose

	opiton 2: Save the failed record into DB for tracking purpose

When we have two listener maintaining group id is best practice. it will not recognize group id given in yml file when we have more than one consumer. We have to explicitly define it.

Kafka Listener Endpoint Registry is the one which holds all the Kafka consumers.

Kafka logs : /private/tmp/kafka-logs

Consumer with message headers:  ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic library-events.DLT --from-beginning --property print.headers=true --property print.timestamp=true

