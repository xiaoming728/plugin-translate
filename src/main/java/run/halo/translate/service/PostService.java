package run.halo.translate.service;

import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import run.halo.translate.vo.PostRequest;
import run.halo.translate.vo.SystemTranslateParam;


public interface PostService  {
    Mono<ServerResponse> copyPost(PostRequest postRequest);

    /**
     * 翻译
     *
     * @param systemTranslateParam 系统翻译参数
     * @return 字符串
     */
    Mono<String> translate(SystemTranslateParam systemTranslateParam);
}
