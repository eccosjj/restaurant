package restaurant.department;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import restaurant.Restaurant;
import restaurant.pojo.CookedOrder;
import restaurante.constants.OrderStatus;

public class Kitchen implements Runnable {
    private CookedOrder cookedOrder;
    private Restaurant restaurant;
    private OrderManager orderManager;
    private CountDownLatch countKitchen;
    private CountDownLatch countCourier;

    public Kitchen(Restaurant restaurant, OrderManager orderManager, CookedOrder order, CountDownLatch countKitchen,
            CountDownLatch countCourier) {
        this.restaurant = restaurant;
        this.orderManager = orderManager;
        this.cookedOrder = order;
        this.countKitchen = countKitchen;
        this.countCourier = countCourier;
    }

    public void run() {
        try {
            cookedOrder.setOrderStatus(OrderStatus.Cooked);
            cookedOrder.setOrderedTimestamp();
            this.orderManager.tryPutIntoShelf(cookedOrder, null);
            this.restaurant.notifyCourier(cookedOrder, countCourier);
        } finally {
            this.countKitchen.countDown();
        }
    }

    public CookedOrder call() {
        cookedOrder.setOrderStatus(OrderStatus.Cooked);
        cookedOrder.setOrderedTimestamp();
        this.orderManager.tryPutIntoShelf(cookedOrder, null);
        this.restaurant.notifyCourier(cookedOrder, countCourier);
        return cookedOrder;
    }

}
