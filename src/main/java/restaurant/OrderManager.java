package restaurant;

import java.util.Map;

public interface OrderManager {
    CookedOrder takeOutOrder(String allowedTemperature, String orderId);

    void tryPutIntoShelf(CookedOrder cookedOrder, Map<String, CookedOrder> singleTempShelf);

    String getOverflowShelfKey();

    Map<String, ShelfInfo> getShelfInfos();

    void completeOrder(CookedOrder deliveryOrder);

    void discardOrder(CookedOrder wastedOrder);

}
