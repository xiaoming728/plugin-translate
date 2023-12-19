package run.halo.translate.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import org.markdown4j.Markdown4jProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.parameters.P;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Snapshot;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.Ref;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.translate.rest.PostTranslateRequest;
import run.halo.translate.service.ContentWrapper;
import run.halo.translate.service.PostRequest;
import run.halo.translate.service.PostService;
import run.halo.translate.service.TranslateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.translate.vo.SystemTranslateParam;

/**
 * A default implementation of {@link TranslateService}.
 *
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
@Component
@AllArgsConstructor
public class TranslateServiceImpl implements TranslateService {

    private final ReactiveExtensionClient client;

    private final ReactiveSettingFetcher settingFetcher;

    private final PostService postService;

    private final HttpClient httpClient = HttpClient.create()
        .followRedirect(true);
    private final WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();

    @Override
    public Mono<ServerResponse> copyPost(PostTranslateRequest postTranslateRequest) {
        Mono<Post> postMono = client.get(Post.class, postTranslateRequest.postName());
        return postMono.flatMap(post -> postService.getHeadContent(postTranslateRequest.postName())
            .flatMap(contentWrapper -> {
                return Flux.fromIterable(postTranslateRequest.categorys())
                    .flatMap(categoryName -> client.get(Category.class, categoryName)
                        .flatMap(category -> {
                            Map<String, String> annotations =
                                category.getMetadata().getAnnotations();
                            String lang = annotations.get("lang");
                            if (StringUtils.isBlank(lang)) {
                                lang = "en";
                            }
                            String finalLang = lang;
                            String finalLang1 = lang;
                            return  getContextUsername().flatMap(username -> translate(post.getSpec().getTitle(),
                                finalLang1).flatMap(title -> translate(contentWrapper.getRaw(), finalLang).flatMap(
                                    raw -> {
                                        contentWrapper.setRaw(raw);
                                        return translate(contentWrapper.getContent(),
                                            finalLang).flatMap(
                                            content -> {
                                                contentWrapper.setContent(content);
                                                Post.PostSpec postSpec = new Post.PostSpec();
                                                postSpec.setTitle(title);
                                                postSpec.setSlug(UUID.fastUUID().toString(false));
                                                postSpec.setAllowComment(true);
                                                postSpec.setDeleted(false);
                                                Post.Excerpt excerpt = new Post.Excerpt();
                                                excerpt.setAutoGenerate(true);
                                                postSpec.setExcerpt(excerpt);
                                                postSpec.setPriority(0);
                                                postSpec.setVisible(Post.VisibleEnum.PUBLIC);
                                                postSpec.setPublish(false);
                                                postSpec.setPinned(false);
                                                Post.PostStatus postStatus = new Post.PostStatus();
                                                //草稿箱，待发布状态
                                                postStatus.setPhase(Post.PostPhase.DRAFT.name());

                                                post.setSpec(postSpec);
                                                post.setStatus(postStatus);
                                                post.setMetadata(new Metadata());
                                                post.getMetadata()
                                                    .setName(UUID.fastUUID().toString(false));
                                                Snapshot.SnapShotSpec snapShotSpec =
                                                    new Snapshot.SnapShotSpec();
                                                snapShotSpec.setRawType(contentWrapper.getRawType());
                                                snapShotSpec.setRawPatch(StringUtils.defaultString(raw));
                                                snapShotSpec.setContentPatch(StringUtils.defaultString(content));

                                                snapShotSpec.setSubjectRef(Ref.of(post));

                                                snapShotSpec.setOwner(username);
                                                Snapshot snapshot = new Snapshot();
                                                snapshot.setSpec(snapShotSpec);
                                                //设置元数据才能保存
                                                snapshot.setMetadata(new Metadata());
                                                snapshot.getMetadata()
                                                    .setName(UUID.fastUUID().toString(false));
                                                MetadataUtil.nullSafeAnnotations(snapshot).put(Snapshot.KEEP_RAW_ANNO,
                                                    String.valueOf(true));
                                                return client.create(snapshot).flatMap(snapshot1 -> {
                                                    postSpec.setBaseSnapshot(
                                                        snapshot1.getMetadata().getName());
                                                    postSpec.setHeadSnapshot(
                                                        snapshot1.getMetadata().getName());
                                                    postSpec.setReleaseSnapshot(
                                                        snapshot1.getMetadata().getName());

                                                    post.getSpec().setCategories(
                                                        List.of(category.getMetadata().getName()));
                                                    post.getMetadata()
                                                        .setName(UUID.fastUUID().toString(false));
                                                    PostRequest postRequest = new PostRequest(post,
                                                        new PostRequest.Content(raw, content,
                                                            contentWrapper.getRawType()));
                                                    return postService.draftPost(postRequest);
                                                });
                                            });
                                    })));
                        })).then(ServerResponse.ok().build());
            }));
    }

    @Override
    public Mono<String> translate(SystemTranslateParam systemTranslateParam) {
        String text = systemTranslateParam.getText();
        String toLan = systemTranslateParam.getToLan();
        return getMomentUrl()
            .flatMap(url ->
                getMomentToken()
                    .flatMap(token -> {

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.add("Authorization", "DeepL-Auth-Key " + token);

                        String[] texts = {text};
                        net.minidev.json.JSONObject request = new net.minidev.json.JSONObject();
                        request.put("text", texts);
                        request.put("target_lang", toLan);

                        return webClient.post()
                            .uri(url)
                            .headers(httpHeaders -> httpHeaders.addAll(headers))
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(String.class);

                    })
            );
    }

    Mono<String> getMomentUrl() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("url").asText("https://api-free.deepl.com/v2/translate"))
            .defaultIfEmpty("https://api-free.deepl.com/v2/translate");
    }

    Mono<String> getMomentToken() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("token").asText(""))
            .defaultIfEmpty("");
    }

    private Mono<String> translate(String text, String toLan) {
        return getMomentUrl()
            .flatMap(url ->
                getMomentToken()
                    .flatMap(token -> {

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.add("Authorization", "DeepL-Auth-Key " + token);

                        String[] texts = {text};
                        net.minidev.json.JSONObject request = new net.minidev.json.JSONObject();
                        request.put("text", texts);
                        request.put("target_lang", toLan);
                        Mono<String> stringMono = webClient.post()
                            .uri(url)
                            .headers(httpHeaders -> httpHeaders.addAll(headers))
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(String.class);
                        return stringMono.flatMap(json -> {
                            JSONObject jsonObject = JSONUtil.parseObj(json);
                            String content =
                                jsonObject.getJSONArray("translations").getJSONObject(0)
                                    .getStr("text");
                            return Mono.just(content);
                        });

                    })
            );
    }


    public Mono<String> getContextUsername() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Principal::getName);
    }
}
