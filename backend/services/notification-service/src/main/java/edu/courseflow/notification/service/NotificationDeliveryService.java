package edu.courseflow.notification.service;

import edu.courseflow.notification.model.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {

    private final NotificationDeliveryPort deliveryPort;

    public NotificationDeliveryService(NotificationDeliveryPort deliveryPort) {
        this.deliveryPort = deliveryPort;
    }

    public void deliver(Notification notification) {
        // TODO(training-day-11-impl): Harden external delivery handling.
        // Step 1: Treat push/email delivery as best-effort after inbox persistence.
        // Step 2: Mark delivery status and capture failure reason.
        // Step 3: Leave failed rows retryable without rolling back the inbox write.
        if (!"DISPATCHING".equals(notification.getDeliveryStatus())) {
            notification.markDeliveryInProgress();
        }
        try {
            deliveryPort.deliver(notification);
            notification.markDelivered();
        } catch (RuntimeException ex) {
            notification.markDeliveryFailed(ex.getMessage());
        }
    }
}
