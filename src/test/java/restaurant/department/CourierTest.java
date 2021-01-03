package restaurant.department;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import restaurant.Restaurant;
import restaurant.constants.OrderStatus;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

/**
 * Courier unit test
 * 
 * @author junjiesun
 *
 */
public class CourierTest {
    Restaurant restaurant;
    List<CookedOrder> orders;
    OrderManager orderManager;
    ListeningExecutorService listeningExecutorService;
    Map<String, ShelfInfo> shelfInfos;

    @Before
    public void setUp() throws Exception {
        restaurant = new Restaurant();
        orders = new ArrayList<CookedOrder>();
        for (int i = 0; i < 5; i++) {
            orders.add(this.restaurant.orders.get(i));
        }
        restaurant.orders = orders;
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
     * test all roders should be discard because of the value below 0
     * 
     * @throws InterruptedException
     */
    @Test
    public void test() throws InterruptedException {
        for (CookedOrder order : this.restaurant.orders) {
            order.setShelfLife(1);
        }
        this.restaurant.waitMax = 5;
        this.restaurant.waitMin = 5;
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
        Assert.assertTrue(deliveredOrders.size() == 0);
        Assert.assertTrue(wastedOrders.size() == this.restaurant.orders.size());
        for (CookedOrder order : wastedOrders) {
            // delivered order status should be Wasted
            Assert.assertTrue(order.getOrderStatus().equals(OrderStatus.Wasted));
            // delivered order discard reason should not be null, either RandomlyChoose or
            // ValueIsBelowZero
            Assert.assertTrue(order.getDiscardReason() != null);
        }
    }

}
