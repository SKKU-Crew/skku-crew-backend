package com.skkucrew.web;

import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.NotificationItem;
import com.skkucrew.service.Dtos.NotificationList;
import com.skkucrew.service.Dtos.UnreadCount;
import com.skkucrew.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. 알림")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final AuthService authService;
    private final NotificationService notificationService;

    public NotificationController(AuthService authService, NotificationService notificationService) {
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Operation(summary = "알림 목록", description = "최신순. targetScreen으로 이동할 화면을 정한다 (SCR-08 내 지원 현황, SCR-12 지원자 목록, SCR-06 공고 상세)")
    @GetMapping
    public NotificationList list(@RequestHeader(value = "Authorization", required = false) String auth,
                                 @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.list(authService.require(auth), unreadOnly);
    }

    @Operation(summary = "안 읽은 알림 개수", description = "상단 종 아이콘 배지용 (FR-NOTI-01)")
    @GetMapping("/unread-count")
    public UnreadCount unreadCount(@RequestHeader(value = "Authorization", required = false) String auth) {
        return notificationService.unreadCount(authService.require(auth));
    }

    @Operation(summary = "알림 읽음 처리")
    @PostMapping("/{notificationId}/read")
    public NotificationItem read(@RequestHeader(value = "Authorization", required = false) String auth,
                                 @PathVariable Long notificationId) {
        return notificationService.markRead(authService.require(auth), notificationId);
    }

    @Operation(summary = "모두 읽음 처리", description = "FR-NOTI-06")
    @PostMapping("/read-all")
    public UnreadCount readAll(@RequestHeader(value = "Authorization", required = false) String auth) {
        return notificationService.markAllRead(authService.require(auth));
    }
}
