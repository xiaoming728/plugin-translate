package run.halo.translate.rest;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ApiVersion;
import run.halo.translate.entity.PostParam;
import run.halo.translate.service.PostService;
import run.halo.translate.service.SettingsService;

@ApiVersion("v1alpha1")
@RequestMapping("/translate")
@RestController
public class TranslateController {

    private PostService postService;

    private SettingsService settingsService;

    @PostMapping("/posts")
    public Mono<ServerResponse> posts(String postName, List<String> categorys) {
        return postService.copyPost(postName, categorys);
    }

    @PostMapping("/settings")
    public Mono<ServerResponse> settings(String sourceLang, List<String> langs) {
        return settingsService.copySettings(sourceLang, langs);
    }

}