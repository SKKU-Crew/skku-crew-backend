package com.skkucrew.web;

import com.skkucrew.store.DataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "9. 개발용")
@RestController
@RequestMapping("/api/dev")
public class DevController {

    private final DataStore store;

    public DevController(DataStore store) {
        this.store = store;
    }

    @Operation(summary = "더미 데이터 초기화", description = "시연 녹화 전에 호출하면 처음 상태로 돌아간다")
    @PostMapping("/reset")
    public Map<String, String> reset() {
        store.reset();
        return Map.of("result", "OK");
    }
}
