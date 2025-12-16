package oleborn.gateway.a;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SpringBootApplication
@RestController
public class ServiceAApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAApplication.class, args);
    }

    /**
     * Базовый health-check / ping.
     * Используется для проверки маршрутизации.
     */
    @GetMapping("/ping")
    public String ping() throws InterruptedException {
        System.out.println("[SERVICE A] /ping called");

        Thread.sleep(100);

        return "pong from SERVICE A";
    }

    /**
     * Endpoint для демонстрации Retry.
     *
     * С вероятностью ~70% кидает 500,
     * чтобы Gateway мог повторить запрос.
     */
    @GetMapping("/unstable")
    public ResponseEntity<String> unstable() {

        System.out.println("[SERVICE A] /unstable called");

        if (Math.random() < 0.7) {
            System.out.println("[SERVICE A] simulated failure");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("temporary error from SERVICE A");
        }

        return ResponseEntity.ok("success after retry from SERVICE A");
    }

    /**
     * Endpoint для проверки headers,
     * которые добавляет Gateway.
     */
    @GetMapping("/headers")
    public Map<String, String> headers(
            @RequestHeader Map<String, String> headers
    ) {

        System.out.println("[SERVICE A] /headers called");

        return headers;
    }
}
