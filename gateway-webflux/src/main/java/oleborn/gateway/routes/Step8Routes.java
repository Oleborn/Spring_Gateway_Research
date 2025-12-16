package oleborn.gateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile("step8")
public class Step8Routes {

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
    public RouteLocator route(RouteLocatorBuilder builder) {
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

