package restaurant.department;

import org.apache.log4j.Logger;

import restaurant.Restaurant;
import restaurant.pojo.CookedOrder;
import restaurante.constants.OrderStatus;

public class Kitchen extends Thread {
    Logger log = Logger.getLogger(Kitchen.class);
    private CookedOrder cookedOrder;
    private Restaurant restaurant;
    private OrderManager orderManager;

    public Kitchen(Restaurant restaurant, OrderManager orderManager, CookedOrder order) {
        this.restaurant = restaurant;
        this.orderManager = orderManager;
        this.cookedOrder = order;

    }

    public void run() {
        log.trace("The kitchen receive the order:" + cookedOrder.getId());
        cookedOrder.setOrderStatus(OrderStatus.Cooked);
        cookedOrder.setOrderedTimestamp();
        log.trace("The kitchen cooked the order:" + cookedOrder.getId());
        this.orderManager.tryPutIntoShelf(cookedOrder, null);
        log.trace("The kitchen put the order:" + cookedOrder.getId() + " into "
                + cookedOrder.getShelfInfo().getAllowableTemperature() + "shelf and call the courier");
        this.restaurant.notifyCourier(cookedOrder);

    }

}
