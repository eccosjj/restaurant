package restaurant.department;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import restaurant.Restaurant;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

/**
 * Kitchen unit test
 * 
 * @author junjiesun
 *
 */
public class KitchenTest {
    Restaurant restaurant;
    List<CookedOrder> orders;
    OrderManager orderManager;
    Map<String, ShelfInfo> shelfInfos;

    @Before
    public void setUp() throws Exception {
        restaurant = new Restaurant();
        orders = this.restaurant.orders;
        orderManager = this.restaurant.orderManager;
        this.shelfInfos = this.restaurant.orderManager.getShelfInfos();
    }

    @After
    public void tearDown() throws Exception {
        restaurant = null;
        orders = null;
        orderManager = null;
        this.shelfInfos = null;
    }

    /**
     * test if the order is put into the right shelf
     */
    @Test
    public void testPutSpecificShelf() {
        int allowedCapacity = 1;
        CookedOrder order = orders.get(0);
        String temp = order.getTemp();
        // set the shelf capacity to 1
        shelfInfos.get(temp).setCapacity(allowedCapacity);
        new Kitchen(orderManager, order, restaurant.movingOrderTimeOut).call();
        Map<String, CookedOrder> singleTempShelf = this.orderManager.getSingleTempShelf(temp);
        // the order should be put into this specific shelf
        Assert.assertEquals(allowedCapacity, singleTempShelf.size());

    }

    /**
     * narrow done the shelf capacity, test if the order is put into the overflow
     * shelf
     */
    @Test
    public void testPutOverflowShelf() {
        int allowedCapacity = 1;
        CookedOrder order1 = orders.get(0);
        CookedOrder order2 = orders.get(1);
        String temp = order1.getTemp();
        order2.setTemp(temp);
        // set the shelf capacity to 1
        shelfInfos.get(temp).setCapacity(allowedCapacity);
        // put the 2 order to the same shelf which only have 1 capacity, one should be
        // put in and another should be put in overflow
        new Kitchen(orderManager, order1, restaurant.movingOrderTimeOut).call();
        new Kitchen(orderManager, order2, restaurant.movingOrderTimeOut).call();
        Map<String, CookedOrder> singleTempShelf = this.orderManager.getSingleTempShelf(temp);
        Assert.assertEquals(allowedCapacity, singleTempShelf.size());
        singleTempShelf = this.orderManager.getSingleTempShelf(this.orderManager.getOverflowShelfKey());
        Assert.assertEquals(1, singleTempShelf.size());

    }

    /**
     * make an order's temp invalid, test if it's put into the overflow shelf
     */
    @Test
    public void testWrongTempOrder() {
        int allowedCapacity = 1;
        CookedOrder order1 = orders.get(0);
        CookedOrder order2 = orders.get(1);
        String temp = order1.getTemp();
        order2.setTemp("i'm not any of a shelf");
        // set the shelf capacity to 1
        shelfInfos.get(temp).setCapacity(allowedCapacity);
        // put the 2 order to the same shelf which only have 1 capacity, one should be
        // put in and another should be put in overflow
        new Kitchen(orderManager, order1, restaurant.movingOrderTimeOut).call();
        new Kitchen(orderManager, order2, restaurant.movingOrderTimeOut).call();
        Map<String, CookedOrder> singleTempShelf = this.orderManager.getSingleTempShelf(temp);
        Assert.assertEquals(allowedCapacity, singleTempShelf.size());
        singleTempShelf = this.orderManager.getSingleTempShelf(this.orderManager.getOverflowShelfKey());
        Assert.assertEquals(1, singleTempShelf.size());

    }

}
