package restaurant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import restaurant.constants.OrderStatus;
import restaurant.department.Kitchen;
import restaurant.department.OrderManager;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

/**
 * Restaurant unit test
 * 
 * @author junjiesun
 *
 */
public class RestaurantTest {

    Restaurant restaurant;
    List<CookedOrder> orders;
    OrderManager orderManager;
    ListeningExecutorService listeningExecutorService;
    Map<String, ShelfInfo> shelfInfos;

    @Before
    public void setUp() throws Exception {
        restaurant = new Restaurant();
        orders = this.restaurant.orders;
        orderManager = this.restaurant.orderManager;
        listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(orders.size()));
        this.shelfInfos = this.restaurant.orderManager.getShelfInfos();
    }

    @After
    public void tearDown() throws Exception {
        restaurant = null;
        orders = null;
        orderManager = null;
        listeningExecutorService = null;
        this.shelfInfos = null;
    }

    /**
     * test if the configuration is correct
     */
    @Test
    public void validateConfigurations() {
        Assert.assertTrue(restaurant.waitMin >= 0);
        Assert.assertTrue(restaurant.waitMax >= 0);
        Assert.assertTrue(restaurant.waitMax >= restaurant.waitMin);
        Assert.assertTrue(restaurant.orders.size() > 0);
        Assert.assertTrue(restaurant.configJsonObject != null);
        Assert.assertTrue(restaurant.oneTimeReceive > 0);
        Assert.assertTrue(restaurant.pauseBetweenReceive >= 0);
        Assert.assertTrue(restaurant.orderManager != null);
        Assert.assertTrue(restaurant.movingOrderTimeOut >= 0);

    }

    /**
     * test if all order are cooked, the order status is valid
     * 
     * @throws InterruptedException
     */
    @Test
    public void testCookedAllOrders() throws InterruptedException {
        List<CookedOrder> checkDeliveredOrders = new ArrayList<CookedOrder>();
        CountDownLatch countDownLatch = new CountDownLatch(orders.size());
        for (CookedOrder order : orders) {
            ListenableFuture<CookedOrder> listenableFuture = listeningExecutorService
                    .submit(new Kitchen(orderManager, order, restaurant.movingOrderTimeOut));
            Futures.addCallback(listenableFuture, new FutureCallback<CookedOrder>() {
                @Override
                public void onSuccess(CookedOrder order) {
                    try {
                        // the order status should be either InTheShelf or Wasted, the reason it might
                        // be Wasted is it could be randomly discard in the instant between the kitchen
                        // put it in and calling the courier.
                        Assert.assertTrue(order.getOrderStatus().equals(OrderStatus.InTheShelf)
                                || order.getOrderStatus().equals(OrderStatus.Wasted));
                        checkDeliveredOrders.add(order);
                    } finally {
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, listeningExecutorService);

        }
        countDownLatch.await();
        listeningExecutorService.shutdown();
        // all orders should be cooked so it should be equals the total order size
        Assert.assertEquals(orders.size(), checkDeliveredOrders.size());
    }

    /**
     * test if any shelf is out of capacity.
     * 
     * @throws InterruptedException
     */
    @Test
    public void testShelfOutOfCapacity() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(orders.size());
        for (CookedOrder order : orders) {
            ListenableFuture<CookedOrder> listenableFuture = listeningExecutorService
                    .submit(new Kitchen(orderManager, order, restaurant.movingOrderTimeOut));
            Futures.addCallback(listenableFuture, new FutureCallback<CookedOrder>() {
                @Override
                public void onSuccess(CookedOrder order) {
                    try {
                        for (String key : shelfInfos.keySet()) {
                            ShelfInfo shelfInfo = shelfInfos.get(key);
                            Map<String, CookedOrder> singleTempShelf = restaurant.orderManager.getSingleTempShelf(key);
                            // randomly check any shelf occupied should not out of the capacity
                            Assert.assertTrue(singleTempShelf.size() <= shelfInfo.getCapacity());
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, listeningExecutorService);

        }
        countDownLatch.await();
        listeningExecutorService.shutdown();
    }

    /**
     * test if all order are delivered or wasted, validate order status
     * 
     * @throws InterruptedException
     */
    @Test
    public void testDeliverAllOrders() throws InterruptedException {
        this.restaurant.startService();
        for (String key : shelfInfos.keySet()) {
            Map<String, CookedOrder> singleTempShelf = restaurant.orderManager.getSingleTempShelf(key);
            // all shelfs should be empty;
            Assert.assertTrue(singleTempShelf.size() == 0);
        }
        Vector<CookedOrder> deliveredOrders = this.restaurant.orderManager.getDeliveredOrders();
        Vector<CookedOrder> wastedOrders = this.restaurant.orderManager.getWastedOrders();
        // delivered order plus discarded order should be equal to totally order,
        Assert.assertTrue(deliveredOrders.size() + wastedOrders.size() == this.orders.size());
        for (CookedOrder order : deliveredOrders) {
            // delivered order status should be Delivered
            Assert.assertTrue(order.getOrderStatus().equals(OrderStatus.Delivered));
            // delivered order value should be greater than 0
            Assert.assertTrue(order.getFinalValue() > 0);
            // delivered order discard reason should be null
            Assert.assertTrue(order.getDiscardReason() == null);
        }
        for (CookedOrder order : wastedOrders) {
            // delivered order status should be Wasted
            Assert.assertTrue(order.getOrderStatus().equals(OrderStatus.Wasted));
            // delivered order discard reason should not be null, either RandomlyChoose or
            // ValueIsBelowZero
            Assert.assertTrue(order.getDiscardReason() != null);
        }

    }

}
