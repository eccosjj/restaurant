package restaurant.department;

import org.apache.log4j.Logger;

import restaurant.Restaurant;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;
import restaurante.constants.OrderStatus;

public class Kitchen extends Thread {
    static Logger log = Logger.getLogger(Kitchen.class);
    private CookedOrder cookedOrder;
    private Restaurant restaurant;
    private OrderManager orderManager;

    public Kitchen(Restaurant restaurant, OrderManager orderManager, CookedOrder order) {
        this.restaurant = restaurant;
        this.cookedOrder = order;
        this.orderManager = orderManager;

    }

    public void run() {
        log.trace("The kitchen" + Thread.currentThread().getId() + " receive the order:" + cookedOrder.getId());
        cookedOrder.setOrderStatus(OrderStatus.Cooked);
        cookedOrder.setOrderedTimestamp();
        log.trace("The kitchen" + Thread.currentThread().getId() + " cooked the order:" + cookedOrder.getId());
        boolean putIntoShelf = false;
        while (!putIntoShelf) {
            ShelfInfo shelfInfo = this.orderManager.getShelfInfos().get(cookedOrder.getTemp());
            ShelfInfo overflowShelfInfo = this.orderManager.getShelfInfos()
                    .get(this.orderManager.getOverflowShelfKey());
            switch (cookedOrder.getOrderStatus()) {
            case Cooked:
                cookedOrder.setOrderStatus(OrderStatus.tryPutIntoSpecificShelf);
                log.trace("The kitchen" + Thread.currentThread().getId() + " try to put the order:"
                        + cookedOrder.getId() + " into " + shelfInfo.getName());
                putIntoShelf = this.orderManager.putIntoShelf(cookedOrder, cookedOrder.getTemp());
                break;
            case tryPutIntoSpecificShelf:
                log.trace("The kitchen" + Thread.currentThread().getId() + " is failed to put the order:"
                        + cookedOrder.getId() + " into " + shelfInfo.getName() + ", now try to put it into "
                        + overflowShelfInfo.getName());
                cookedOrder.setOrderStatus(OrderStatus.TryPutIntoOverFlowShelf);
                putIntoShelf = this.orderManager.putIntoShelf(cookedOrder, this.orderManager.getOverflowShelfKey());
                break;
            default:
                log.trace("The kitchen" + Thread.currentThread().getId() + " is failed to put the order:"
                        + cookedOrder.getId() + " into " + overflowShelfInfo.getName());
                putIntoShelf = this.orderManager.moveOutOrderFromOverFlow(cookedOrder);
                break;
            }
        }
        log.trace("The kitchen put the order:" + cookedOrder.getId() + " into " + cookedOrder.getShelfInfo().getName()
                + " and call the courier");
        this.restaurant.notifyCourier(cookedOrder);

    }

}
