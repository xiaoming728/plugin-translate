package run.halo.translate.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.SettingFetcher;
import run.halo.translate.service.TranslateService;
import run.halo.translate.service.SettingsService;

/**
 * A default implementation of {@link TranslateService}.
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
        JSONObject entries = JSONUtil.parseObj(sourceLang);
        for (String lang : langs) {
            recursionJSON(entries, lang);
        }
        return ServerResponse.ok().bodyValue(entries);
    }

    private void recursionJSON(JSONObject entries, String lang) {
        for (String key : entries.keySet()) {
            Object value = entries.get(key);
            if (value instanceof String str) {
                String translate = translate(str, lang);
                entries.set(key, translate);
            } else if (value instanceof JSONArray array) {
                for (int i = 0; i < array.size(); i++) {
                    Object object = array.get(i);
                    if (object instanceof String str) {
                        String translate = translate(str, lang);
                        array.set(i, translate);
                    } else if (object instanceof JSONObject jsonObject) {
                        recursionJSON(jsonObject, lang);
                    }
                }
            } else if (value instanceof JSONObject jsonObject) {
                recursionJSON(jsonObject, lang);
            }
        }
    }

    private String translate(String str, String lang) {
        return str;
    }

}
