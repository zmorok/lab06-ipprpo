package service;

import model.Customer;
import model.Dish;
import model.Order;
import repository.IRepository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final IRepository<Order> repository;
    private final double minOrderPrice;

    private final Counter totalOperationsCounter;
    private final Counter successOperationsCounter;
    private final Counter errorOperationsCounter;

    public OrderService(IRepository<Order> repository, double minOrderPrice) {
        this(repository, minOrderPrice, new SimpleMeterRegistry());
    }

    public OrderService(IRepository<Order> repository, double minOrderPrice, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.minOrderPrice = minOrderPrice;

        this.totalOperationsCounter = Counter.builder("application_operations_total")
                .description("Общее количество попыток оформления заказа")
                .register(meterRegistry);

        this.successOperationsCounter = Counter.builder("application_operations_success_total")
                .description("Количество успешно оформленых заказов")
                .register(meterRegistry);

        this.errorOperationsCounter = Counter.builder("application_operations_error_total")
                .description("Количество ошибок при оформлении заказов")
                .register(meterRegistry);
    }

    public Order createOrder(Customer customer, List<Dish> dishes) {
        totalOperationsCounter.increment();

        if (customer == null || dishes == null || dishes.isEmpty()) {
            errorOperationsCounter.increment();
            throw new IllegalArgumentException("Данные заказа заполнены неверно");
        }

        double orderPrice = 0;
        for (Dish dish : dishes) {
            orderPrice += dish.price;
        }

        if (orderPrice < minOrderPrice) {
            errorOperationsCounter.increment();
            logger.warn("Сумма заказа ({}) меньше, чем минимально допустимая ({}).", orderPrice, minOrderPrice);
            throw new IllegalArgumentException(
                    String.format(
                            "Сумма заказа (%.2f) меньше допустимой минимальной суммы (%.2f)",
                            orderPrice,
                            minOrderPrice)
                    );
        }

        if (customer.cash < orderPrice) {
            errorOperationsCounter.increment();
            logger.warn("Недостаточно средств у клиента {}. Баланс: {}, сумма заказа: {}", customer.id, customer.cash, orderPrice);
            throw new IllegalArgumentException(
                    String.format(
                            "Не хватает средств (%.2f) для оплаты заказа на сумму %.2f",
                            customer.cash,
                            orderPrice)
            );
        }

        customer.cash -= orderPrice;
        int id = repository.getMaxId() + 1;
        Order order = new Order(id, customer.id, dishes);

        try {
            repository.add(order);
        } catch (RuntimeException e) {
            errorOperationsCounter.increment();
            logger.error("Ошибка сохранения заказа клиента {}", customer.id, e);
            throw e;
        }

        successOperationsCounter.increment();
        logger.info("Заказ №{} успешно оформлен. Клиент: {}, сумма: {}", order.id, customer.id, order.price);

        return order;
    }

    public String testMethod() {
        StringBuilder sb = new StringBuilder("String var2 =");

        for (int i = 1; i < 5; i++) {
            sb.append(" ").append(i);
        }

        return sb.toString();
    }
}
