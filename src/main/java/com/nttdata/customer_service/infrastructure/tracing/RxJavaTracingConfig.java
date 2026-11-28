package com.nttdata.customer_service.infrastructure.tracing;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.context.annotation.Configuration;

/**
 * Propaga la traza (span y traceId) a través de las cadenas de RxJava.
 *
 * <p>WebFlux guarda la traza en el hilo del request, pero los resultados de MongoDB llegan en otro hilo y RxJava
 * no la lleva consigo: las llamadas que vienen después (por ejemplo, a transaction-service) salían como trazas
 * aparte. Este hook toma una foto del contexto al suscribirse y la restaura cada vez que llega un resultado.</p>
 */
@Configuration
public class RxJavaTracingConfig {

    private static final ContextSnapshotFactory SNAPSHOTS = ContextSnapshotFactory.builder().build();

    /** Registra los hooks globales de RxJava al arrancar la aplicación. */
    @PostConstruct
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void registerHooks() {
        RxJavaPlugins.setOnSingleSubscribe((source, observer) ->
                new TracedSingleObserver(observer, SNAPSHOTS.captureAll()));
        RxJavaPlugins.setOnMaybeSubscribe((source, observer) ->
                new TracedMaybeObserver(observer, SNAPSHOTS.captureAll()));
        RxJavaPlugins.setOnCompletableSubscribe((source, observer) ->
                new TracedCompletableObserver(observer, SNAPSHOTS.captureAll()));
        RxJavaPlugins.setOnFlowableSubscribe((source, subscriber) ->
                new TracedSubscriber(subscriber, SNAPSHOTS.captureAll()));
    }

    /** Quita los hooks al apagar la aplicación. */
    @PreDestroy
    public void resetHooks() {
        RxJavaPlugins.reset();
    }

    private record TracedSingleObserver<T>(SingleObserver<T> actual, ContextSnapshot snapshot)
            implements SingleObserver<T> {
        @Override
        public void onSubscribe(Disposable d) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onSubscribe(d);
            }
        }

        @Override
        public void onSuccess(T value) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onError(e);
            }
        }
    }

    private record TracedMaybeObserver<T>(MaybeObserver<T> actual, ContextSnapshot snapshot)
            implements MaybeObserver<T> {
        @Override
        public void onSubscribe(Disposable d) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onSubscribe(d);
            }
        }

        @Override
        public void onSuccess(T value) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onError(e);
            }
        }

        @Override
        public void onComplete() {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onComplete();
            }
        }
    }

    private record TracedCompletableObserver(CompletableObserver actual, ContextSnapshot snapshot)
            implements CompletableObserver {
        @Override
        public void onSubscribe(Disposable d) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onSubscribe(d);
            }
        }

        @Override
        public void onComplete() {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            try (ContextSnapshot.Scope s = snapshot.setThreadLocals()) {
                actual.onError(e);
            }
        }
    }

    private record TracedSubscriber<T>(Subscriber<? super T> actual, ContextSnapshot snapshot)
            implements FlowableSubscriber<T> {
        @Override
        public void onSubscribe(Subscription s) {
            try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                actual.onSubscribe(s);
            }
        }

        @Override
        public void onNext(T value) {
            try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                actual.onNext(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                actual.onError(e);
            }
        }

        @Override
        public void onComplete() {
            try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                actual.onComplete();
            }
        }
    }
}