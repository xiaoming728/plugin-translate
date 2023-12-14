package run.halo.translate.service;

import java.util.List;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface SettingsService {
    /**
     * 复制设置
     *
     * @param sourceLang 源lang
     * @param langs 沿着
     * @return {@link Mono}<{@link ServerResponse}>
     */
    Mono<ServerResponse> copySettings(String sourceLang, List<String> langs);
}
