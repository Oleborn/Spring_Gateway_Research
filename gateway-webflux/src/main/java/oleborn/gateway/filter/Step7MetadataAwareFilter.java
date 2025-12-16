package oleborn.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Step7MetadataAwareFilter
 *
 * Данный класс представляет собой ГЛОБАЛЬНЫЙ фильтр Spring Cloud Gateway,
 * который демонстрирует использование Route metadata
 * для управления поведением инфраструктурных фильтров.
 *
 * Цель этого шага — показать, как фильтр может принимать решения
 * НЕ на основе данных запроса (headers, path),
 * а на основе конфигурации маршрута (Route metadata).
 *
 * Архитектурный смысл:
 * - Route metadata описывает ПОЛИТИКУ маршрута;
 * - GlobalFilter читает эту политику и адаптирует своё поведение;
 * - логика фильтра не привязана к path или URI.
 *
 * Ключевая идея:
 * - Route определяет КУДА идёт запрос;
 * - Metadata определяет КАК с этим запросом работать;
 * - Фильтр остаётся универсальным и переиспользуемым.
 *
 * Фильтр активируется через Spring Profile "step7"
 * и используется исключительно в учебных целях.
 */
@Component
@Profile("step7")
public class Step7MetadataAwareFilter implements GlobalFilter, Ordered {

    /**
     * Метод filter — основная точка входа GlobalFilter.
     *
     * В этом методе фильтр:
     * - получает текущий Route;
     * - извлекает metadata маршрута;
     * - принимает инфраструктурное решение
     *   на основе конфигурации Route.
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
            // к запросу, ответу и контексту выполнения.
            ServerWebExchange exchange,

            // GatewayFilterChain управляет передачей управления
            // следующим фильтрам Gateway.
            GatewayFilterChain chain
    ) {

        // Извлечение текущего Route из attributes ServerWebExchange.
        //
        // GATEWAY_ROUTE_ATTR — служебный ключ Spring Cloud Gateway,
        // под которым хранится маршрут,
        // выбранный на этапе Route + Predicate matching.
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        // Проверка на null необходима,
        // так как фильтр может быть вызван
        // в редких сценариях ДО выбора маршрута.
        if (route != null) {

            // Извлечение metadata с ключом "service".
            //
            // Это значение задаётся в конфигурации Route
            // и описывает логический сервис,
            // к которому относится маршрут.
            Object service = route.getMetadata().get("service");

            // Извлечение metadata с ключом "log".
            //
            // Этот флаг используется как управляющий параметр,
            // определяющий, должно ли выполняться логирование
            // для данного маршрута.
            Object log = route.getMetadata().get("log");

            // Логирование полученной metadata.
            //
            // Это учебный лог,
            // позволяющий визуально увидеть,
            // какие metadata были привязаны к маршруту.
            System.out.println(
                    "[META] route=" + route.getId()
                    + ", service=" + service
                    + ", log=" + log
            );

            // Проверка значения metadata "log".
            //
            // Если значение равно Boolean.FALSE,
            // фильтр принимает решение
            // НЕ выполнять дополнительное логирование.
            if (Boolean.FALSE.equals(log)) {

                // Передача управления следующему фильтру
                // без выполнения логики,
                // связанной с логированием.
                //
                // Это демонстрирует,
                // как metadata управляет поведением фильтра.
                return chain.filter(exchange);
            }
        }

        // Логирование факта,
        // что для данного маршрута логирование разрешено.
        //
        // Здесь используется URI запроса
        // исключительно для наглядности.
        System.out.println("[META] logging enabled for " + exchange.getRequest().getURI());

        // Продолжение Gateway pipeline.
        //
        // Управление передаётся следующему фильтру
        // или backend-вызову.
        return chain.filter(exchange);
    }

    /**
     * Определение порядка выполнения данного GlobalFilter.
     *
     * @return значение порядка выполнения фильтра.
     */
    @Override
    public int getOrder() {

        // Значение -2 означает,
        // что фильтр выполняется:
        // - ПОСЛЕ фильтров с order -5;
        // - ДО фильтров с order -1 и выше.
        //
        // Это позволяет:
        // - сначала принять решение на основе metadata;
        // - затем выполнить логирование или другие действия.
        return -2;
    }
}

