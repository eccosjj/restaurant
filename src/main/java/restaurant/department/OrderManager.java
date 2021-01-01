package restaurant.department;

import java.util.Map;
import java.util.Vector;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

/**
 * The order manager interface
 * 
 * @author junjiesun
 *
 */
public interface OrderManager {

    /**
     * return a singleTempShelf by given temperature
     * 
     * @param allowedTemperature
     * @return Map
     */
    Map<String, CookedOrder> getSingleTempShelf(String allowedTemperature);

    /**
     * return the all shelf basic info.
     * 
     * @return Map
     */
    Map<String, ShelfInfo> getShelfInfos();

    /**
     * return a shelf info by given temperature
     * 
     * @param allowedTemperature
     * @return
     */
    ShelfInfo getShelfInfo(String allowedTemperature);

    /**
     * print current snapshot of order, including all shelf's content, wasted order
     * list and delivery order list
     * 
     * @param event
     */
    void takeCurrentSnapshot(String event);

    /**
     * return the overflow shelf key
     * 
     * @return String
     */
    String getOverflowShelfKey();

    /**
     * return the delivery list
     * 
     * @return Vector
     */
    public Vector<CookedOrder> getDeliveredOrders();

    /**
     * return the wasted order list
     * 
     * @return Vector
     */
    public Vector<CookedOrder> getWastedOrders();

}
