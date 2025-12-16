package oleborn.gateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Step1Routes
 * Архитектурный смысл:
 * - Route — это декларативное правило маршрутизации.
 * - На этом этапе Gateway выступает как простой HTTP-прокси.
 * - Любой входящий запрос будет перенаправлен на указанный backend.
 * Что этот класс демонстрирует:
 * - Route существует сам по себе, без условий.
 * - Gateway не принимает решений, а лишь следует конфигурации.
 * - Это отправная точка для понимания всего pipeline.
 * Что этот класс ОСОЗНАННО не делает:
 * - не использует predicates (нет условий);
 * - не использует filters (нет модификаций);
 * - не содержит бизнес-логики;
 * - не взаимодействует с Security.
 *  активируется через Spring Profile "step1".
 */
@Configuration
// Аннотация @Configuration указывает Spring, что данный класс содержит
// определения бинов (Bean), которые должны быть добавлены в ApplicationContext.
// В контексте Gateway это означает, что здесь будет объявлена конфигурация маршрутов.
@Profile("step1")
// Аннотация @Profile ограничивает активацию данного класса.
// Этот класс будет загружен ТОЛЬКО если активен профиль "step1".
// Это позволяет поэтапно включать разные конфигурации Gateway.
public class Step1Routes {

    /**
     * Создание бина RouteLocator.
     *
     * RouteLocator — это основной интерфейс Spring Cloud Gateway,
     * который отвечает за предоставление набора Route.
     *
     * Gateway при старте приложения запрашивает все бины RouteLocator
     * и агрегирует их в единый набор маршрутов.
     *
     * @param builder RouteLocatorBuilder — вспомогательный класс,
     *                предоставляющий Java DSL для декларативного
     *                описания маршрутов.
     * @return RouteLocator — объект, содержащий один или несколько Route.
     */
    @Bean
    // Аннотация @Bean сообщает Spring, что результат выполнения метода
    // должен быть зарегистрирован в ApplicationContext как бин.
    // В данном случае это бин типа RouteLocator.
    public RouteLocator route(RouteLocatorBuilder builder) {

        // RouteLocatorBuilder — это DSL-строитель маршрутов.
        // Метод routes() инициализирует процесс построения набора Route.
        return builder.routes()

                // Метод route(...) добавляет новое правило маршрутизации.
                // Первый параметр — это уникальный идентификатор маршрута.
                // Идентификатор используется:
                // - в логах;
                // - в метаданных;
                // - при отладке и мониторинге.
                .route("route-to-a", r -> r

                        // Predicate path("/a/**") проверяет,
                        // начинается ли путь запроса с "/a/".
                        //
                        // Если входящий запрос имеет путь:
                        // /a/test, /a/ping, /a/api/anything
                        // — данный predicate вернёт TRUE.
                        .path("/a/**")
                        //Удаляем префикс /b
                        .filters(f -> f.stripPrefix(1))

                        // Метод uri(...) задаёт целевой backend,
                        // на который Gateway будет проксировать запрос.
                        //
                        // ВАЖНО:
                        // - здесь НЕТ predicates, поэтому маршрут подходит ЛЮБОМУ запросу;
                        // - Gateway не анализирует path, headers или method;
                        // - любой запрос, дошедший до Gateway pipeline,
                        //   будет отправлен на http://localhost:8081.
                        //
                        // URI указывает на backend-сервис (Service A),
                        // который будет фактически обрабатывать запрос.
                        .uri("http://localhost:8081")
                )

                // Метод build() завершает построение RouteLocator.
                // После вызова build() все описанные маршруты
                // становятся доступными Gateway для обработки запросов.
                .build();
    }
}

