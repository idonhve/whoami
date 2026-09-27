package com.whoami.module.track.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.track.dto.SessionEndRequest;
import com.whoami.module.track.dto.SessionStartRequest;
import com.whoami.module.track.dto.TrackEventRequest;
import com.whoami.module.track.service.TrackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开埋点上报接口（免登录，sendBeacon 目标）：会话进入/结束 + 统一事件入口。 */
@RestController
@RequestMapping("/api/track")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @PostMapping("/session")
    public ApiResult<Void> startSession(
            @Valid @RequestBody SessionStartRequest request,
            HttpServletRequest httpRequest) {
        trackService.startSession(request, httpRequest);
        return ApiResult.ok();
    }

    /** sendBeacon 可能带空 body，required=false 兜底 */
    @PostMapping("/session/{sessionId}/end")
    public ApiResult<Void> endSession(
            @PathVariable("sessionId") String sessionId,
            @RequestBody(required = false) SessionEndRequest request) {
        trackService.endSession(sessionId, request);
        return ApiResult.ok();
    }

    @PostMapping("/event")
    public ApiResult<Void> recordEvent(
            @Valid @RequestBody TrackEventRequest request,
            HttpServletRequest httpRequest) {
        trackService.recordEvent(request, httpRequest);
        return ApiResult.ok();
    }
}
