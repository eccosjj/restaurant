package restaurant;

import java.util.concurrent.CountDownLatch;

public interface Delivery {

    void notifyCourier(CookedOrder cookedOrder, CountDownLatch countCourier);
}
