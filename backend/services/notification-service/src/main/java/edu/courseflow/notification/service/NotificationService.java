package edu.courseflow.notification.service;

import edu.courseflow.notification.dto.NotificationDtos.CreateNotificationRequestDto;
import edu.courseflow.notification.dto.NotificationDtos.NotificationDto;
import edu.courseflow.notification.dto.NotificationDtos.NotificationPreferenceDto;
import edu.courseflow.notification.dto.NotificationDtos.UpsertPreferenceRequestDto;
import edu.courseflow.notification.model.Notification;
import edu.courseflow.notification.repository.NotificationRepository;
import edu.courseflow.notification.web.ForbiddenException;
import edu.courseflow.commonlibrary.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final NotificationDeliveryDispatcher dispatcher;

    public NotificationService(NotificationRepository notifications,
                               NotificationDeliveryDispatcher dispatcher) {
        this.notifications = notifications;
        this.dispatcher = dispatcher;
    }

    public List<NotificationDto> listForUser(String userId, boolean unreadOnly) {
        return notifications.listForUser(userId, unreadOnly);
    }

    @Transactional
    public NotificationDto create(CreateNotificationRequestDto request) {
        // TODO(training-day-11-impl): Harden notification creation.
        // Step 1: Accept trusted event/admin input only and validate recipient/channel.
        // Step 2: Deduplicate by eventId where available.
        // Step 3: Persist inbox row, then dispatch only through enabled user channels.
        Notification notification = notifications.insertEntity(
                request.userId(), request.notificationType(), request.title(), request.body());
        return dispatcher.dispatch(notification);
    }

    @Transactional
    public void markRead(UUID notificationId, String userId) {
        // TODO(training-day-11-impl): Harden mark-read ownership.
        // Step 1: Load notification and compare recipient userId with CurrentUser-derived caller id.
        // Step 2: Allow platform admin override only if controller grants it explicitly.
        // Step 3: Update readAt idempotently so repeated clicks are safe.
        NotificationDto notification = notifications.find(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        if (!notification.userId().equals(userId)) {
            throw new ForbiddenException("FORBIDDEN_NOT_OWNER");
        }
        notifications.markRead(notificationId);
    }

    public List<NotificationPreferenceDto> preferences(String userId) {
        return notifications.preferences(userId);
    }

    @Transactional
    public NotificationPreferenceDto upsertPreference(UpsertPreferenceRequestDto request) {
        return notifications.upsertPreference(request);
    }
}
