package com.sbqs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbqs.config.WebPushProperties;
import com.sbqs.dto.push.PushPublicKeyResponse;
import com.sbqs.dto.push.PushSubscriptionRequest;
import com.sbqs.dto.push.PushSubscriptionResponse;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.entity.WebPushDelivery;
import com.sbqs.entity.WebPushSubscription;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.repository.TicketRepository;
import com.sbqs.repository.WebPushDeliveryRepository;
import com.sbqs.repository.WebPushSubscriptionRepository;
import com.interaso.webpush.VapidKeys;
import com.interaso.webpush.WebPush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class WebPushService {
    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);
    private static final String BASE64_URL_PATTERN = "^[A-Za-z0-9_-]+={0,2}$";

    private final WebPushProperties properties;
    private final WebPushSubscriptionRepository subscriptionRepository;
    private final WebPushDeliveryRepository deliveryRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.interaso.webpush.WebPushService pushClient;

    public WebPushService(
            WebPushProperties properties,
            WebPushSubscriptionRepository subscriptionRepository,
            WebPushDeliveryRepository deliveryRepository,
            TicketRepository ticketRepository,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher) {
        this.properties = properties;
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.ticketRepository = ticketRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.pushClient = createPushClient(properties);
    }

    public PushPublicKeyResponse getPublicKey() {
        return new PushPublicKeyResponse(pushClient != null, pushClient == null ? "" : properties.getPublicKey());
    }

    @Transactional
    public PushSubscriptionResponse subscribe(PushSubscriptionRequest request, String userAgent) {
        requireConfigured();
        validateSubscription(request);
        User user = currentUserService.requireUser();
        if (!"CUSTOMER".equals(user.getRole())) {
            throw new RuntimeException("Chỉ khách hàng được đăng ký thông báo hàng đợi");
        }

        String endpoint = request.endpoint().trim();
        String endpointHash = sha256(endpoint);
        WebPushSubscription value = subscriptionRepository.findByEndpointHash(endpointHash)
                .orElseGet(WebPushSubscription::new);
        value.setUser(user);
        value.setEndpoint(endpoint);
        value.setEndpointHash(endpointHash);
        value.setP256dh(request.p256dh().trim());
        value.setAuthSecret(request.auth().trim());
        value.setUserAgent(trimToLength(userAgent, 500));
        value.setActive(true);
        value.setFailureCount(0);
        subscriptionRepository.save(value);
        publishCurrentNearTicket(user);
        return new PushSubscriptionResponse(true);
    }

    public PushSubscriptionResponse unsubscribe(PushSubscriptionRequest request) {
        User user = currentUserService.requireUser();
        subscriptionRepository.findByEndpointHash(sha256(request.endpoint().trim()))
                .filter(value -> value.getUser().getUserId().equals(user.getUserId()))
                .ifPresent(value -> {
                    value.setActive(false);
                    subscriptionRepository.save(value);
                });
        return new PushSubscriptionResponse(false);
    }

    public void sendTicketNotification(
            Long ticketId,
            String customerEmail,
            String notificationType,
            String title,
            String body) {
        if (pushClient == null || ticketId == null || customerEmail == null || customerEmail.isBlank()) {
            return;
        }
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return;

        List<WebPushSubscription> subscriptions =
                subscriptionRepository.findByUserEmailIgnoreCaseAndActiveTrue(customerEmail);
        String payload = payload(title, body, ticketId, notificationType);
        for (WebPushSubscription subscription : subscriptions) {
            sendOnce(ticket, subscription, notificationType, payload);
        }
    }

    private void sendOnce(
            Ticket ticket,
            WebPushSubscription subscription,
            String notificationType,
            String payload) {
        if (deliveryRepository.existsByTicketTicketIdAndSubscriptionSubscriptionIdAndNotificationType(
                ticket.getTicketId(), subscription.getSubscriptionId(), notificationType)) {
            return;
        }

        WebPushDelivery delivery = new WebPushDelivery();
        delivery.setTicket(ticket);
        delivery.setSubscription(subscription);
        delivery.setNotificationType(notificationType);
        delivery.setStatus("PENDING");
        try {
            delivery = deliveryRepository.saveAndFlush(delivery);
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }

        try {
            WebPush.SubscriptionState state = pushClient.send(
                    payload,
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuthSecret(),
                    Math.max(0, properties.getTtlSeconds()),
                    "ticket-" + ticket.getTicketId(),
                    WebPush.Urgency.High);
            if (state == WebPush.SubscriptionState.ACTIVE) {
                delivery.setStatus("SENT");
                delivery.setSentAt(LocalDateTime.now());
                subscription.setFailureCount(0);
                subscription.setLastSuccessAt(LocalDateTime.now());
            } else {
                delivery.setStatus("FAILED");
                subscription.setFailureCount(subscription.getFailureCount() + 1);
                subscription.setActive(false);
                log.info("Web Push subscription expired ticketId={} subscriptionId={}",
                        ticket.getTicketId(), subscription.getSubscriptionId());
            }
        } catch (Exception ex) {
            delivery.setStatus("FAILED");
            subscription.setFailureCount(subscription.getFailureCount() + 1);
            log.warn("Web Push failed ticketId={} subscriptionId={}",
                    ticket.getTicketId(), subscription.getSubscriptionId(), ex);
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        }
        deliveryRepository.save(delivery);
        subscriptionRepository.save(subscription);
    }

    private com.interaso.webpush.WebPushService createPushClient(WebPushProperties config) {
        if (!config.isConfigured()) {
            log.info("Web Push is disabled because VAPID keys are not configured");
            return null;
        }
        try {
            VapidKeys vapidKeys = VapidKeys.fromUncompressedBytes(
                    config.getPublicKey(), config.getPrivateKey());
            return new com.interaso.webpush.WebPushService(config.getSubject(), vapidKeys);
        } catch (Exception ex) {
            log.error("Web Push VAPID configuration is invalid; notifications are disabled", ex);
            return null;
        }
    }

    private void requireConfigured() {
        if (pushClient == null) throw new RuntimeException("Web Push chưa được cấu hình trên máy chủ");
    }

    private void validateSubscription(PushSubscriptionRequest request) {
        URI uri;
        try {
            uri = URI.create(request.endpoint().trim());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Push endpoint không hợp lệ");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean allowedHost = properties.getAllowedEndpointHosts().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> host.equals(value) || host.endsWith("." + value));
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443) || !allowedHost) {
            throw new RuntimeException("Push endpoint không thuộc nhà cung cấp trình duyệt được hỗ trợ");
        }
        if (!request.p256dh().matches(BASE64_URL_PATTERN) || !request.auth().matches(BASE64_URL_PATTERN)) {
            throw new RuntimeException("Khóa PushSubscription không hợp lệ");
        }
    }

    private void publishCurrentNearTicket(User user) {
        ticketRepository.findFirstByCustomerEmailAndStatusInOrderByCreatedAtDesc(
                        user.getEmail(), List.of("WAITING"))
                .ifPresent(ticket -> {
                    long peopleAhead = ticketRepository.countByQueueMachineAndStatusAndTicketNumberLessThan(
                            ticket.getQueueMachine(), "WAITING", ticket.getTicketNumber());
                    if (peopleAhead <= 3) {
                        applicationEventPublisher.publishEvent(new TicketQueueThresholdNotification(
                                ticket.getTicketId(), ticket.getCustomerEmail(),
                                ticket.getTicketNumber(), peopleAhead));
                    }
                });
    }

    private String payload(String title, String body, Long ticketId, String type) {
        try {
            return objectMapper.writeValueAsString(new PushPayload(
                    title, body, "/ticket", "ticket-" + ticketId + "-" + type, type));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Không thể tạo nội dung Web Push", ex);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record PushPayload(String title, String body, String url, String tag, String type) { }
}
