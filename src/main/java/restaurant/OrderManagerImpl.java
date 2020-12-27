package restaurant;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class OrderManagerImpl implements OrderManager {
    private Map<String, Map<String, CookedOrder>> ordersInShelves;
    private Map<String, ShelfInfo> shelfInfos;
    private Vector<CookedOrder> deliveredOrders;
    private Vector<CookedOrder> wastedOrders;
    private String overflowShelfKey;

    public CookedOrder takeOutOrder(String allowedTemperature, String orderId) {
        CookedOrder returnOrder = null;
        Map<String, CookedOrder> singleTempShelf = this.ordersInShelves.get(allowedTemperature);
        if (singleTempShelf == null)
            return returnOrder;
        synchronized (singleTempShelf) {
            returnOrder = singleTempShelf.get(orderId);
            if (returnOrder != null) {
                singleTempShelf.remove(orderId);
                returnOrder.setFinalValue();
                if (returnOrder.getFinalValue() < 0) {
                    returnOrder.setOrderStatus(OrderStatus.Delivered);
                    this.discardOrder(returnOrder);
                    this.displayAllOrderStatus("Discard the order <" + returnOrder.getName() + "> from the shelf <"
                            + allowedTemperature + ">. The order value is <" + returnOrder.getFinalValue() + ">");
                } else {
                    returnOrder.setOrderStatus(OrderStatus.Wasted);
                    this.completeOrder(returnOrder);
                    this.displayAllOrderStatus("Deliver the order " + returnOrder.getName() + " from the shelf <"
                            + allowedTemperature + ">");
                }
            }
        }
        return returnOrder;
    }

    private CookedOrder putIntoShelf(CookedOrder cookedOrder, String allowedTemperature) {
        Map<String, CookedOrder> singleTempShelf = this.ordersInShelves.get(allowedTemperature);
        if (singleTempShelf == null) {
            return this.tryPutIntoShelf(cookedOrder, singleTempShelf);
        }
        ShelfInfo shelfInfo = this.shelfInfos.get(allowedTemperature);
        int allowedCapacity = shelfInfo.getCapacity();
        if (singleTempShelf.size() == allowedCapacity) {
            return this.tryPutIntoShelf(cookedOrder, singleTempShelf);
        }
        boolean putIntoOtherShelf = false;
        synchronized (singleTempShelf) {
            if (singleTempShelf.size() < allowedCapacity) {
                OrderStatus currentOrderStatus = cookedOrder.getOrderStatus();
                cookedOrder.setShelfInfo(shelfInfo);
                cookedOrder.setOrderStatus(OrderStatus.InTheShelf);
                cookedOrder.setShelfInfo(shelfInfo);
                singleTempShelf.put(cookedOrder.getId(), cookedOrder);
                if (!currentOrderStatus.equals(OrderStatus.InTheShelf)) {
                    this.displayAllOrderStatus(
                            "Put the order <" + cookedOrder.getName() + "> to the shelf <" + allowedTemperature + ">");
                }
            } else {
                putIntoOtherShelf = true;
            }
        }
        if (putIntoOtherShelf) {
            return this.tryPutIntoShelf(cookedOrder, singleTempShelf);
        }
        return cookedOrder;
    }

    public CookedOrder tryPutIntoShelf(CookedOrder cookedOrder, Map<String, CookedOrder> singleTempShelf) {
        switch (cookedOrder.getOrderStatus()) {
        case Cooked:
            cookedOrder.setOrderStatus(OrderStatus.tryPutIntoSpecificShelf);
            putIntoShelf(cookedOrder, cookedOrder.getTemp());
            break;
        case tryPutIntoSpecificShelf:
            cookedOrder.setOrderStatus(OrderStatus.TryPutIntoOverFlowShelf);
            putIntoShelf(cookedOrder, this.overflowShelfKey);
            break;
        case TryPutIntoOverFlowShelf:
            synchronized (singleTempShelf) {
                Map<Integer, CookedOrder> userSelectOptions = new HashMap<Integer, CookedOrder>();
                int selectIndex = 0;
                for (String orderId : singleTempShelf.keySet()) {
                    userSelectOptions.put(selectIndex, singleTempShelf.get(orderId));
                    selectIndex++;
                }
                System.out.println(
                        "The OverFlow Shelf is full right now, the Order you choose will be moved to another Shelf which has rooms:");
                for (int index = 0; index < selectIndex; index++) {
                    System.out.println("Index: " + index + " --> " + userSelectOptions.get(index).getName());
                }
                CookedOrder selectedOrder = null;
                java.util.Scanner in = new java.util.Scanner(System.in);
                try {
                    int selectedIndex = in.nextInt();
                    selectedOrder = userSelectOptions.get(selectedIndex);
                } catch (Exception e) {
                    System.out.println("The order index you choose is not in the list");
                }
                if (selectedOrder == null) {
                    randomWastedOrder(singleTempShelf);
                } else {
                    String allowedTemperature = null;
                    for (String key : this.shelfInfos.keySet()) {
                        if (key.equals(this.overflowShelfKey))
                            continue;
                        selectedOrder.setOrderStatus(OrderStatus.InTheShelf);
                        putIntoShelf(selectedOrder, key);
                        if (selectedOrder.getOrderStatus().equals(OrderStatus.InTheShelf)) {
                            allowedTemperature = key;
                            break;
                        }
                    }
                    if (selectedOrder.getOrderStatus().equals(OrderStatus.InTheShelf)) {
                        singleTempShelf.remove(selectedOrder.getId());
                        this.displayAllOrderStatus("Move the order <" + cookedOrder.getName() + "> from the shelf <"
                                + this.overflowShelfKey + "> to <" + allowedTemperature + ">");
                    } else {
                        selectedOrder.setOrderStatus(OrderStatus.InTheShelf);
                        randomWastedOrder(singleTempShelf);
                    }
                }
                putIntoShelf(cookedOrder, this.overflowShelfKey);
            }
            break;
        case InTheShelf:
            cookedOrder.setOrderStatus(OrderStatus.MoveOutFromOverFlowFailed);
            break;
        default:
            break;
        }
        return cookedOrder;
    }

    private void randomWastedOrder(Map<String, CookedOrder> singleTempShelf) {
        Random generator = new Random();
        Object[] values = singleTempShelf.values().toArray();
        CookedOrder randomWastedOrder = (CookedOrder) values[generator.nextInt(values.length)];
        singleTempShelf.values().remove(randomWastedOrder);
        randomWastedOrder.setOrderStatus(OrderStatus.Wasted);
        this.discardOrder(randomWastedOrder);
        this.displayAllOrderStatus("Random discard the order <" + randomWastedOrder.getName() + "> from the shelf <"
                + this.overflowShelfKey + ">");
    }

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
        this.deliveredOrders = new Vector<CookedOrder>();
        this.wastedOrders = new Vector<CookedOrder>();
    }

    private void displayAllOrderStatus(String event) {
        synchronized (this) {
            System.out.println("-------------------------Start--------------------------------");
            System.out.println("Event: " + event);
            for (String key : ordersInShelves.keySet()) {
                StringBuffer sb = new StringBuffer(key + ": " + ordersInShelves.get(key).size() + " ");
                Map<String, CookedOrder> singleTempShelf = ordersInShelves.get(key);
                for (String orderId : singleTempShelf.keySet()) {
                    CookedOrder cookedOrder = singleTempShelf.get(orderId);
                    sb.append("<" + cookedOrder.getName() + ">  ");
                }
                System.out.println(sb.toString());
            }
            StringBuffer sb = new StringBuffer("Delivered: " + deliveredOrders.size() + " ");
            for (CookedOrder cookedOrder : deliveredOrders) {
                sb.append("<" + cookedOrder.getName() + ">  ");
            }
            System.out.println(sb.toString());
            sb = new StringBuffer("Wasted: " + wastedOrders.size() + " ");
            for (CookedOrder cookedOrder : wastedOrders) {
                sb.append("<" + cookedOrder.getName() + ">  ");
            }
            System.out.println(sb.toString());
            System.out.println("--------------------------End-------------------------------");
            System.out.println();
        }
    }

    public String getOverflowShelfKey() {
        return overflowShelfKey;
    }

    public Map<String, ShelfInfo> getShelfInfos() {
        return shelfInfos;
    }

    public void completeOrder(CookedOrder deliveryOrder) {
        this.deliveredOrders.add(deliveryOrder);
    }

    public void discardOrder(CookedOrder wastedOrder) {
        this.wastedOrders.add(wastedOrder);
    }

}
