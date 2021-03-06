package restaurant.department;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

/**
 * the order manager object, hold the all order's info, including all shelf's
 * content, wasted order list and delivery order list
 * 
 * @author junjiesun
 *
 */
public class OrderManagerImpl implements OrderManager {
    static Logger log = Logger.getLogger(OrderManagerImpl.class);
    private Map<String, Map<String, CookedOrder>> ordersInShelves;
    private Map<String, ShelfInfo> shelfInfos;
    private Vector<CookedOrder> deliveredOrders;
    private Vector<CookedOrder> wastedOrders;
    private String overflowShelfKey;

    /**
     * construct, initialize the all shelfs info, wasted and delivery list, throw
     * exception when didn't find the overflow shelf
     * 
     * @param shelfInfos
     * @throws Exception
     */
    public OrderManagerImpl(Map<String, ShelfInfo> shelfInfos) throws Exception {
        super();
        this.shelfInfos = shelfInfos;
        this.ordersInShelves = new ConcurrentHashMap<String, Map<String, CookedOrder>>();
        for (String key : this.shelfInfos.keySet()) {
            ordersInShelves.put(key, new ConcurrentHashMap<String, CookedOrder>());
            if (shelfInfos.get(key).isOverFlowShelf()) {
                this.overflowShelfKey = key;
            }
        }
        if (this.overflowShelfKey == null) {
            throw new Exception(
                    "Please check the shelves.json, must set the 'isOverFlowShelf:true' in one shelf in the list");
        }
        if (this.shelfInfos.get(this.overflowShelfKey).getCapacity() <= 0) {
            throw new Exception(
                    "Please check the shelves.json, must set the 'capacity' > 0 in one shelf which contains 'isOverFlowShelf:true'");
        }
        this.deliveredOrders = new Vector<CookedOrder>();
        this.wastedOrders = new Vector<CookedOrder>();
    }

    /**
     * synchronized method, print current snapshot of order, including all shelf's
     * content, wasted order list and delivery order list
     * 
     * @param event
     *            an log event
     */
    public synchronized void takeCurrentSnapshot(String event) {
        log.trace(event);
        log.debug("-------------------------Start--------------------------------");
        log.debug("Event: " + event);
        for (String key : ordersInShelves.keySet()) {
            ShelfInfo shelfInfo = shelfInfos.get(key);
            log.debug(shelfInfo.getName() + ": Capacity:" + shelfInfo.getCapacity() + " Occupied:"
                    + ordersInShelves.get(key).size());
            Map<String, CookedOrder> singleTempShelf = ordersInShelves.get(key);
            synchronized (singleTempShelf) {
                for (String orderId : singleTempShelf.keySet()) {
                    CookedOrder cookedOrder = singleTempShelf.get(orderId);
                    if (cookedOrder != null) {
                        log.debug(cookedOrder.toString());
                    }
                }
            }
        }
        log.debug("Delivered: " + deliveredOrders.size());
        synchronized (deliveredOrders) {

            for (CookedOrder cookedOrder : deliveredOrders) {
                if (cookedOrder != null) {
                    log.debug(cookedOrder.toString());
                }
            }
        }
        log.debug("Wasted: " + wastedOrders.size() + " ");
        synchronized (wastedOrders) {
            for (CookedOrder cookedOrder : wastedOrders) {
                if (cookedOrder != null) {
                    log.debug(cookedOrder.toString());
                }
            }
        }
        log.debug("--------------------------End-------------------------------");
        log.debug("");

    }

    public Vector<CookedOrder> getDeliveredOrders() {
        return deliveredOrders;
    }

    public Vector<CookedOrder> getWastedOrders() {
        return wastedOrders;
    }

    public Map<String, ShelfInfo> getShelfInfos() {
        return shelfInfos;
    }

    @Override
    public Map<String, CookedOrder> getSingleTempShelf(String allowedTemperature) {
        return this.ordersInShelves.get(allowedTemperature);
    }

    @Override
    public ShelfInfo getShelfInfo(String allowedTemperature) {
        return this.shelfInfos.get(allowedTemperature);
    }

    @Override
    public String getOverflowShelfKey() {
        return this.overflowShelfKey;
    }

}
