package restaurant.department;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public class Courier extends Thread {
    private static Logger log = Logger.getLogger(Courier.class);
    private CookedOrder cookedOrder;
    private OrderManager orderManager;
    private Set<String> checkedShelves;
    private int waitMin;
    private int waitMax;
    private CountDownLatch courierCountDownLatch;

    public Courier(CookedOrder cookedOrder, OrderManager orderManager, int waitMin, int waitMax,
            CountDownLatch courierCountDownLatch) {
        super();
        this.cookedOrder = cookedOrder;
        this.orderManager = orderManager;
        this.waitMin = waitMin;
        this.waitMax = waitMax;
        this.courierCountDownLatch = courierCountDownLatch;
    }

    public CookedOrder getCookedOrder() {
        return cookedOrder;
    }

    public void setCookedOrder(CookedOrder cookedOrder) {
        this.cookedOrder = cookedOrder;
    }

    public Set<String> getCheckedShelves() {
        return checkedShelves;
    }

    public void setCheckedShelves(Set<String> checkedShelves) {
        this.checkedShelves = checkedShelves;
    }

    public void run() {
        try {
            String orderId = this.cookedOrder.getId();
            ShelfInfo shelfInfo = this.cookedOrder.getShelfInfo();
            log.trace("The courier" + Thread.currentThread().getId() + " get noticed to pick the order:" + orderId
                    + " from " + shelfInfo.getName());
            try {
                int waitSecond = (new Random().nextInt(this.waitMax) % (this.waitMax - this.waitMin + 1)
                        + this.waitMin);
                log.trace("The courier" + Thread.currentThread().getId() + " will pick the order in :" + waitSecond
                        + " seconds");
                Thread.sleep(waitSecond * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookedOrder deliveryOrder = this.orderManager.takeOutOrder(shelfInfo.getAllowableTemperature(), orderId);
            if (deliveryOrder == null
                    && shelfInfo.getAllowableTemperature().equals(this.orderManager.getOverflowShelfKey())) {
                checkedShelves = new HashSet<String>();
                checkedShelves.add(this.orderManager.getOverflowShelfKey());
                Map<String, ShelfInfo> shelfInfos = this.orderManager.getShelfInfos();
                for (String shelfInfoKey : shelfInfos.keySet()) {
                    if (checkedShelves.contains(shelfInfoKey))
                        continue;
                    checkedShelves.add(shelfInfoKey);
                    deliveryOrder = this.orderManager.takeOutOrder(shelfInfoKey, orderId);
                    if (deliveryOrder != null)
                        break;
                }
            }
            if (deliveryOrder == null) {
                log.trace("The courier" + Thread.currentThread().getId() + " is failed to delivered the order:"
                        + orderId + ". The order should be already discard");
            } else {
                log.trace("The courier" + Thread.currentThread().getId() + " delivered the order:" + orderId);
            }
        } finally {
            this.courierCountDownLatch.countDown();
        }

    }

}
