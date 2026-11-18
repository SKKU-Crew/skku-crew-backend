package com.skkucrew.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 설정.
 * Swagger UI는 Authorization 이름의 헤더 파라미터를 무시하므로, Bearer 보안 스킴을 등록해
 * 오른쪽 위 Authorize 버튼으로 데모 토큰을 넣게 한다.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "demoToken";

    @Bean
    public OpenAPI skkuCrewOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SKKU Crew Demo API")
                        .version("demo")
                        .description("최종 발표 시연용 백엔드. 오른쪽 위 Authorize 버튼에 demo-1(김민준, 지원자) 또는 "
                                + "demo-4(박지훈, 모집자)를 입력하면 로그인한 상태로 호출된다. "
                                + "처음 상태로 되돌리려면 POST /api/dev/reset."))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("토큰 값만 입력 (예: demo-1). 'Bearer '는 자동으로 붙는다.")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}
