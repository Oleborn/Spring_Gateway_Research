package oleborn.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * GlobalLoggingFilter
 *
 * Данный класс представляет собой ГЛОБАЛЬНЫЙ фильтр Spring Cloud Gateway,
 * предназначенный для логирования жизненного цикла HTTP-запроса.
 *
 * Цель этого класса:
 * - показать, как работает GlobalFilter;
 * - продемонстрировать pre- и post-фазы Gateway pipeline;
 * - наглядно зафиксировать момент входа запроса и момент отправки ответа.
 *
 * Архитектурный смысл:
 * - GlobalFilter применяется ко ВСЕМ маршрутам Gateway;
 * - он не зависит от Route, Predicate или metadata;
 * - используется для инфраструктурных задач (логи, метрики, трассировка).
 *
 * Ключевая идея:
 * - GlobalFilter участвует в pipeline ДО и ПОСЛЕ backend-вызова;
 * - порядок выполнения фильтров критичен и управляется через Order.
 *
 * Данный фильтр используется исключительно для демонстрации
 * и не содержит бизнес-логики.
 */
@Component
// Это необходимо, чтобы Gateway автоматически
// обнаружил и подключил фильтр к pipeline.
@Order(-1)
// Аннотация @Order задаёт порядок выполнения фильтра
// в цепочке GlobalFilter'ов.
//
// Чем МЕНЬШЕ значение — тем РАНЬШЕ фильтр выполняется.
//
// Значение -1 означает, что данный фильтр
// будет выполнен ДО большинства стандартных фильтров,
// но ПОСЛЕ security-фильтров.
public class GlobalLoggingFilter implements GlobalFilter
        // Интерфейс GlobalFilter — это контракт Spring Cloud Gateway
        // для фильтров, применяемых ко ВСЕМ маршрутам.
        //
        // В отличие от GatewayFilter:
        // - GlobalFilter не привязан к конкретному Route;
        // - он используется для инфраструктурных задач.
        //
        // Интерфейс Ordered закомментирован,
        // потому что порядок задаётся через @Order.
        // Использовать одновременно @Order и Ordered — избыточно.

        //, Ordered
{

    /**
     * Метод filter — основная точка входа GlobalFilter.
     *
     * Этот метод вызывается Gateway
     * при обработке КАЖДОГО HTTP-запроса,
     * для которого был найден Route.
     *
     * @param exchange ServerWebExchange —
     *                 центральный объект WebFlux,
     *                 представляющий состояние одного запроса.
     * @param chain GatewayFilterChain —
     *              цепочка фильтров Gateway,
     *              управляющая дальнейшим прохождением pipeline.
     * @return Mono<Void> — реактивный сигнал завершения обработки запроса.
     */
    @Override
    public Mono<Void> filter(
            // ServerWebExchange содержит:
            // - HTTP-запрос (request);
            // - HTTP-ответ (response);
            // - attributes — контекст выполнения.
            ServerWebExchange exchange,

            // GatewayFilterChain представляет собой
            // цепочку следующих фильтров Gateway.
            // Вызов chain.filter(exchange) передаёт
            // управление дальше по pipeline.
            GatewayFilterChain chain
    ) {

        // Сохранение текущего времени в attributes exchange.
        //
        // exchange.getAttributes() — это Map,
        // используемая для передачи данных
        // между фильтрами В РАМКАХ ОДНОГО ЗАПРОСА.
        //
        // Здесь сохраняется момент входа запроса
        // для последующего вычисления времени обработки.
        exchange.getAttributes().put(
                "startTime",
                System.currentTimeMillis()
        );

        // Логирование факта поступления запроса в Gateway.
        //
        // exchange.getRequest() возвращает ServerHttpRequest,
        // представляющий входящий HTTP-запрос.
        //
        // getURI() возвращает полный URI запроса,
        // что удобно для диагностики и обучения.
        System.out.println("[GLOBAL] incoming " + exchange.getRequest().getURI());

        // Вызов chain.filter(exchange) передаёт управление
        // следующему фильтру в цепочке Gateway.
        //
        // ВАЖНО:
        // - если этот вызов НЕ сделать,
        //   pipeline будет остановлен (short-circuit);
        // - backend вызван не будет.
        return chain.filter(exchange)

                // Метод then(...) регистрирует действие,
                // которое будет выполнено ПОСЛЕ завершения
                // downstream-обработки.
                //
                // Это post-фаза Gateway pipeline.
                .then(
                        // Mono.fromRunnable(...) создаёт реактивный шаг,
                        // который будет выполнен без блокировок
                        // после завершения обработки запроса.
                        Mono.fromRunnable(
                                () -> {

                                    // Извлечение времени начала обработки
                                    // из attributes exchange.
                                    //
                                    // Тип Long используется,
                                    // так как ранее было сохранено
                                    // значение System.currentTimeMillis().
                                    Long start = exchange.getAttribute(
                                            "startTime"
                                    );

                                    // Вычисление длительности обработки запроса.
                                    //
                                    // Это разница между текущим временем
                                    // и моментом входа запроса в Gateway.
                                    long duration =
                                            System.currentTimeMillis() - start;

                                    // Логирование факта отправки ответа клиенту.
                                    System.out.println("[GLOBAL] response sent");

                                    // Логирование времени обработки запроса.
                                    //
                                    // Это значение включает:
                                    // - время маршрутизации;
                                    // - время выполнения backend;
                                    // - время работы фильтров.
                                    System.out.println("[GLOBAL] time = " + duration + " ms");
                                }
                        )
                );
    }

//
//    @Override
//    public int getOrder() {
//        return -1;
//    }
//
// Закомментированный метод getOrder() оставлен
// для демонстрации альтернативного способа задания порядка.
//
// Существует ДВА эквивалентных способа задать order:
// 1) через аннотацию @Order;
// 2) через реализацию интерфейса Ordered.
//
// Использовать оба одновременно НЕ НУЖНО.
// В данном классе выбран вариант с @Order.
}

