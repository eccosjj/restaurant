package restaurant.department;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;
import restaurante.constants.DiscardReason;
import restaurante.constants.OrderStatus;

public class Courier implements Callable<CookedOrder> {
    private CookedOrder cookedOrder;
    private OrderManager orderManager;
    private int waitMin;
    private int waitMax;
    private CountDownLatch courierCountDownLatch;
    private String overflowShelfKey;

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

    @Override
    public CookedOrder call() throws Exception {
        try {
            String orderId = this.cookedOrder.getId();
            ShelfInfo shelfInfo = this.cookedOrder.getShelfInfo();
            if (this.cookedOrder.getOrderStatus().equals(OrderStatus.Wasted)) {
                this.orderManager.takeCurrentSnapshot("The courier " + Thread.currentThread().getId()
                        + " get noticed to order:" + orderId + " is " + OrderStatus.Wasted.toString());
            } else {
                CookedOrder deliverOrder = null;
                this.orderManager.takeCurrentSnapshot("The courier " + Thread.currentThread().getId()
                        + " get noticed to pick the order:" + orderId + " from " + shelfInfo.getName());
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
                synchronized (singleTempShelf) {
                    deliverOrder = singleTempShelf.get(orderId);
                    if (deliverOrder != null) {
                        discardOrDeliver(deliverOrder, singleTempShelf);
                    } else if (shelfInfo.getAllowableTemperature().equals(this.overflowShelfKey)) {
                        Map<String, ShelfInfo> shelfInfos = this.orderManager.getShelfInfos();
                        for (String shelfInfoKey : shelfInfos.keySet()) {
                            if (shelfInfoKey.equals(this.overflowShelfKey))
                                continue;
                            Map<String, CookedOrder> otherSingleTempShelf = this.orderManager
                                    .getSingleTempShelf(shelfInfoKey);
                            synchronized (otherSingleTempShelf) {
                                deliverOrder = otherSingleTempShelf.get(orderId);
                                if (deliverOrder != null) {
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

    private int getWaitSecond() {
        if (waitMax == waitMin) {
            return waitMax;
        }
        return new Random().nextInt(waitMax - waitMin + 1) + waitMin;
    }

    private void discardOrDeliver(CookedOrder deliverOrder, Map<String, CookedOrder> singleTempShelf) {
        singleTempShelf.remove(deliverOrder.getId());
        deliverOrder.setFinalValue();
        if (deliverOrder.getFinalValue() < 0) {
            deliverOrder.setOrderStatus(OrderStatus.Wasted);
            deliverOrder.setDiscardReason(DiscardReason.ValueIsBelowZero);
            this.orderManager.getWastedOrders().add(deliverOrder);
        } else {
            deliverOrder.setOrderStatus(OrderStatus.Delivered);
            this.orderManager.getDeliveredOrders().add(deliverOrder);
        }
    }
}
