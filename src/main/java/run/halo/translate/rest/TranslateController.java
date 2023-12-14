package run.halo.translate.rest;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ApiVersion;
import run.halo.translate.service.PostService;

@ApiVersion("v1alpha1")
@RequestMapping("/translate")
@RestController
public class TranslateController {

    private PostService postService;

    @PostMapping("/post")
    public Mono<ServerResponse> starting(String postName, List<String> categorys) {
        return postService.copyPost(postName, categorys);
    }
}