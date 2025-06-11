package org.example.mollyapi.common.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> produce(String topic, Object message){
        return kafkaTemplate.send(topic, message)
                .whenComplete((result,ex) -> {
                    if (ex != null){
                        //dlq 로직 추가하기
                        log.error("Failed to send message{}, {}", topic, message, ex);
                    } else {
                        log.debug("Sucessfully sent message to topic {}: offset={}",
                                topic, result.getRecordMetadata().offset());
                    }
                });
    }

    public CompletableFuture<SendResult<String, Object>> produce(String topic, Object message, Long offset){
        return kafkaTemplate.send(topic, message);
    }

}
