package oleborn.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Step5ShortCircuitFilter
 *
 * Данный класс представляет собой ПЯТЫЙ шаг изучения Spring Cloud Gateway
 * и демонстрирует механизм short-circuit в Gateway pipeline.
 *
 * Цель этого шага — показать, что Gateway может ОСОЗНАННО
 * остановить обработку запроса,
 * не вызывая backend и не передавая управление дальше по цепочке фильтров.
 * ВАЖНО! БЕЗ РОУТИНГА НЕ ВЫЗОВЕТСЯ ФИЛЬТР!
 *
 * Архитектурный смысл:
 * - Gateway pipeline — это цепочка фильтров, а не обязательная последовательность.
 * - Вызов chain.filter(exchange) — это ЯВНОЕ решение продолжить обработку.
 * - Отсутствие этого вызова означает остановку pipeline.
 *
 * Ключевая идея:
 * - short-circuit ≠ exception;
 * - short-circuit ≠ ошибка выполнения;
 * - short-circuit — это управляемое инфраструктурное решение.
 *
 * Этот фильтр используется исключительно в учебных целях
 * и активируется через Spring Profile "step5".
 */
@Component
@Profile("step5")
public class Step5ShortCircuitFilter implements GlobalFilter, Ordered {

    /**
     * Метод filter — основная точка входа GlobalFilter.
     *
     * Именно здесь принимается решение:
     * - продолжать pipeline;
     * - либо остановить его (short-circuit).
     *
     * @param exchange ServerWebExchange —
     *                 объект, содержащий состояние текущего HTTP-запроса.
     * @param chain GatewayFilterChain —
     *              цепочка фильтров Gateway,
     *              управляющая дальнейшим прохождением pipeline.
     * @return Mono<Void> — сигнал завершения обработки запроса.
     */
    @Override
    public Mono<Void> filter(
            // ServerWebExchange предоставляет доступ:
            // - к HTTP-запросу;
            // - к HTTP-ответу;
            // - к контексту выполнения запроса.
            ServerWebExchange exchange,

            // GatewayFilterChain представляет собой
            // цепочку следующих фильтров Gateway.
            // Вызов chain.filter(exchange) передаёт управление дальше.
            GatewayFilterChain chain
    ) {

        // Извлечение значения HTTP-заголовка "X-Deny"
        // из входящего запроса.
        //
        // Этот заголовок используется как управляющий флаг,
        // позволяющий явно инициировать short-circuit.
        String deny = exchange.getRequest()
                // Получение объекта ServerHttpRequest —
                // представления входящего HTTP-запроса.
                .getHeaders()
                // Получение первого значения заголовка "X-Deny".
                .getFirst("X-Deny");

        // Проверка значения управляющего заголовка.
        //
        // Если заголовок равен "true" (без учёта регистра),
        // Gateway принимает решение остановить pipeline.
        if ("true".equalsIgnoreCase(deny)) {

            // Логирование факта срабатывания short-circuit.
            //
            // Этот лог позволяет наглядно увидеть,
            // что запрос был остановлен на уровне Gateway.
            System.out.println("[STEP5] Short-circuit triggered");

            // Установка HTTP-статуса ответа.
            //
            // HttpStatus.FORBIDDEN (403) используется,
            // чтобы явно указать клиенту,
            // что запрос был отклонён инфраструктурным слоем.
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);

            // Завершение обработки ответа.
            //
            // ВАЖНО:
            // - chain.filter(exchange) НЕ вызывается;
            // - backend НЕ будет вызван;
            // - остальные GlobalFilter'ы могут НЕ выполниться,
            //   в зависимости от их order.
            return exchange.getResponse().setComplete();
        }

        // Логирование факта, что запрос допущен
        // к дальнейшей обработке.
        System.out.println("[STEP5] Passing request further");

        // Явное продолжение Gateway pipeline.
        //
        // Этот вызов передаёт управление
        // следующему фильтру в цепочке.
        return chain.filter(exchange);
    }

    /**
     * Определение порядка выполнения данного GlobalFilter.
     *
     * @return значение порядка выполнения фильтра.
     */
    @Override
    public int getOrder() {

        // Значение -5 означает, что данный фильтр
        // будет выполнен РАНЬШЕ, чем фильтры с order -1, 0 и выше.
        //
        // Это позволяет:
        // - остановить pipeline ДО логирования;
        // - показать влияние order на поведение Gateway.
        return -5;
    }
}

