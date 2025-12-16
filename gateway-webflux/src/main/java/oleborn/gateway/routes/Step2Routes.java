package oleborn.gateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Step2Routes
 *
 * Данный класс представляет собой ВТОРОЙ шаг изучения Spring Cloud Gateway.
 *
 * Цель этого шага — показать, как Gateway ВЫБИРАЕТ маршрут
 * на основании условий (predicates), а не просто проксирует все запросы подряд.
 *
 * Архитектурный смысл:
 * - Predicate — это условие, по которому Gateway решает,
 *   какой Route подходит для конкретного запроса.
 * - Route + Predicate = правило маршрутизации.
 * - Если predicate не совпал — маршрут считается НЕПОДХОДЯЩИМ.
 *
 * Что этот класс демонстрирует:
 * - path-based routing;
 * - наличие нескольких маршрутов;
 * - момент, где Gateway начинает "думать", а не просто проксировать.
 *
 * Ключевая идея:
 * - Route без predicate подходит всем запросам;
 * - Route с predicate подходит ТОЛЬКО запросам,
 *   удовлетворяющим условию.
 *
 * Этот класс активируется через Spring Profile "step2"
 * и используется исключительно в учебных целях.
 */
@Configuration
@Profile("step2")
public class Step2Routes {

    /**
     * Создание бина RouteLocator для демонстрации predicates.
     *
     * RouteLocator — центральный интерфейс Spring Cloud Gateway,
     * предоставляющий набор Route для обработки входящих запросов.
     *
     * Gateway агрегирует все бины RouteLocator
     * и использует их при маршрутизации запросов.
     *
     * @param builder RouteLocatorBuilder —
     *                DSL-строитель маршрутов,
     *                предоставляющий fluent API
     *                для описания Route, Predicate и Filters.
     * @return RouteLocator с двумя маршрутами,
     *         каждый из которых имеет собственный predicate.
     */
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {

        // Инициализация DSL для построения маршрутов.
        // С этого момента начинается декларативное описание Route.
        return builder.routes()

                // Определение первого маршрута с идентификатором "only-a".
                // Идентификатор маршрута используется:
                // - для логирования;
                // - для отладки;
                // - при работе с metadata.
                .route("only-a", r -> r

                        // Predicate path("/a/**") — это условие,
                        // которое проверяет URI входящего HTTP-запроса.
                        //
                        // Данный predicate возвращает TRUE,
                        // если путь запроса начинается с "/a/".
                        //
                        // Если predicate НЕ совпал:
                        // - данный маршрут НЕ рассматривается Gateway;
                        // - Gateway продолжает искать другой подходящий Route.
                        .path("/a/**")

                        //Удаляем префикс /a
                        .filters(f -> f.stripPrefix(1))

                        // URI задаёт backend-сервис,
                        // на который будет перенаправлён запрос,
                        // ЕСЛИ predicate path("/a/**") вернул TRUE.
                        //
                        // Таким образом:
                        // - запросы вида /a/anything → http://localhost:8081
                        .uri("http://localhost:8081")
                )

                // Определение второго маршрута с идентификатором "only-b".
                // Этот маршрут независим от предыдущего
                // и имеет собственное условие.
                .route("only-b", r -> r

                        // Predicate path("/b/**") проверяет,
                        // начинается ли путь запроса с "/b/".
                        //
                        // Если входящий запрос имеет путь:
                        // /b/test, /b/ping, /b/api/anything
                        // — данный predicate вернёт TRUE.
                        .path("/b/**")

                        //Удаляем префикс /b
                        .filters(f -> f.stripPrefix(1))

                        // URI второго backend-сервиса.
                        // Все запросы, удовлетворяющие predicate "/b/**",
                        // будут проксированы на http://localhost:8082.
                        .uri("http://localhost:8082")
                )

                // Метод build() завершает конфигурацию маршрутов.
                // После этого RouteLocator становится готовым
                // к использованию Gateway во время обработки запросов.
                .build();
    }
}

