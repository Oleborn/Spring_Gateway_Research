package oleborn.gateway.b;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SpringBootApplication
@RestController
public class ServiceBApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
    }

    /**
     * Базовый ping для проверки маршрутизации.
     */
    @GetMapping("/ping")
    public String ping() throws InterruptedException {
        System.out.println("[SERVICE B] /ping called");

        Thread.sleep(500);

        return "pong from SERVICE B";
    }

    /**
     * Endpoint для демонстрации нестабильного поведения бека.
     *
     * С вероятностью ~30% кидает 500,
     * чтобы Gateway мог повторить запрос.
     */
    @GetMapping("/unstable")
    public ResponseEntity<String> unstable() throws InterruptedException {

        System.out.println("[SERVICE B] /unstable called");

        int delay = (int) (Math.random() * 3000); // 0–3s
        Thread.sleep(delay);

        if (Math.random() < 0.3) {
            System.out.println("[SERVICE B] /unstable FAILED");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("random failure");
        }

        return ResponseEntity.ok(
                "unstable response after " + delay + " ms"
        );
    }

    /**
     * Endpoint для демонстрации динамического routing
     * (например, через X-Target=B).
     */
    @GetMapping("/info")
    public String info() {
        System.out.println("[SERVICE B] /info called");
        return "response from SERVICE B";
    }

    /**
     * Endpoint, который ВСЕГДА падает.
     * Используется для демонстрации:
     * - fallback
     * - circuit breaker
     * - retry
     */
    @GetMapping("/fail")
    public ResponseEntity<String> fail() {

        System.out.println("[SERVICE B] /fail called — hard failure");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("SERVICE B is down");
    }

    /**
     * Endpoint для проверки headers,
     * пришедших от Gateway.
     */
    @GetMapping("/headers")
    public Map<String, String> headers(
            @RequestHeader Map<String, String> headers
    ) {

        System.out.println("[SERVICE B] /headers called");

        return headers;
    }
}

