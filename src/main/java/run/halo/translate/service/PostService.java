package run.halo.translate.service;

import java.util.List;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import run.halo.translate.rest.PostRequest;


public interface PostService  {
    Mono<ServerResponse> copyPost(PostRequest postRequest);
}
