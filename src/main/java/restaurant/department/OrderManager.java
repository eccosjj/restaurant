package restaurant.department;

import java.util.Map;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public interface OrderManager {
    CookedOrder takeOutOrder(String allowedTemperature, String orderId);

    boolean putIntoShelf(CookedOrder cookedOrder, String allowedTemperature);

    boolean moveOutOrderFromOverFlow(CookedOrder cookedOrder);

    String getOverflowShelfKey();

    Map<String, ShelfInfo> getShelfInfos();

}
