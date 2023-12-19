package run.halo.translate.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.http.client.HttpClient;
import run.halo.app.core.extension.content.Category;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.translate.vo.PostRequest;
import run.halo.translate.vo.SystemTranslateParam;
import run.halo.translate.service.PostService;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * A default implementation of {@link PostService}.
 *
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
@Component
@AllArgsConstructor
public class PostServiceImpl implements PostService {

    private final ReactiveExtensionClient client;

    private final ReactiveSettingFetcher settingFetcher;


    private final HttpClient httpClient = HttpClient.create()
        .followRedirect(true);
    private final WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();

    @Override
    public Mono<ServerResponse> copyPost(PostRequest postRequest) {
        Mono<Post> postMono = client.get(Post.class, postRequest.postName());
        // 获取文章标题和内容
        AtomicReference<Post> thisPost = new AtomicReference<>();
        AtomicReference<String> title = new AtomicReference<>();
        AtomicReference<String> snapshot = new AtomicReference<>();
        postMono.doOnSuccess(post -> {
            thisPost.set(post);
            title.set(post.getSpec().getTitle());
            snapshot.set(post.getSpec().getHeadSnapshot());
        }).subscribe();
        // 获取categorys元数据lang获取语言，然后把post标题和内容翻译成对应的语言
        List<Post> posts = new ArrayList<>();
        for (String category : postRequest.categorys()) {
            Mono<Category> categoryMono = client.get(Category.class, category);
            categoryMono.subscribe(category1 -> {
                Map<String, String> annotations = category1.getMetadata().getAnnotations();
                String lang = annotations.get("lang");

                // 翻译标题
                String titleTranslate = translate(new SystemTranslateParam(title.get(), lang)).block();
                // 翻译内容
                String bodyTranslate = translate(new SystemTranslateParam(snapshot.get(), lang)).block();
                // 保存翻译后的文章
                Post post = new Post();
                BeanUtil.copyProperties(thisPost, post);
                post.getMetadata().setName("post");
                post.getSpec().setTitle(titleTranslate);
                post.getSpec().setHeadSnapshot(bodyTranslate);
                post.getSpec().setCategories(List.of(category));
                posts.add(post);
                client.create(post);
            });
        }
        return ServerResponse.ok().bodyValue(true);
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
            .map(setting -> setting.get("url").asText())
            .defaultIfEmpty("https://api-free.deepl.com/v2/translate");
    }

    Mono<String> getMomentToken() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("token").asText())
            .defaultIfEmpty("");
    }
}
