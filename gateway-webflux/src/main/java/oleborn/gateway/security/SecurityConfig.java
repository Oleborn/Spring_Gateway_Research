package oleborn.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * SecurityConfig
 *
 * Данный класс представляет собой КОРНЕВУЮ конфигурацию
 * Spring Security для реактивного стека WebFlux
 * в приложении с Spring Cloud Gateway.
 *
 * Цель этого класса — продемонстрировать:
 * - как правильно подключать Security к Gateway (WebFlux);
 * - где именно в pipeline располагается аутентификация;
 * - чем Authentication отличается от Authorization;
 * - почему Security НЕ должна реализовываться через WebFilter.
 *
 * Архитектурный смысл:
 * - Security работает ДО Gateway;
 * - Security решает, МОЖНО ЛИ обрабатывать запрос;
 * - Gateway решает, КУДА проксировать разрешённый запрос.
 *
 * Ключевая идея:
 * - AuthenticationWebFilter отвечает за установление личности;
 * - authorizeExchange отвечает за правила доступа;
 * - Gateway не должен знать ничего про JWT.
 *
 * Данная конфигурация является УЧЕБНОЙ
 * и демонстрирует минимально корректную модель JWT-аутентификации.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Создание SecurityWebFilterChain —
     * центрального элемента Spring Security WebFlux.
     *
     * SecurityWebFilterChain определяет:
     * - какие security-фильтры применяются;
     * - в каком порядке они выполняются;
     * - какие правила авторизации действуют для запросов.
     *
     * ВАЖНО:
     * - это НЕ Servlet FilterChain;
     * - это реактивная цепочка WebFilter'ов;
     * - Gateway начнёт работу ТОЛЬКО ПОСЛЕ успешного прохождения Security.
     *
     * @param http ServerHttpSecurity —
     *             DSL-объект для конфигурации WebFlux Security.
     * @return SecurityWebFilterChain — готовая security-цепочка.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        /**
         * AuthenticationWebFilter —
         * стандартный security-фильтр Spring Security,
         * отвечающий ИСКЛЮЧИТЕЛЬНО за аутентификацию.
         *
         * Он:
         * - извлекает учетные данные (через converter);
         * - передаёт их в AuthenticationManager;
         * - при успехе помещает Authentication в SecurityContext.
         *
         * ВАЖНО:
         * - это НЕ WebFilter общего назначения;
         * - его нельзя реализовывать вручную для JWT.
         */
        AuthenticationWebFilter jwtAuthFilter = new AuthenticationWebFilter(authenticationManager());

        /**
         * requiresAuthenticationMatcher определяет,
         * ДЛЯ КАКИХ ЗАПРОСОВ будет запускаться аутентификация.
         *
         * В данном учебном примере используется anyExchange():
         * - аутентификация ПЫТАЕТСЯ выполниться для всех запросов;
         * - если токена нет → аутентификация просто не произойдёт;
         * - решение о доступе будет принято на этапе Authorization.
         */
        jwtAuthFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.anyExchange());

        /**
         * ServerAuthenticationConverter —
         * компонент, отвечающий за извлечение учетных данных
         * из входящего HTTP-запроса.
         *
         * В данном случае:
         * - JWT извлекается из заголовка Authorization;
         * - если заголовка нет — возвращается Mono.empty();
         * - Mono.empty() означает: "пользователь анонимен".
         *
         * ВАЖНО:
         * - здесь НЕЛЬЗЯ возвращать Mono<Void>;
         * - здесь НЕЛЬЗЯ выбрасывать 401;
         * - converter ТОЛЬКО извлекает credentials.
         */
        jwtAuthFilter.setServerAuthenticationConverter(exchange -> {
            String auth = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (auth == null || !auth.startsWith("Bearer ")) {
                return Mono.empty();
            }

            String token = auth.substring(7);

            return Mono.just(
                    new UsernamePasswordAuthenticationToken(null, token)
            );
        });

        return http
                /**
                 * CSRF-защита отключена.
                 *
                 * Причина:
                 * - Gateway — это API;
                 * - cookies и browser-sessions не используются;
                 * - CSRF здесь не имеет смысла.
                 */
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                /**
                 * Конфигурация АВТОРИЗАЦИИ.
                 *
                 * На этом этапе Security решает:
                 * - разрешён ли запрос;
                 * - требуется ли Authentication;
                 * - или доступ запрещён.
                 *
                 * ВАЖНО:
                 * - authorizeExchange НЕ аутентифицирует;
                 * - он работает ТОЛЬКО с SecurityContext.
                 */
                .authorizeExchange(ex -> ex
                        .pathMatchers("/public/**").permitAll()
                        .anyExchange().authenticated()
                )

                /**
                 * Явное добавление AuthenticationWebFilter
                 * в security-pipeline.
                 *
                 * SecurityWebFiltersOrder.AUTHENTICATION означает:
                 * - фильтр выполнится ДО Authorization;
                 * - Authentication будет доступна при проверке прав.
                 */
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                /**
                 * Финализация конфигурации.
                 */
                .build();
    }

    /**
     * ReactiveAuthenticationManager —
     * компонент, отвечающий за ПРОВЕРКУ учетных данных.
     *
     * В данном учебном примере:
     * - credentials = JWT-токен (строка);
     * - токен сравнивается с фиксированным значением;
     * - при успехе создаётся Authentication.
     *
     * В реальной системе здесь выполняется:
     * - проверка подписи JWT;
     * - проверка срока действия;
     * - извлечение claims;
     * - построение authorities.
     *
     * @return ReactiveAuthenticationManager — менеджер аутентификации.
     */
    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return authentication -> {

            String token = authentication.getCredentials().toString();

            if ("valid-token".equals(token)) {
                return Mono.just(
                        new UsernamePasswordAuthenticationToken(
                                "user",
                                token,
                                List.of()
                        )
                );
            }

            return Mono.error(
                    new BadCredentialsException("Invalid token")
            );
        };
    }
}
