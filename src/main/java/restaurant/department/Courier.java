package restaurant.department;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import restaurant.constants.DiscardReason;
import restaurant.constants.OrderStatus;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

/**
 * The courier class, a thread class pick up the order from the shelf
 * 
 * @author junjiesun
 *
 */
public class Courier implements Callable<CookedOrder> {
    private CookedOrder cookedOrder;
    private OrderManager orderManager;
    private int waitMin;
    private int waitMax;
    private CountDownLatch courierCountDownLatch;
    private String overflowShelfKey;

    /**
     * construct
     * 
     * @param cookedOrder
     * @param orderManager
     * @param waitMin
     * @param waitMax
     * @param courierCountDownLatch
     */
    public Courier(CookedOrder cookedOrder, OrderManager orderManager, int waitMin, int waitMax,
            CountDownLatch courierCountDownLatch) {
        super();
        this.cookedOrder = cookedOrder;
        this.orderManager = orderManager;
        this.waitMin = waitMin;
        this.waitMax = waitMax;
        this.courierCountDownLatch = courierCountDownLatch;
        this.overflowShelfKey = orderManager.getOverflowShelfKey();
    }

    /**
     * The thread methond, pick up the order from the shelf
     */
    @Override
    public CookedOrder call() throws Exception {
        try {
            String orderId = this.cookedOrder.getId();
            ShelfInfo shelfInfo = this.cookedOrder.getShelfInfo();
            // check if the order is allready discard.
            if (this.cookedOrder.getOrderStatus().equals(OrderStatus.Wasted)) {
                this.orderManager.takeCurrentSnapshot("The courier " + Thread.currentThread().getId()
                        + " get noticed to order:" + orderId + " is " + OrderStatus.Wasted.toString());
            } else {
                CookedOrder deliverOrder = null;
                this.orderManager.takeCurrentSnapshot("The courier " + Thread.currentThread().getId()
                        + " get noticed to pick the order:" + orderId + " from " + shelfInfo.getName());
                // randomly wait for seconds
                try {
                    int waitSecond = this.getWaitSecond();
                    this.orderManager.takeCurrentSnapshot("The courier " + Thread.currentThread().getId()
                            + " will pick the order in :" + waitSecond + " seconds");
                    Thread.sleep(waitSecond * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Map<String, CookedOrder> singleTempShelf = this.orderManager
                        .getSingleTempShelf(shelfInfo.getAllowableTemperature());
                // lock the shelf before seeking the order
                synchronized (singleTempShelf) {
                    deliverOrder = singleTempShelf.get(orderId);
                    if (deliverOrder != null) {
                        // find order, deliver or discard
                        discardOrDeliver(deliverOrder, singleTempShelf);
                    } else if (shelfInfo.getAllowableTemperature().equals(this.overflowShelfKey)) {
                        // order is not found in overflow shelf, will seek in other shelfs
                        Map<String, ShelfInfo> shelfInfos = this.orderManager.getShelfInfos();
                        for (String shelfInfoKey : shelfInfos.keySet()) {
                            // ship the overflow shelf
                            if (shelfInfoKey.equals(this.overflowShelfKey))
                                continue;
                            Map<String, CookedOrder> otherSingleTempShelf = this.orderManager
                                    .getSingleTempShelf(shelfInfoKey);
                            // lock the shelf before seeking the order
                            synchronized (otherSingleTempShelf) {
                                deliverOrder = otherSingleTempShelf.get(orderId);
                                if (deliverOrder != null) {
                                    // find order, deliver or discard
                                    discardOrDeliver(deliverOrder, otherSingleTempShelf);
                                }
                            }
                        }
                    }
                }
                if (deliverOrder == null) {
                    this.orderManager.takeCurrentSnapshot("The courier " + Thread.currentThread().getId()
                            + " didn't find the order:" + orderId + " in any shelf. it should be already discarded");
                } else {
                    if (deliverOrder.getFinalValue() < 0) {
                        this.orderManager.takeCurrentSnapshot("The courier " + Thread.currentThread().getId()
                                + " discard the order:" + orderId + " from the " + deliverOrder.getShelfInfo().getName()
                                + " because the order's value:" + deliverOrder.getFinalValue() + " is below 0");
                    } else {
                        this.orderManager.takeCurrentSnapshot(
                                "The courier " + Thread.currentThread().getId() + " pick up the order:" + orderId
                                        + " from the " + deliverOrder.getShelfInfo().getName() + " and delivered");
                    }
                    this.cookedOrder = deliverOrder;
                }
            }
            return this.cookedOrder;
        } finally {
            this.courierCountDownLatch.countDown();
        }
    }

    // get wait second
    private int getWaitSecond() {
        if (waitMax == waitMin) {
            return waitMax;
        }
        return new Random().nextInt(waitMax - waitMin + 1) + waitMin;
    }

    /**
     * deliver or discard the order depend on the order's value
     * 
     * @param deliverOrder
     * @param singleTempShelf
     */
    private void discardOrDeliver(CookedOrder deliverOrder, Map<String, CookedOrder> singleTempShelf) {
        singleTempShelf.remove(deliverOrder.getId());
        deliverOrder.setFinalValue();
        // deliver or discard order depend on the order value is greater or lower than 0
        if (deliverOrder.getFinalValue() < 0) {
            // put the order into the wasted order list
            deliverOrder.setOrderStatus(OrderStatus.Wasted);
            deliverOrder.setDiscardReason(DiscardReason.ValueIsBelowZero);
            this.orderManager.getWastedOrders().add(deliverOrder);
        } else {
            // put the order into the delivery order list
            deliverOrder.setOrderStatus(OrderStatus.Delivered);
            this.orderManager.getDeliveredOrders().add(deliverOrder);
        }
    }
}
