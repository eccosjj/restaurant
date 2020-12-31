package restaurant.department;

import restaurant.Restaurant;

public class Monitor extends Thread {
    private OrderManager orderManager;
    private int workInterval;

    public Monitor(OrderManager orderManager, int workInterval) {
        super();
        this.orderManager = orderManager;
        this.workInterval = workInterval;
    }

    public void run() {
        while (Restaurant.Restaurant_Is_Openning) {
            try {
                this.orderManager.displayAllOrderStatus();
                Thread.sleep(this.workInterval * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
