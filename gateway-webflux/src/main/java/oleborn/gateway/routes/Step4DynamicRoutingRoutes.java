package oleborn.gateway.routes;

import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URI;

/**
 * Step4DynamicRoutingRoutes
 * <p>
 * Данный класс представляет собой ЧЕТВЁРТЫЙ шаг изучения Spring Cloud Gateway
 * и демонстрирует динамическую маршрутизацию с использованием Java DSL.
 * <p>
 * Цель этого шага — показать, что Gateway способен принимать РЕШЕНИЯ
 * о маршрутизации ВО ВРЕМЯ выполнения запроса, а не только на этапе конфигурации.
 * <p>
 * Архитектурный смысл:
 * - Route и Predicate по-прежнему определяют, какие запросы
 * попадают в Gateway pipeline.
 * - Однако конечный backend может быть выбран динамически,
 * на основе данных запроса (headers, attributes и т.д.).
 * <p>
 * Ключевая идея:
 * - YAML позволяет описывать только статические маршруты.
 * - Java DSL позволяет ПРОГРАММИРОВАТЬ поведение Gateway.
 * - Именно здесь проходит принципиальная граница
 * между декларативной и императивной конфигурацией.
 * <p>
 * Этот класс активируется через Spring Profile "step4"
 * и используется исключительно в учебных целях.
 */

@Configuration
@Profile("step4")
public class Step4DynamicRoutingRoutes {

    /**
     * Создание RouteLocator с динамической маршрутизацией.
     * <p>
     * В данном методе используется кастомный GatewayFilter,
     * определённый напрямую через Java DSL,
     * без создания отдельного класса.
     *
     * @param builder RouteLocatorBuilder —
     *                DSL-строитель маршрутов Spring Cloud Gateway,
     *                позволяющий декларативно и программно
     *                описывать маршруты.
     * @return RouteLocator с маршрутом,
     * использующим динамическое определение backend.
     */
    @Bean
    public RouteLocator dynamicRoutes(RouteLocatorBuilder builder) {

        return builder.routes()
                // Определение маршрута с идентификатором "smart".
                // Идентификатор используется для логирования,
                // мониторинга и отладки.
                .route("smart", r -> r

                        // Predicate path("/smart/**") проверяет,
                        // начинается ли путь входящего запроса с "/smart/".
                        //
                        // Только запросы, удовлетворяющие этому условию,
                        // попадут в дальнейший Gateway pipeline.
                        .path("/smart/**")

                        /**
                         * КАСТОМНЫЙ GatewayFilter
                         * с ЯВНО ЗАДАННЫМ ПОРЯДКОМ.
                         *
                         * Ordered.HIGHEST_PRECEDENCE гарантирует,
                         * что фильтр выполнится ДО:
                         * - RouteToRequestUrlFilter
                         * - LoadBalancerClientFilter
                         * - HttpClientFilter
                         */
                        .filters(
                                f -> f
                                        .rewritePath("/smart/(?<segment>.*)", "/${segment}")
                                        .filter((exchange, chain) -> {

                                            // ServerWebExchange — центральный объект WebFlux,
                                            // представляющий состояние одного HTTP-запроса.
                                            //
                                            // Он содержит:
                                            // - ServerHttpRequest (request);
                                            // - ServerHttpResponse (response);
                                            // - attributes — контекст выполнения.
                                            //
                                            // Через exchange Gateway получает доступ
                                            // ко всем данным запроса.

                                            // Извлечение значения HTTP-заголовка "X-Target"
                                            // из входящего запроса.
                                            //
                                            // Заголовок используется как управляющий параметр
                                            // для выбора backend-сервиса.
                                            String target = exchange.getRequest()
                                                    // Получение объекта ServerHttpRequest
                                                    // — представления входящего HTTP-запроса.
                                                    .getHeaders()
                                                    // Получение первого значения заголовка "X-Target".
                                                    .getFirst("X-Target");

                                            // Инициализация базового URI по умолчанию.
                                            //
                                            // Это значение будет использовано,
                                            // если условие ниже не выполнится.
                                            String baseUri = "http://localhost:8081";

                                            // Условная логика маршрутизации.
                                            //
                                            // ВАЖНО:
                                            // - именно здесь появляется императивная логика;
                                            // - это невозможно выразить в YAML.
                                            if ("B".equalsIgnoreCase(target)) {
                                                // Если заголовок X-Target равен "B",
                                                // то запрос будет направлен на другой backend.
                                                baseUri = "http://localhost:8082";
                                            }

                                            // КЛЮЧЕВОЙ МОМЕНТ:
                                            // мы создаём НОВЫЙ Route во время выполнения запроса.
                                            // Gateway дальше будет работать УЖЕ С НИМ.
                                            Route newRoute = Route.async()
                                                    // id нужен только для логов и отладки
                                                    .id("dynamic-" + target)
                                                    // URI backend-сервиса
                                                    .uri(URI.create(baseUri))
                                                    // порядок роли не играет — Route уже выбран
                                                    .order(0)
                                                    // predicate всегда true,
                                                    // потому что маршрут уже выбран ранее
                                                    .predicate(e -> true)
                                                    .build();

                                            // Подменяем Route в exchange.
                                            // С этого момента Gateway СЧИТАЕТ,
                                            // что именно этот Route является текущим.
                                            exchange.getAttributes().put(
                                                    ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                                                    newRoute
                                            );

                                            // Явно кладём request URL,
                                            // чтобы системные фильтры
                                            // (RouteToRequestUrlFilter, NettyRoutingFilter)
                                            // использовали НОВЫЙ backend,
                                            // а не исходный route.uri().
                                            exchange.getAttributes().put(
                                                    ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                                                    URI.create(baseUri)
                                            );


                                            // GatewayFilterChain представляет собой
                                            // цепочку фильтров Gateway.
                                            //
                                            // Вызов chain.filter(exchange) передаёт управление
                                            // следующему фильтру в pipeline.
                                            //
                                            // Если этот вызов НЕ сделать,
                                            // pipeline будет остановлен (short-circuit).
                                            return chain.filter(exchange);
                                        }
                                )
                        )

                        // Обязательное указание uri().
                        //
                        // ДАЖЕ при динамической маршрутизации
                        // Route ОБЯЗАН иметь URI.
                        //
                        // Это значение используется как fallback
                        // и как техническое требование Gateway.
                        //
                        // Фактический URI будет переопределён
                        // через GATEWAY_REQUEST_URL_ATTR.
                        .uri("http://localhost:8081")
                )

                // Завершение конфигурации маршрутов.
                // После вызова build() RouteLocator
                // становится доступным Gateway.
                .build();
    }
}

