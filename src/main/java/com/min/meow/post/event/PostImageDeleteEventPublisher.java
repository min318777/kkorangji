package com.min.meow.post.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostImageDeleteEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return;
        }
        eventPublisher.publishEvent(new PostImageDeleteEvent(imageKeys));
    }
}
