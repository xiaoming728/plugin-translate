package run.halo.translate.service;

import java.util.List;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;


public interface PostService  {
    Mono<ServerResponse> copyPost(String postName, List<String> categorys);
}
