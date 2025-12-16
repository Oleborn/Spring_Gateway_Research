package oleborn.gateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("step5")
public class Step5Routes {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("dummy", r -> r
                        .path("/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8081")
                )
                .build();
    }
}
