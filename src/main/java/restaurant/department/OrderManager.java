package restaurant.department;

import java.util.Map;
import java.util.Vector;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public interface OrderManager {

    Map<String, CookedOrder> getSingleTempShelf(String allowedTemperature);

    Map<String, ShelfInfo> getShelfInfos();

    ShelfInfo getShelfInfo(String allowedTemperature);

    void takeCurrentSnapshot(String event);

    String getOverflowShelfKey();

    public Vector<CookedOrder> getDeliveredOrders();

    public Vector<CookedOrder> getWastedOrders();

}
