package run.halo.translate.service;

import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import run.halo.translate.rest.PostTranslateRequest;
import run.halo.translate.rest.SystemTranslateParam;


public interface TranslateService {
    Mono<ServerResponse> copyPost(PostTranslateRequest postTranslateRequest);

    /**
     * 翻译
     *
     * @param systemTranslateParam 系统翻译参数
     * @return 字符串
     */
    Mono<String> translate2(SystemTranslateParam systemTranslateParam);
}
