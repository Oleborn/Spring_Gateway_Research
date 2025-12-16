package oleborn.gateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Step7RoutesWithMetadata
 *
 * Данный класс представляет собой СЕДЬМОЙ шаг изучения Spring Cloud Gateway
 * и демонстрирует механизм Route metadata.
 *
 * Цель этого шага — показать, как Route может передавать
 * КОНФИГУРАЦИОННУЮ ИНФОРМАЦИЮ в фильтры,
 * не используя headers запроса и не загрязняя бизнес-логику.
 *
 * Архитектурный смысл:
 * - Metadata — это данные, привязанные к Route, а не к запросу.
 * - Metadata задаётся на этапе конфигурации Gateway.
 * - Metadata используется фильтрами для принятия инфраструктурных решений.
 *
 * Ключевая идея:
 * - Headers приходят от клиента и небезопасны.
 * - Exchange attributes живут только в рамках одного запроса.
 * - Route metadata описывает ПОЛИТИКУ маршрута.
 *
 * Этот класс активируется через Spring Profile "step7"
 * и используется исключительно в учебных целях.
 */
@Configuration
@Profile("step7")
public class Step7RoutesWithMetadata {

    /**
     * Создание RouteLocator с маршрутами,
     * содержащими metadata.
     *
     * RouteLocator — центральный интерфейс Spring Cloud Gateway,
     * предоставляющий набор Route для обработки входящих запросов.
     *
     * @param builder RouteLocatorBuilder —
     *                DSL-строитель маршрутов,
     *                позволяющий декларативно описывать
     *                Route, Predicate, Filters и Metadata.
     * @return RouteLocator с маршрутами,
     *         содержащими metadata.
     */
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // Определение первого маршрута с идентификатором "only-a".
                // Идентификатор маршрута используется для логирования,
                // мониторинга и отладки.
                .route("only-a", r -> r

                        // Predicate path("/a/**") проверяет,
                        // начинается ли путь входящего запроса с "/a/".
                        //
                        // Только запросы, удовлетворяющие этому условию,
                        // попадут под данный маршрут.
                        .path("/a/**")
                        .filters(f -> f.stripPrefix(1))

                        // Metadata с ключом "service".
                        //
                        // Это произвольная пара ключ-значение,
                        // описывающая ЛОГИЧЕСКИЙ КОНТЕКСТ маршрута.
                        //
                        // В данном случае metadata сообщает фильтрам,
                        // что данный маршрут относится к сервису "A".
                        .metadata("service", "A")

                        // Metadata с ключом "log".
                        //
                        // Данный флаг может использоваться фильтрами
                        // для принятия решений, например:
                        // - логировать запросы;
                        // - отключать логирование;
                        // - изменять уровень детализации логов.
                        //
                        // ВАЖНО:
                        // metadata не приходит от клиента
                        // и не может быть подделана.
                        .metadata("log", true)

                        // URI указывает backend-сервис,
                        // на который будут направлены запросы,
                        // удовлетворяющие predicate "/a/**".
                        .uri("http://localhost:8081")
                )

                // Определение второго маршрута с идентификатором "only-b".
                // Этот маршрут независим от предыдущего
                // и имеет собственный набор metadata.
                .route("only-b", r -> r

                        // Predicate path("/b/**") проверяет,
                        // начинается ли путь входящего запроса с "/b/".
                        //
                        // Только такие запросы будут сопоставлены
                        // с данным маршрутом.
                        .path("/b/**")
                        .filters(f -> f.stripPrefix(1))

                        // Metadata "service" со значением "B".
                        //
                        // Фильтры могут использовать это значение
                        // для:
                        // - маршрутизации;
                        // - логирования;
                        // - метрик;
                        // - принятия инфраструктурных решений.
                        .metadata("service", "B")

                        // Metadata "log" со значением false.
                        //
                        // Это означает, что для данного маршрута
                        // логирование может быть отключено
                        // или обработано иначе.
                        //
                        // ВАЖНО:
                        // логика обработки metadata
                        // находится НЕ ЗДЕСЬ,
                        // а в соответствующих фильтрах.
                        .metadata("log", false)

                        // URI backend-сервиса,
                        // на который будут направлены запросы
                        // для маршрута "only-b".
                        .uri("http://localhost:8082")
                )

                // Завершение конфигурации маршрутов.
                // После вызова build() RouteLocator
                // становится доступным Gateway
                // для обработки входящих запросов.
                .build();
    }
}
