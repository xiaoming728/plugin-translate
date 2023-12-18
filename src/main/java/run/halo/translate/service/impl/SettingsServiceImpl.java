package run.halo.translate.service.impl;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.SettingFetcher;
import run.halo.translate.service.PostService;
import run.halo.translate.service.SettingsService;

/**
 * A default implementation of {@link PostService}.
 *
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
@Component
public class SettingsServiceImpl implements SettingsService {

    private final SettingFetcher settingFetcher;

    public SettingsServiceImpl(SettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }
    @Override
    public Mono<ServerResponse> copySettings(String sourceLang, List<String> langs) {
        //TODO 循环settings，递归翻译翻译中文内容到对应的语言langs中
        //TODO 返回前端。
        return null;
    }
}
