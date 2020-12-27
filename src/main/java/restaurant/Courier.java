package restaurant;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Courier extends Thread {
    private CookedOrder cookedOrder;
    private OrderManager orderManager;
    private Set<String> checkedShelves;
    private CountDownLatch countCourier;

    public Courier(CookedOrder cookedOrder, OrderManager orderManager, CountDownLatch countCourier) {
        super();
        this.cookedOrder = cookedOrder;
        this.orderManager = orderManager;
        this.countCourier = countCourier;
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
            // int max = 20;
            // int min = 10;
            // Random random = new Random();
            // int s = random.nextInt(max) % (max - min + 1) + min;
            // Thread.sleep(s * 1000);
//            String orderId = this.cookedOrder.getId();
//            String finalshelfInfoKey = this.cookedOrder.getTemp();
//            CookedOrder deliveryOrder = this.orderManager.takeOutOrder(finalshelfInfoKey, orderId);
//            if (deliveryOrder == null) {
//                checkedShelves.add(this.cookedOrder.getTemp());
//                finalshelfInfoKey = this.orderManager.getOverflowShelfKey();
//                deliveryOrder = this.orderManager.takeOutOrder(finalshelfInfoKey, orderId);
//                if (deliveryOrder == null) {
//                    checkedShelves.add(this.orderManager.getOverflowShelfKey());
//                    Map<String, ShelfInfo> shelfInfos = this.orderManager.getShelfInfos();
//                    for (String shelfInfoKey : shelfInfos.keySet()) {
//                        if (checkedShelves.contains(shelfInfoKey))
//                            continue;
//                        finalshelfInfoKey = shelfInfoKey;
//                        deliveryOrder = this.orderManager.takeOutOrder(finalshelfInfoKey, orderId);
//                        if (deliveryOrder != null)
//                            break;
//                    }
//                }
//            }
//            if (deliveryOrder == null) {
//                System.out.println("Failed to delivery order:" + orderId + ", it should be already wasted");
//            }
        } finally {
            this.countCourier.countDown();
        }
    }

}
