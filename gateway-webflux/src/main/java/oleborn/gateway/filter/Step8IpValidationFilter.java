package oleborn.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Step8IpValidationFilter
 *
 * Данный класс представляет собой ДЕВЯТЫЙ шаг изучения Spring Cloud Gateway
 * и демонстрирует инфраструктурную IP-валидацию на уровне Gateway.
 *
 * Цель этого шага — показать, что Gateway может принимать решения
 * о ДОПУСТИМОСТИ запроса на основе сетевых атрибутов,
 * ещё ДО маршрутизации и вызова backend-сервисов.
 *
 * Архитектурный смысл:
 * - IP-валидация относится к edge-слою, а не к бизнес-логике.
 * - Проверка IP выполняется централизованно,
 *   чтобы backend-сервисы не дублировали эту логику.
 * - Gateway управляет ДОСТУПОМ, а не смыслом запроса.
 *
 * Ключевая идея:
 * - IP — это характеристика соединения, а не пользователя.
 * - Gateway — правильное место для таких проверок.
 * - Short-circuit используется для осознанного отказа.
 *
 * Фильтр активируется через Spring Profile "step9"
 * и используется исключительно в учебных целях.
 */
@Component
@Profile("step8")
@Order(-20)
// Аннотация @Order задаёт порядок выполнения фильтра.
//
// Значение -20 означает, что данный фильтр
// выполняется ОДНИМ ИЗ ПЕРВЫХ в Gateway pipeline.
//
// Это критично, потому что:
// - IP-валидация должна происходить ДО логирования;
// - ДО динамической маршрутизации;
// - ДО backend-вызова.
public class Step8IpValidationFilter implements GlobalFilter {

    // Статический набор разрешённых IP-адресов.
    //
    // В учебном примере используется фиксированный allowlist.
    // В реальных системах:
    // - список может приходить из конфигурации;
    // - может храниться в Redis или базе;
    // - может быть сегментирован по Route metadata.
    private static final Set<String> ALLOWED_IPS =
            Set.of(
                    // Loopback-адрес, используемый для локального тестирования.
                    "127.0.0.1",
                    // Пример внутреннего IP-адреса.
                    "192.168.1.10",
                    // Адрес из postman
                    "0:0:0:0:0:0:0:1"
            );

    /**
     * Метод filter — основная точка входа GlobalFilter.
     *
     * В данном методе выполняется:
     * - определение IP клиента;
     * - проверка IP по allowlist;
     * - принятие решения о продолжении pipeline
     *   или его остановке (short-circuit).
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
            // ServerWebExchange предоставляет доступ
            // к HTTP-запросу, ответу и контексту выполнения.
            ServerWebExchange exchange,

            // GatewayFilterChain управляет передачей управления
            // следующим фильтрам Gateway.
            GatewayFilterChain chain
    ) {

        // Определение IP-адреса клиента.
        //
        // Вынесено в отдельный метод,
        // так как логика определения IP
        // не является тривиальной.
        String clientIp = resolveClientIp(exchange);

        // Проверка, содержится ли IP клиента
        // в списке разрешённых адресов.
        if (!ALLOWED_IPS.contains(clientIp)) {

            // Логирование факта блокировки запроса.
            //
            // Это учебный лог,
            // позволяющий явно увидеть причину отказа.
            System.out.println("[IP] blocked request from " + clientIp);

            // Установка HTTP-статуса FORBIDDEN (403).
            //
            // Это означает, что:
            // - запрос понят;
            // - но доступ запрещён инфраструктурным слоем.
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);

            // Завершение обработки ответа.
            //
            // ВАЖНО:
            // - chain.filter(exchange) НЕ вызывается;
            // - backend НЕ будет вызван;
            // - pipeline останавливается осознанно.
            return exchange.getResponse().setComplete();
        }

        // Логирование факта,
        // что запрос прошёл IP-валидацию.
        System.out.println("[IP] allowed request from " + clientIp);

        // Продолжение Gateway pipeline.
        //
        // Управление передаётся следующему фильтру
        // или backend-вызову.
        return chain.filter(exchange);
    }

    /**
     * Определение IP-адреса клиента.
     *
     * Метод реализует ПРАВИЛЬНЫЙ порядок
     * определения реального IP клиента
     * в условиях работы за proxy / load balancer.
     *
     * @param exchange ServerWebExchange —
     *                 объект запроса,
     *                 содержащий HTTP-заголовки
     *                 и сетевую информацию.
     * @return строковое представление IP-адреса клиента
     *         либо "unknown", если IP определить невозможно.
     */
    private String resolveClientIp(ServerWebExchange exchange) {

        // Получение HTTP-заголовков входящего запроса.
        HttpHeaders headers = exchange.getRequest().getHeaders();

        // Попытка получить заголовок X-Forwarded-For.
        //
        // Это стандартный заголовок,
        // добавляемый proxy и load balancer'ами.
        String xff = headers.getFirst("X-Forwarded-For");

        // Проверка, что заголовок присутствует и не пустой.
        if (xff != null && !xff.isBlank()) {

            // X-Forwarded-For может содержать цепочку IP:
            // client, proxy1, proxy2
            //
            // По стандарту первый IP — это исходный клиент.
            return xff.split(",")[0].trim();
        }

        // Попытка получить заголовок X-Real-IP.
        //
        // Это неформальный, но широко используемый заголовок,
        // часто добавляемый nginx.
        String realIp = headers.getFirst("X-Real-IP");

        // Проверка наличия и непустоты X-Real-IP.
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        // Использование RemoteAddress как последнего источника.
        //
        // ВАЖНО:
        // - это IP последнего сетевого хопа;
        // - чаще всего это IP proxy или ingress,
        //   а НЕ реального клиента.
        if (exchange.getRequest().getRemoteAddress() != null) {

            // Извлечение IP-адреса из RemoteAddress.
            return exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
        }

        // Фолбэк-значение, если IP определить невозможно.
        return "unknown";
    }
}

