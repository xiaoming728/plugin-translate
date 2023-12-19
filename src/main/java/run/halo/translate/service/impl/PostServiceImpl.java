package run.halo.translate.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Tag;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Ref;
import run.halo.app.infra.Condition;
import run.halo.app.infra.ConditionStatus;
import run.halo.translate.service.AbstractContentService;
import run.halo.translate.service.ContentRequest;
import run.halo.translate.service.ContentWrapper;

import run.halo.translate.service.PostQuery;
import run.halo.translate.service.PostRequest;
import run.halo.translate.service.PostService;

/**
 * A default implementation of {@link PostService}.
 *
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
@Component
public class PostServiceImpl extends AbstractContentService implements PostService {
    private final ReactiveExtensionClient client;

    public PostServiceImpl(ReactiveExtensionClient client) {
        super(client);
        this.client = client;
    }


    private Flux<Tag> listTags(List<String> tagNames) {
        if (tagNames == null) {
            return Flux.empty();
        }
        return Flux.fromIterable(tagNames)
            .flatMapSequential(tagName -> client.fetch(Tag.class, tagName));
    }

    private Flux<Category> listCategories(List<String> categoryNames) {
        if (categoryNames == null) {
            return Flux.empty();
        }
        return Flux.fromIterable(categoryNames)
            .flatMapSequential(categoryName -> client.fetch(Category.class, categoryName));
    }

    @Override
    public Mono<Post> draftPost(PostRequest postRequest) {
        return Mono.defer(
                () -> {
                    Post post = postRequest.post();
                    return getContextUsername()
                        .map(username -> {
                            post.getSpec().setOwner(username);
                            return post;
                        })
                        .defaultIfEmpty(post);
                }
            )
            .flatMap(client::create)
            .flatMap(post -> {
                var contentRequest =
                    new ContentRequest(Ref.of(post), post.getSpec().getHeadSnapshot(),
                        postRequest.content().raw(), postRequest.content().content(),
                        postRequest.content().rawType());
                return draftContent(post.getSpec().getBaseSnapshot(), contentRequest)
                    .flatMap(contentWrapper -> waitForPostToDraftConcludingWork(
                        post.getMetadata().getName(),
                        contentWrapper)
                    );
            })
            .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance));
    }

    private Mono<Post> waitForPostToDraftConcludingWork(String postName,
        ContentWrapper contentWrapper) {
        return Mono.defer(() -> client.fetch(Post.class, postName)
                .flatMap(post -> {
                    post.getSpec().setBaseSnapshot(contentWrapper.getSnapshotName());
                    post.getSpec().setHeadSnapshot(contentWrapper.getSnapshotName());
                    if (Objects.equals(true, post.getSpec().getPublish())) {
                        post.getSpec().setReleaseSnapshot(post.getSpec().getHeadSnapshot());
                    }
                    Condition condition = Condition.builder()
                        .type(Post.PostPhase.DRAFT.name())
                        .reason("DraftedSuccessfully")
                        .message("Drafted post successfully.")
                        .status(ConditionStatus.TRUE)
                        .lastTransitionTime(Instant.now())
                        .build();
                    Post.PostStatus status = post.getStatusOrDefault();
                    status.setPhase(Post.PostPhase.DRAFT.name());
                    status.getConditionsOrDefault().addAndEvictFIFO(condition);
                    return client.update(post);
                }))
            .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance));
    }

    @Override
    public Mono<Post> updatePost(PostRequest postRequest) {
        Post post = postRequest.post();
        String headSnapshot = post.getSpec().getHeadSnapshot();
        String releaseSnapshot = post.getSpec().getReleaseSnapshot();
        String baseSnapshot = post.getSpec().getBaseSnapshot();

        if (StringUtils.equals(releaseSnapshot, headSnapshot)) {
            // create new snapshot to update first
            return draftContent(baseSnapshot, postRequest.contentRequest(), headSnapshot)
                .flatMap(contentWrapper -> {
                    post.getSpec().setHeadSnapshot(contentWrapper.getSnapshotName());
                    return client.update(post);
                });
        }
        return Mono.defer(() -> updateContent(baseSnapshot, postRequest.contentRequest())
                .flatMap(contentWrapper -> {
                    post.getSpec().setHeadSnapshot(contentWrapper.getSnapshotName());
                    return client.update(post);
                }))
            .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                .filter(throwable -> throwable instanceof OptimisticLockingFailureException));
    }

    @Override
    public Mono<ContentWrapper> getHeadContent(String postName) {
        return client.get(Post.class, postName)
            .flatMap(post -> {
                String headSnapshot = post.getSpec().getHeadSnapshot();
                return getContent(headSnapshot, post.getSpec().getBaseSnapshot());
            });
    }

    @Override
    public Mono<ContentWrapper> getReleaseContent(String postName) {
        return client.get(Post.class, postName)
            .flatMap(post -> {
                String releaseSnapshot = post.getSpec().getReleaseSnapshot();
                return getContent(releaseSnapshot, post.getSpec().getBaseSnapshot());
            });
    }
}
