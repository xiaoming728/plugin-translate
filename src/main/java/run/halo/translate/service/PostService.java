package run.halo.translate.service;

import java.util.List;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import run.halo.translate.rest.PostRequest;
import run.halo.translate.rest.SystemTranslateParam;


public interface PostService  {
    Mono<ServerResponse> copyPost(PostRequest postRequest);

    /**
     * 翻译
     *
     * @param systemTranslateParam 系统翻译参数
     * @return 字符串
     */
    Mono<String> translate2(SystemTranslateParam systemTranslateParam);
}
