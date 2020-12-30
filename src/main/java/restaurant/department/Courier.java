package restaurant.department;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public class Courier extends Thread {
    private CookedOrder cookedOrder;
    private OrderManager orderManager;
    private Set<String> checkedShelves;
    private CountDownLatch countCourier;
    private int waitMin;
    private int waitMax;

    public Courier(CookedOrder cookedOrder, OrderManager orderManager, CountDownLatch countCourier, int waitMin,
            int waitMax) {
        super();
        this.cookedOrder = cookedOrder;
        this.orderManager = orderManager;
        this.countCourier = countCourier;
        this.waitMin = waitMin;
        this.waitMax = waitMax;
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
        // String orderId = this.cookedOrder.getId();
        // try {
        // Thread.sleep(
        // (new Random().nextInt(this.waitMax) % (this.waitMax - this.waitMin + 1) +
        // this.waitMin) * 1000);
        // // String orderId = this.cookedOrder.getId();
        // System.out.println("cookedOrder id:" + orderId);
        // String allowableTemperature =
        // this.cookedOrder.getShelfInfo().getAllowableTemperature();
        // CookedOrder deliveryOrder =
        // this.orderManager.takeOutOrder(allowableTemperature, orderId);
        // if (deliveryOrder == null &&
        // allowableTemperature.equals(this.orderManager.getOverflowShelfKey())) {
        // checkedShelves = new HashSet<String>();
        // checkedShelves.add(this.orderManager.getOverflowShelfKey());
        // Map<String, ShelfInfo> shelfInfos = this.orderManager.getShelfInfos();
        // for (String shelfInfoKey : shelfInfos.keySet()) {
        // if (checkedShelves.contains(shelfInfoKey))
        // continue;
        // checkedShelves.add(shelfInfoKey);
        // deliveryOrder = this.orderManager.takeOutOrder(shelfInfoKey, orderId);
        // if (deliveryOrder != null)
        // break;
        // }
        // }
        // if (deliveryOrder == null) {
        // System.out.println("Failed to delivery order:" + orderId + ", it should be
        // already wasted");
        // }
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // } finally {
        this.countCourier.countDown();
        // }
    }

}
