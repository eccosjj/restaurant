package restaurant;

import java.util.concurrent.CountDownLatch;

public class Kitchen implements Runnable {
    private CookedOrder cookedOrder;
    private Delivery restaurant;
    private OrderManager orderManager;
    private CountDownLatch countKitchen;
    private CountDownLatch countCourier;

    public Kitchen(Delivery restaurant, OrderManager orderManager, CookedOrder order, CountDownLatch countKitchen,
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
            cookedOrder = this.orderManager.tryPutIntoShelf(cookedOrder, null);
            this.restaurant.notifyCourier(cookedOrder, countCourier);
        } finally {
            this.countKitchen.countDown();
        }
    }

}
