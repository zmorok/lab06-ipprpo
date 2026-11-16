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
import java.util.Scanner;

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

        Customer customer1 = new Customer(1, "Иванов", "Иван", "Иванович", "123", 10000.0);
        Customer customer2 = new Customer(1, "Дмитриев", "Дмитрий", "Дмитриевич", "321", 400.0);
        Dish dish1 = new Dish(1, "Стейк", new String[]{}, 500.0);
        Dish dish2 = new Dish(1, "Креветка", new String[]{}, 50.0);

        runInitialOperations(orderService, customer1, customer2, dish1, dish2);

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            System.out.print("\033[H\033[2J");
            System.out.flush();

            System.out.println();
            System.out.println("Выберите действие:");
            System.out.println("1 - Успешный заказ (Стейк + Креветка)");
            System.out.println("2 - Ошибка (недостаточно средств)");
            System.out.println("3 - Ошибка (пустой список блюд)");
            System.out.println("4 - Очистить консоль");
            System.out.println("5 - Показать значения метрик");
            System.out.println("0 - Выход");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    try {
                        orderService.createOrder(customer1, List.of(dish1, dish2));
                    } catch (Exception e) {
                        System.out.println("Ошибка при оформлении заказа: " + e.getMessage());
                    }
                    break;
                case "2":
                    try {
                        orderService.createOrder(customer2, List.of(dish1));
                    } catch (Exception e) {
                        System.out.println("Ошибка при оформлении заказа: " + e.getMessage());
                    }
                    break;
                case "3":
                    try {
                        orderService.createOrder(customer1, List.of());
                    } catch (Exception e) {
                        System.out.println("Ошибка при оформлении заказа: " + e.getMessage());
                    }
                    break;
                case "4":
                    clearConsole();
                    break;
                case "5":
                    printRawMetrics(prometheusRegistry);
                    break;
                case "0":
                    System.out.println("Остановка сервера...");
                    server.stop(0);
                    return;
                default:
                    System.out.println("Неизвестная команда, введите 1, 2, 3 или 0.");
                    break;
            }
        }
    }

    private static void runInitialOperations(
            OrderService orderService,
            Customer customer1, Customer customer2,
            Dish dish1, Dish dish2) {
        try {
            orderService.createOrder(customer1, List.of(dish1, dish2));
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
    }

    private static void printRawMetrics(PrometheusMeterRegistry registry) {
        System.out.println("=== Prometheus Scrape ===");
        System.out.println(registry.scrape());
        System.out.println("=========================");
    }

    private static void clearConsole() {
        for (int i = 0; i < 40; i++) {
            System.out.println();
        }
    }
}
