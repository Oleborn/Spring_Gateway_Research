package oleborn.gateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Step3RoutesWithFilter
 *
 * Данный класс представляет собой ТРЕТИЙ шаг изучения Spring Cloud Gateway.
 *
 * Цель этого шага — показать, что после выбора маршрута (Route + Predicate)
 * Gateway может МОДИФИЦИРОВАТЬ запрос перед отправкой его в backend.
 *
 * Архитектурный смысл:
 * - GatewayFilter применяется НА УРОВНЕ КОНКРЕТНОГО ROUTE.
 * - GatewayFilter участвует в pipeline ДО и ПОСЛЕ backend-вызова.
 * - Фильтр позволяет изменять request и response,
 *   не вмешиваясь в бизнес-логику backend.
 *
 * Что демонстрирует этот класс:
 * - использование route-level filters;
 * - модификацию HTTP-запроса;
 * - отличие GatewayFilter от GlobalFilter.
 *
 * Ключевая идея:
 * - Route определяет КУДА идёт запрос;
 * - Predicate определяет КОГДА маршрут применяется;
 * - GatewayFilter определяет ЧТО можно изменить в запросе/ответе.
 *
 * Класс активируется через Spring Profile "step3"
 * и используется исключительно для учебной демонстрации.
 */
@Configuration
@Profile("step3")
public class Step3RoutesWithFilter {

    /**
     * Создание RouteLocator с route-level GatewayFilter.
     *
     * RouteLocator — основной интерфейс Spring Cloud Gateway,
     * предоставляющий набор Route для обработки запросов.
     *
     * @param builder RouteLocatorBuilder —
     *                DSL-строитель маршрутов,
     *                позволяющий декларативно описывать
     *                Route, Predicate и Filters.
     * @return RouteLocator с маршрутом,
     *         содержащим GatewayFilter.
     */
    @Bean
    public RouteLocator step3Routes(RouteLocatorBuilder builder) {

        // Инициализация DSL для построения маршрутов Gateway.
        return builder.routes()

                // Определение маршрута с идентификатором "only-a".
                // Идентификатор используется для логирования,
                // мониторинга и отладки.
                .route("only-a", r -> r

                        // Predicate path("/a/**") проверяет,
                        // начинается ли путь входящего запроса с "/a/".
                        // Только такие запросы попадут под этот маршрут.
                        .path("/a/**")

                        // Блок filters(...) определяет GatewayFilter'ы,
                        // которые будут применены ТОЛЬКО к этому маршруту.
                        //
                        // GatewayFilter — это фильтр,
                        // работающий в рамках одного Route,
                        // в отличие от GlobalFilter,
                        // который применяется ко всем маршрутам.
                        .filters(f -> f

                                // addRequestHeader(...) добавляет HTTP-заголовок
                                // в запрос ПЕРЕД отправкой его в backend.
                                //
                                // В данном случае Gateway помечает запрос,
                                // что он прошёл через edge-слой.
                                //
                                // Backend может:
                                // - использовать этот заголовок для логирования;
                                // - отличать внутренние вызовы от внешних;
                                // - не использовать его вовсе.
                                .addRequestHeader("X-From-Gateway", "false")

                                // stripPrefix(1) удаляет первый сегмент пути URI.
                                //
                                // Пример:
                                //   Входящий запрос: /a/ping
                                //   После stripPrefix(1): /ping
                                //
                                // Это типичный приём,
                                // когда Gateway использует префиксы
                                // для маршрутизации,
                                // а backend ожидает "чистый" путь.
                                .stripPrefix(1)
                        )

                        // URI указывает backend-сервис,
                        // на который будет направлен модифицированный запрос.
                        //
                        // Запрос уже прошёл:
                        // - выбор маршрута (predicate);
                        // - применение GatewayFilter'ов.
                        .uri("http://localhost:8081")
                )
                .build();
    }
}

