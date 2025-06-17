package org.example.mollyapi.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
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
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS;
    private String BASE_PACKAGES = "org.example.mollyapi.order.v4.event.message.*";

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
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, BASE_PACKAGES);


        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 공통 ConcurrentKafkaListenerContainerFactory 생성 메서드
    private ConcurrentKafkaListenerContainerFactory<String, Object> listenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> replyTemplate){
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        factory.setConsumerFactory(consumerFactory);
        if (replyTemplate != null) {
            factory.setReplyTemplate(replyTemplate);
        }

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate(),
                (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(BACK_OFF_INTERVAL, MAX_ATTEMPTS));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /// Todo - 중복된 코드 메서드화하기 (or 방법 찾아보기)
    // (aggregator : order.validated) Consumer
    @Bean
    public ConsumerFactory<String, Object> orderValidatedConsumer(){
        return consumerFactory("aggregator");
    }

    // (aggregator : order.validated) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderValidatedContainerFactory(){
        return listenerContainerFactory(orderValidatedConsumer(),null);
    }

    // (order : order.validated.failed) Consumer
    @Bean
    public ConsumerFactory<String, Object> orderFailedConsumer(){
        return consumerFactory("order-fail-group");
    }

    // (order : order.validated.failed) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderFailedContainerFactory(){
        return listenerContainerFactory(orderFailedConsumer(),null);
    }

    // (order : order.validated.failed) Consumer
    @Bean
    public ConsumerFactory<String, Object> orderCompleteConsumer(){
        return consumerFactory("order-complete-group");
    }

    // (order : order.validated.failed) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderCompleteContainerFactory(){
        return listenerContainerFactory(orderCompleteConsumer(),null);
    }

    // (point : order.initiate) Consumer
    @Bean
    public ConsumerFactory<String, Object> orderInitiateConsumer() { return consumerFactory("point-deduct-consumer-group");}

    // (point : order.initiate) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderInitiateContainerFactory(){
        return listenerContainerFactory(orderInitiateConsumer(), null);
    }

    // (aggregator : point.deduct.completed) Consumer
    @Bean
    public ConsumerFactory<String, Object> pointDeductedConsumer() { return consumerFactory("aggregator");}

    // (point : order.initiate) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> pointDeductedContainerFactory(){
        return listenerContainerFactory(pointDeductedConsumer(), null);
    }

    // (aggregator : stock.reserve.completed) Consumer
    @Bean
    public ConsumerFactory<String, Object> stockReserveRequestConsumer() { return consumerFactory("aggregator");}

    // (aggregator : stock.reserve.completed) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> stockReserveRequestContainerFactory(){
        return listenerContainerFactory(stockReserveRequestConsumer(), null);
    }


    // (aggregator : stock.reserve.requested) Consumer
    @Bean
    public ConsumerFactory<String, Object> stockReservedConsumer() { return consumerFactory("stock-reserve-group");}

    // (aggregator : stock.reserve.requested) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> stockReservedContainerFactory(){
        return listenerContainerFactory(stockReservedConsumer(), null);
    }

    // (payment : payment.requested) Consumer
    @Bean
    public ConsumerFactory<String, Object> paymentRequestConsumer() { return consumerFactory("payment-consumer-group");}

    // (payment : payment.requested) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> paymentRequestContainerFactory(){
        return listenerContainerFactory(paymentRequestConsumer(), null);
    }


    // (delivery, cart : order.completed) Consumer
    @Bean
    public ConsumerFactory<String, Object> orderCompletedConsumer() { return consumerFactory("payment-consumer-group");}

    // (delivery, cart : order.completed) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderCompletedContainerFactory(){
        return listenerContainerFactory(orderCompletedConsumer(), null);
    }



    // (aggregator : order.completed) Consumer
    @Bean
    public ConsumerFactory<String, Object> paymentSuccessConsumer() { return consumerFactory("payment-consumer-group");}

    // (delivery, cart : order.completed) Listener
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> paymentSuccessContainerFactory(){
        return listenerContainerFactory(paymentSuccessConsumer(), null);
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
