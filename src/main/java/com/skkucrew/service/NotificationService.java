package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.domain.Member;
import com.skkucrew.domain.Notification;
import com.skkucrew.service.Dtos.NotificationItem;
import com.skkucrew.service.Dtos.NotificationList;
import com.skkucrew.service.Dtos.UnreadCount;
import com.skkucrew.store.DataStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 서비스 내 알림 센터 (FR-NOTI-01, 03, 06).
 * 알림 생성은 이벤트가 일어나는 곳(지원 제출, 마감, 결과 발표)에서 DataStore.notify로 한다.
 */
@Service
public class NotificationService {

    private final DataStore store;

    public NotificationService(DataStore store) {
        this.store = store;
    }

    public NotificationList list(Member member, boolean unreadOnly) {
        store.closeExpiredPostings(); // 마감 시각이 지난 공고의 N-02 알림을 먼저 만든다
        List<Notification> all = store.notificationsOf(member.id);
        int unread = (int) all.stream().filter(n -> !n.read).count();
        List<NotificationItem> items = all.stream()
                .filter(n -> !unreadOnly || !n.read)
                .map(NotificationService::toItem)
                .toList();
        return new NotificationList(unread, items);
    }

    public UnreadCount unreadCount(Member member) {
        store.closeExpiredPostings();
        return new UnreadCount((int) store.notificationsOf(member.id).stream().filter(n -> !n.read).count());
    }

    public NotificationItem markRead(Member member, Long id) {
        Notification n = store.notification(id).orElseThrow(() -> ApiException.notFound("알림"));
        if (!n.memberId.equals(member.id)) {
            throw ApiException.forbidden("본인 알림만 읽음 처리할 수 있습니다.");
        }
        n.read = true;
        return toItem(n);
    }

    public UnreadCount markAllRead(Member member) {
        store.notificationsOf(member.id).forEach(n -> n.read = true);
        return new UnreadCount(0);
    }

    private static NotificationItem toItem(Notification n) {
        return new NotificationItem(n.id, n.type, n.title, n.body, n.targetScreen, n.postingId, n.applicationId,
                n.read, n.createdAt);
    }
}
