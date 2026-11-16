import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import model.Customer;
import model.Dish;
import repository.OrderRepository;
import service.OrderService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/metrics", exchange -> {
            String prometheusScrape = prometheusRegistry.scrape();
            byte[] content = prometheusScrape.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        });
        server.start();

        System.out.println("HTTP-сервер запущен.");
        System.out.println("Метрики: http://localhost:8081/metrics");
        System.out.println();

        OrderRepository repository = new OrderRepository();
        OrderService orderService = new OrderService(repository, 100.0, prometheusRegistry);

        Customer customer1 = new Customer(1, "Иванов", "Иван", "Иванович", "123", 1000.0);
        Customer customer2 = new Customer(1, "Дмитриев", "Дмитрий", "Дмитриевич", "321", 400.0);
        Dish dish1 = new Dish(1, "Стейк", new String[]{}, 500.0);
        Dish dish2 = new Dish(1, "Креветка", new String[]{}, 50.0);

        try {
            orderService.createOrder(customer1, List.of(dish1));
        } catch (Exception e) {
            System.out.println("Операция 1 завершилась с ошибкой: " + e.getMessage());
        }

        try {
            orderService.createOrder(customer2, List.of(dish1));
        } catch (Exception e) {
            System.out.println("Операция 2 завершилась с ошибкой: " + e.getMessage());
        }

        try {
            orderService.createOrder(customer1, List.of());
        } catch (Exception e) {
            System.out.println("Операция 3 завершилась с ошибкой: " + e.getMessage());
        }

        System.out.println("Нажмите Enter для остановки приложения...");
        System.in.read();
        server.stop(0);
    }
}
