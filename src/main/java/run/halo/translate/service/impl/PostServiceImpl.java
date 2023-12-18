package run.halo.translate.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.halo.app.core.extension.content.Category;
import run.halo.app.plugin.SettingFetcher;
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
public class PostServiceImpl implements PostService {

    private final ReactiveExtensionClient client;

    private final SettingFetcher settingFetcher;

    public PostServiceImpl(ReactiveExtensionClient client, SettingFetcher settingFetcher) {
        this.client = client;
        this.settingFetcher = settingFetcher;
    }

    @Override
    public Mono<ServerResponse> copyPost(String postName, List<String> categorys) {
        Mono<Post> postMono = client.get(Post.class, postName);
        // 获取categorys元数据lang获取语言，然后把post标题和内容翻译成对应的语言
        List<Post> posts = new ArrayList<>();
        for (String category : categorys) {
            Mono<Category> categoryMono = client.get(Category.class, category);
            categoryMono.subscribe(category1 -> {
                Map<String, String> annotations = category1.getMetadata().getAnnotations();
                String lang = annotations.get("lang");
                // 获取文章标题和内容
                String title = postMono.block().getSpec().getTitle();
                String snapshot = postMono.block().getSpec().getHeadSnapshot();
                // 翻译标题
                String titleTranslate = translate(title, lang);
                // 翻译内容
                String bodyTranslate = translate(snapshot, lang);
                // 保存翻译后的文章
                Post post = new Post();
                BeanUtil.copyProperties(postMono.block(), post);
                post.getMetadata().setName("post");
                post.getSpec().setTitle(titleTranslate);
                post.getSpec().setHeadSnapshot(bodyTranslate);
                post.getSpec().setCategories(List.of(category));
                posts.add(post);
                client.create(post);
            });
        }
        return Mono.just(posts).flatMap(postList -> ServerResponse.ok().build());
    }

    private String translate(String title, String lang) {
        JsonNode basic = settingFetcher.get("basic");
        String url = basic.get("url").asText();
        String key = basic.get("key").asText();
        String value = basic.get("value").asText();
        if (StringUtils.isBlank(url) || StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            throw new RuntimeException("请先配置翻译接口");
        }
        String result = null;
        WebClient webClient = WebClient.create();
        StringJoiner stringJoiner = new StringJoiner("&");
        stringJoiner.add("key=" + key);
        stringJoiner.add("value=" + value);
        stringJoiner.add("text=" + title);
        stringJoiner.add("to=" + lang);
        String uri = url + "?" + stringJoiner;
        Mono<String> stringMono = webClient.get().uri(uri).retrieve().bodyToMono(String.class);
        try {
            result = stringMono.block();
        } catch (Exception e) {
            log.error("翻译失败", e);
        }
        JSONObject jsonNode = JSONUtil.parseObj(result);
        return jsonNode.getStr("data");

    }
}
