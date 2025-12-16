package oleborn.gateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Step6RoutesWithRetray
 *
 * Данный класс представляет собой ШЕСТОЙ шаг изучения Spring Cloud Gateway
 * и демонстрирует механизм Retry как часть Gateway pipeline.
 *
 * Цель этого шага — показать, что Gateway может повторять backend-вызов
 * при временных инфраструктурных ошибках,
 * НЕ вмешиваясь в бизнес-логику и НЕ обрабатывая исключения вручную.
 *
 * Архитектурный смысл:
 * - Retry — это GatewayFilter, применяемый на уровне маршрута.
 * - Retry повторяет ТОЛЬКО backend-вызов.
 * - Retry не перезапускает pre-filters и не изменяет семантику ошибки.
 *
 * Ключевая идея:
 * - Retry ≠ try/catch.
 * - Retry ≠ ExceptionHandler.
 * - Retry — это декларативная политика устойчивости.
 *
 * Этот класс активируется через Spring Profile "step6"
 * и используется исключительно в учебных целях.
 */
@Configuration
@Profile("step6")
public class Step6RoutesWithRetray {

    /**
     * Создание RouteLocator с настроенным Retry-фильтром.
     *
     * RouteLocator — центральный интерфейс Spring Cloud Gateway,
     * предоставляющий набор маршрутов для обработки запросов.
     *
     * @param builder RouteLocatorBuilder —
     *                DSL-строитель маршрутов,
     *                позволяющий декларативно описывать
     *                Route, Predicate и Filters.
     * @return RouteLocator с маршрутом,
     *         использующим Retry как часть pipeline.
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Определение маршрута с идентификатором "retry-demo".
                // Идентификатор используется для логирования,
                // мониторинга и отладки.
                .route("retry-demo", r -> r

                        // Predicate path("/retry/**") проверяет,
                        // начинается ли путь входящего запроса с "/retry/".
                        //
                        // Только такие запросы попадут
                        // под данный маршрут и смогут быть повторены.
                        .path("/retry/**")

                        // Блок filters(...) определяет GatewayFilter'ы,
                        // применяемые ТОЛЬКО к этому маршруту.
                        .filters(f -> f

                                // stripPrefix(1) удаляет первый сегмент пути.
                                //
                                // Пример:
                                //   Входящий запрос: /retry/ping
                                //   После stripPrefix(1): /ping
                                //
                                // Это необходимо, если backend
                                // не ожидает маршрутизирующий префикс.
                                .stripPrefix(1)

                                // retry(...) — встроенный GatewayFilter,
                                // предоставляемый Spring Cloud Gateway.
                                //
                                // Он отвечает за повтор backend-вызова
                                // при определённых условиях.
                                .retry(retry -> retry

                                        // setRetries(3) задаёт количество повторных попыток.
                                        //
                                        // Это означает:
                                        // - первый вызов + 3 повтора;
                                        // - всего до 4 попыток обращения к backend.
                                        .setRetries(3)

                                        // setStatuses(...) ограничивает Retry
                                        // ТОЛЬКО определёнными HTTP-статусами.
                                        //
                                        // В данном случае:
                                        // - retry срабатывает ТОЛЬКО при 500 Internal Server Error;
                                        // - бизнес-ошибки (4xx) не затрагиваются.
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR)

                                        // setMethods(...) ограничивает Retry
                                        // по HTTP-методам.
                                        //
                                        // Это КРИТИЧЕСКИ ВАЖНО:
                                        // - retry безопасен только для идемпотентных методов;
                                        // - POST/PUT могут привести к дублированию данных.
                                        //
                                        // Здесь явно разрешён только GET.
                                        .setMethods(HttpMethod.GET)
                                )
                        )

                        // URI указывает backend-сервис,
                        // к которому будет выполняться вызов.
                        //
                        // Именно этот вызов будет повторяться
                        // Retry-фильтром при возникновении ошибок.
                        .uri("http://localhost:8082")
                )

                // Завершение конфигурации маршрутов.
                // После вызова build() RouteLocator
                // становится доступным Gateway.
                .build();
    }
}
