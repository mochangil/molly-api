package org.example.mollyapi.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.mollyapi.order.v4.event.message.BaseMessage;
import org.example.mollyapi.order.v4.event.message.suceess.OrderValidateSuccessMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS;
    private String BASE_PACKAGES = "org.example.mollyapi.order.v4";

    private static final Integer BACK_OFF_INTERVAL = 0;
    private static final Integer MAX_ATTEMPTS = 0;

    private Map<String, Object> baseConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        return props;
    }

    // Producer 설정
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = baseConfig();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.RETRIES_CONFIG, MAX_ATTEMPTS);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, BACK_OFF_INTERVAL);
        return new DefaultKafkaProducerFactory<>(props);
    }

    // KafkaTemplate 설정
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    // 공통 ConsumerFactory 생성 메서드
    private ConsumerFactory<String, Object> consumerFactory(String groupId){

        Map<String, Object> props = baseConfig();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, BASE_PACKAGES);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 공통 ConcurrentKafkaListenerContainerFactory 생성 메서드
    private ConcurrentKafkaListenerContainerFactory<String, Object> listenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> replyTemplate){
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // todo - dlq 설정
//        factory.setConsumerFactory(consumerFactory);
//        if (replyTemplate != null) {
//            factory.setReplyTemplate(replyTemplate);
//        }
//
//        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate(),
//                (record, ex) -> new TopicPartition(record.topic() + "-dlq", record.partition()));
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
//                new FixedBackOff(BACK_OFF_INTERVAL, MAX_ATTEMPTS));
//        factory.setCommonErrorHandler(errorHandler);
//
        return factory;
    }

    // (order.validated) Consumer
    @Bean
    public ConsumerFactory<String, Object> orderValidatedConsumer(){
        return consumerFactory("aggregator");
    }

    // (order.validated) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderValidatedContainerFactory(){
        return listenerContainerFactory(orderValidatedConsumer(),null);
    }



//    // ConsumerFactory
//    public ConsumerFactory<String, Object> consumerFactory(String groupId) {
//        Map<String, Object> props = baseConfig();
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//        props.put(JsonDeserializer.TRUSTED_PACKAGES, BASE_PACKAGES);
//        return new DefaultKafkaConsumerFactory<>(props);
//    }
//
//    // KafkaListenerContainerFactory
//
//    public ConcurrentKafkaListenerContainerFactory<String, Object> listenerContainerFactory(
//            ConsumerFactory<String, Object> consumerFactory
//    ){
//        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory);
//        return factory;
//    }
//
//    @Bean
//    public ConsumerFactory<String, Object> orderValidateGroupConsumer(){
//        return consumerFactory("aggregator");
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(){
//        return listenerContainerFactory(orderValidateGroupConsumer());
//    }
}
