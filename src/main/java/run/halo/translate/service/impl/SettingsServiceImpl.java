package run.halo.translate.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.SettingFetcher;
import run.halo.translate.service.PostService;
import run.halo.translate.service.SettingsService;
import run.halo.translate.service.TranslateService;
import run.halo.translate.vo.SystemTranslateParam;
import run.halo.translate.vo.ThymeConfig;

/**
 * A default implementation of {@link PostService}.
 *
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
@Component
@AllArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final TranslateService translateService;

    @Override
    public Mono<ServerResponse> copySettings(ThymeConfig thymeConfig) {
        List<String> langs = thymeConfig.getLangs();
        JSONObject entries = thymeConfig.getData();

        List<Mono<Void>> translationMonos = new ArrayList<>();

        for (String lang : langs) {
            translationMonos.add(recursionJSON(entries, lang));
        }

        return Flux.merge(translationMonos)
            .then(ServerResponse.ok().bodyValue(thymeConfig));
    }

    private Mono<Void> recursionJSON(JSONObject entries, String lang) {
        List<Mono<Void>> translationMonos = new ArrayList<>();

        for (String key : entries.keySet()) {
            Object value = entries.get(key);
            if (value instanceof String str) {
                // parse string
                JSONObject jsonObject = JSONUtil.parseObj(str);
                if (jsonObject.isEmpty() || jsonObject.getJSONObject("cn") == null) {
                    continue;
                }
                JSONObject cn = jsonObject.getJSONObject("cn");
                JSONObject target = jsonObject.getJSONObject(lang.toLowerCase());

                for (String cnKey : cn.keySet()) {
                    // 判断value类型，有String和JSONObject和JSONArray
                    if (cn.get(cnKey) instanceof String) {
                        String cnValue = cn.getStr(cnKey);
                        if (StringUtils.isBlank(cnValue)) {
                            continue;
                        }

                        Mono<Void> translateAndSet =
                            translateService.translate(new SystemTranslateParam(cnValue, lang))
                                .flatMap(translated -> {
                                    JSONObject json = JSONUtil.parseObj(translated);
                                    String content =
                                        json.getJSONArray("translations").getJSONObject(0)
                                            .getStr("text");
                                    target.set(cnKey, content);
                                    entries.set(key, jsonObject.toString());
                                    return Mono.empty();
                                });

                        translationMonos.add(translateAndSet);
                    }

                    if (cn.get(cnKey) instanceof JSONArray) {
                        // 先获取cn.getJSONArray(cnKey)，然后复制一份
                        JSONArray cnValue = cn.getJSONArray(cnKey);
                        // 复制一份
                        JSONArray targetValue = JSONUtil.parseArray(cnValue.toString());
                        // 遍历JSONArray
                        for (int i = 0; i < targetValue.size(); i++) {
                            if (targetValue.get(i) instanceof JSONObject) {
                                JSONObject cnValue1 = targetValue.getJSONObject(i);
                                // 遍历JSONObject
                                for (String cnKey1 : cnValue1.keySet()) {
                                    String cnValue2 = cnValue1.getStr(cnKey1);
                                    if (StringUtils.isBlank(cnValue2)) {
                                        continue;
                                    }

                                    Mono<Void> translateAndSet =
                                        translateService.translate(new SystemTranslateParam(cnValue2, lang))
                                            .flatMap(translated -> {
                                                JSONObject json = JSONUtil.parseObj(translated);
                                                String content =
                                                    json.getJSONArray("translations").getJSONObject(0)
                                                        .getStr("text");
                                                cnValue1.set(cnKey1, content);
                                                entries.set(key, jsonObject.toString());
                                                return Mono.empty();
                                            });

                                    translationMonos.add(translateAndSet);
                                }
                            }
                            if (targetValue.get(i) instanceof String) {
                                String cnValue1 = targetValue.getStr(i);
                                if (StringUtils.isBlank(cnValue1)) {
                                    continue;
                                }

                                int finalI = i;
                                Mono<Void> translateAndSet =
                                    translateService.translate(new SystemTranslateParam(cnValue1, lang))
                                        .flatMap(translated -> {
                                            JSONObject json = JSONUtil.parseObj(translated);
                                            String content =
                                                json.getJSONArray("translations").getJSONObject(0)
                                                    .getStr("text");
                                            targetValue.set(finalI, content);
                                            entries.set(key, jsonObject.toString());
                                            return Mono.empty();
                                        });

                                translationMonos.add(translateAndSet);
                            }
                        }
                        target.set(cnKey, targetValue);
                        entries.set(key, jsonObject.toString());
                    }

                    if (cn.get(cnKey) instanceof JSONObject) {
                        JSONObject cnValue = cn.getJSONObject(cnKey);
                        JSONObject targetValue = JSONUtil.parseObj(cnValue.toString());
                        for (String cnKey1 : cnValue.keySet()) {
                            String cnValue1 = cnValue.getStr(cnKey1);
                            if (StringUtils.isBlank(cnValue1)) {
                                continue;
                            }

                            Mono<Void> translateAndSet =
                                translateService.translate(new SystemTranslateParam(cnValue1, lang))
                                    .flatMap(translated -> {
                                        JSONObject json = JSONUtil.parseObj(translated);
                                        String content =
                                            json.getJSONArray("translations").getJSONObject(0)
                                                .getStr("text");
                                        targetValue.set(cnKey1, content);
                                        entries.set(key, jsonObject.toString());
                                        return Mono.empty();
                                    });

                            translationMonos.add(translateAndSet);
                        }
                        target.set(cnKey, targetValue);
                        entries.set(key, jsonObject.toString());
                    }
                }
            }
        }

        return Flux.merge(translationMonos).then();
    }
}