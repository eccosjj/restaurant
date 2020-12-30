package restaurant.department;

import java.util.Map;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public interface OrderManager {
    CookedOrder takeOutOrder(String allowedTemperature, String orderId);

    void tryPutIntoShelf(CookedOrder cookedOrder, Map<String, CookedOrder> singleTempShelf);

    String getOverflowShelfKey();

    Map<String, ShelfInfo> getShelfInfos();

}
