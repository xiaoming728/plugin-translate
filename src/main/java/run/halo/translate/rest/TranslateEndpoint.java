package run.halo.translate.rest;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Theme;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.translate.service.SettingsService;
import run.halo.translate.service.TranslateService;
import run.halo.translate.service.PostService;
import run.halo.translate.vo.SystemTranslateParam;
import run.halo.translate.vo.ThymeConfig;

@Component
@RequiredArgsConstructor
public class TranslateEndpoint implements CustomEndpoint {

    private final TranslateService translateService;
    private final SettingsService settingsService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "plugin/Translate";

        return SpringdocRouteBuilder.route()
            .POST("/translate/posts", this::posts,
                builder -> builder.operationId("CreateBrowsingLog")
                    .description("Create a BrowsingLog.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder().implementation(PostTranslateRequest.class)
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder())
                        ))
                    .response(responseBuilder()
                        .implementation(Boolean.class))
            )
            .POST("translate/deepl", this::translate,
                builder -> builder.operationId("Translate")
                    .description("translate article")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(schemaBuilder()
                                .implementation(TranslateService.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Theme.class))
            )
            .POST("thyme/translate", this::thymeTranslate,
                builder -> builder.operationId("Translate")
                    .description("translate article")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(schemaBuilder()
                                .implementation(PostService.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Theme.class))
            )
            .build();

    }
    private Mono<ServerResponse> posts(ServerRequest request) {
        return request.bodyToMono(PostTranslateRequest.class)
            .flatMap(translateService::copyPost);
    }

    private Mono<ServerResponse> translate(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(SystemTranslateParam.class)
            .flatMap(translateService::translate)
            .flatMap(response ->
                ServerResponse.ok().bodyValue(response)
            );
    }

    private Mono<ServerResponse> thymeTranslate(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(ThymeConfig.class)
            .flatMap(settingsService::copySettings);
    }
}