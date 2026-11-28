package com.nttdata.customer_service.infrastructure.adapter.out.cache.context;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Conecta las llamadas HTTP salientes con la traza del request entrante.
 *
 * <p>El caso de uso es RxJava y el {@code WebClient} es Reactor: al cruzar de uno a otro se pierde el contexto
 * de Reactor y la llamada saldría como una traza aparte en Zipkin. Aquí se toma el span actual y se vuelve a
 * poner en el contexto del {@code Mono}, para que la llamada aparezca como hija.</p>
 */
public final class TraceContext {

    private TraceContext() {
    }

    /**
     * Ejecuta la llamada con el span actual como padre.
     *
     * @param call     llamada HTTP (ya envuelta en el circuit breaker)
     * @param registry registro de observaciones de Micrometer
     * @param <T>      tipo de la respuesta
     * @return la misma llamada, conectada a la traza actual
     */
    public static <T> Mono<T> withCurrentTrace(Mono<T> call, ObservationRegistry registry) {
        return Mono.defer(() -> {
            Observation parent = registry.getCurrentObservation();
            return parent == null
                    ? call
                    : call.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, parent));
        });
    }

    /**
     * Ejecuta la llamada con el span actual como padre (versión para respuestas con varios elementos).
     *
     * @param call     llamada HTTP (ya envuelta en el circuit breaker)
     * @param registry registro de observaciones de Micrometer
     * @param <T>      tipo de cada elemento
     * @return la misma llamada, conectada a la traza actual
     */
    public static <T> Flux<T> withCurrentTrace(Flux<T> call, ObservationRegistry registry) {
        return Flux.defer(() -> {
            Observation parent = registry.getCurrentObservation();
            return parent == null
                    ? call
                    : call.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, parent));
        });
    }

}
