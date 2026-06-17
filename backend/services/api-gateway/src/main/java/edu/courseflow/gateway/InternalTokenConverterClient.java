package edu.courseflow.gateway;

import reactor.core.publisher.Mono;

interface InternalTokenConverterClient {

    boolean enabled();

    boolean required();

    default boolean localIdentityMode() {
        return false;
    }

    Mono<String> exchange(String subjectToken);

    static InternalTokenConverterClient disabled() {
        return new InternalTokenConverterClient() {
            @Override
            public boolean enabled() {
                return false;
            }

            @Override
            public boolean required() {
                return false;
            }

            @Override
            public boolean localIdentityMode() {
                return false;
            }

            @Override
            public Mono<String> exchange(String subjectToken) {
                return Mono.empty();
            }
        };
    }
}
