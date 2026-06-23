package com.sbqs.event;

import com.sbqs.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);

    private final ObjectProvider<KafkaTemplate<String, DomainEvent>> kafkaTemplateProvider;
    private final CurrentUserService currentUserService;
    private final boolean kafkaEnabled;
    private final String topic;

    public KafkaDomainEventPublisher(
            ObjectProvider<KafkaTemplate<String, DomainEvent>> kafkaTemplateProvider,
            CurrentUserService currentUserService,
            @Value("${sbqs.kafka.enabled:true}") boolean kafkaEnabled,
            @Value("${sbqs.kafka.topic.domain-events:sbqs.domain-events}") String topic) {

        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.currentUserService = currentUserService;
        this.kafkaEnabled = kafkaEnabled;
        this.topic = topic;
    }

    @Override
    public void publish(
            String type,
            String aggregateType,
            String aggregateId,
            Long branchId,
            Map<String, Object> payload) {

        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                type,
                aggregateType,
                aggregateId,
                branchId,
                currentActorEmail(),
                LocalDateTime.now(),
                payload == null ? Map.of() : payload);

        log.info(
                "Domain event type={} aggregateType={} aggregateId={} branchId={}",
                event.type(),
                event.aggregateType(),
                event.aggregateId(),
                event.branchId());

        if (!kafkaEnabled) {
            return;
        }

        KafkaTemplate<String, DomainEvent> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate is not available, skip eventId={}", event.eventId());
            return;
        }

        try {
            kafkaTemplate
                    .send(topic, event.aggregateType() + ":" + event.aggregateId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn(
                                    "Kafka publish failed topic={} eventId={} type={}",
                                    topic,
                                    event.eventId(),
                                    event.type(),
                                    ex);
                        }
                    });
        } catch (RuntimeException ex) {
            log.warn("Kafka publish rejected eventId={} type={}", event.eventId(), event.type(), ex);
        }
    }

    private String currentActorEmail() {
        try {
            return currentUserService.requireUser().getEmail();
        } catch (RuntimeException ex) {
            return "system";
        }
    }
}
