package restaurant;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OrderManagerImpl implements OrderManager {
    private Map<String, Map<String, CookedOrder>> ordersInShelves;
    private Map<String, ShelfInfo> shelfInfos;
    private Vector<CookedOrder> deliveredOrders;
    private Vector<CookedOrder> wastedOrders;
    private String overflowShelfKey;

    public CookedOrder takeOutOrder(String allowedTemperature, String orderId) {
        Map<String, CookedOrder> singleTempShelf = this.ordersInShelves.get(allowedTemperature);
        if (singleTempShelf == null)
            return null;
        if (allowedTemperature.equals(this.overflowShelfKey)) {
            synchronized (singleTempShelf) {
                return this.takeOutOrder(singleTempShelf, orderId, allowedTemperature);
            }
        }
        return this.takeOutOrder(singleTempShelf, orderId, allowedTemperature);
    }

    private CookedOrder takeOutOrder(Map<String, CookedOrder> singleTempShelf, String orderId,
            String allowedTemperature) {
        CookedOrder returnOrder = singleTempShelf.get(orderId);
        if (returnOrder != null) {
            singleTempShelf.remove(orderId);
            returnOrder.setFinalValue();
            if (returnOrder.getFinalValue() < 0) {
                returnOrder.setOrderStatus(OrderStatus.Delivered);
                this.discardOrder(returnOrder);
                this.displayAllOrderStatus("Discard the order <" + returnOrder.getId() + "> from the shelf <"
                        + allowedTemperature + ">. The order value is <" + returnOrder.getFinalValue() + ">");
            } else {
                returnOrder.setOrderStatus(OrderStatus.Wasted);
                this.completeOrder(returnOrder);
                this.displayAllOrderStatus(
                        "Deliver the order " + returnOrder.getId() + " from the shelf <" + allowedTemperature + ">");
            }
        }
        return returnOrder;
    }

    private void putIntoShelf(CookedOrder cookedOrder, String allowedTemperature) {
        Map<String, CookedOrder> singleTempShelf = this.ordersInShelves.get(allowedTemperature);
        if (singleTempShelf == null) {
            this.tryPutIntoShelf(cookedOrder, singleTempShelf);
            return;
        }
        ShelfInfo shelfInfo = this.shelfInfos.get(allowedTemperature);
        int allowedCapacity = shelfInfo.getCapacity();
        if (singleTempShelf.size() == allowedCapacity) {
            this.tryPutIntoShelf(cookedOrder, singleTempShelf);
            return;
        }
        boolean putIntoOtherShelf = false;
        synchronized (singleTempShelf) {
            if (singleTempShelf.size() < allowedCapacity) {
                OrderStatus currentOrderStatus = cookedOrder.getOrderStatus();
                cookedOrder.setShelfInfo(shelfInfo);
                cookedOrder.setOrderStatus(OrderStatus.InTheShelf);
                cookedOrder.setShelfInfo(shelfInfo);
                // cookedOrder.setFinalValue();
                // cookedOrder.setOrderedTimestamp();
                singleTempShelf.put(cookedOrder.getId(), cookedOrder);
                if (!currentOrderStatus.equals(OrderStatus.InTheShelf)) {
                    this.displayAllOrderStatus(
                            "Put the order <" + cookedOrder.getId() + "> to the shelf <" + allowedTemperature + ">");
                }
            } else {
                putIntoOtherShelf = true;
            }
        }
        if (putIntoOtherShelf) {
            this.tryPutIntoShelf(cookedOrder, singleTempShelf);
            return;
        }
    }

    public void tryPutIntoShelf(CookedOrder cookedOrder, Map<String, CookedOrder> singleTempShelf) {
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
                Map<String, CookedOrder> userSelectOptions = new HashMap<String, CookedOrder>();
                Integer selectIndex = 0;
                CookedOrder selectedOrder = null;
                String selectedIndex = null;
                for (String orderId : singleTempShelf.keySet()) {
                    userSelectOptions.put(selectIndex.toString(), singleTempShelf.get(orderId));
                    selectIndex++;
                }
                System.out.println(
                        "The OverFlow Shelf is full right now, please choose an order in 5 seconds, the order you choose will be moved to another Shelf which has rooms:");
                for (Integer index = 0; index < selectIndex; index++) {
                    System.out.println("Index: " + index + " --> " + userSelectOptions.get(index.toString()).getId());
                }

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                try {
                    Future<String> future = executorService.submit(new Callable<String>() {
                        public String call() {
                            String selectedIndex;
                            java.util.Scanner in = null;
                            try {
                                in = new java.util.Scanner(System.in);
                                selectedIndex = in.next();
                            } finally {
                                in.close();
                            }
                            return selectedIndex;
                        }
                    });
                    selectedIndex = future.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.out.println("Choose order from " + this.overflowShelfKey
                            + " time out, randomly discard an order from the shelf:" + this.overflowShelfKey);
                } finally {
                    executorService.shutdown();
                }
                if (selectedIndex != null) {
                    selectedOrder = userSelectOptions.get(selectedIndex);
                }
                if (singleTempShelf.size() == this.ordersInShelves.get(this.overflowShelfKey).size()) {
                    if (selectedOrder == null) {
                        if (selectedIndex != null) {
                            System.out.println(
                                    "The index you typed in is not in the list, randomly discard an order from the shelf:"
                                            + this.overflowShelfKey);
                        }
                        this.randomWastedOrder(singleTempShelf);
                    } else {
                        if (singleTempShelf.containsKey(selectedOrder.getId())) {
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
                                this.displayAllOrderStatus(
                                        "Move the order <" + cookedOrder.getId() + "> from the shelf <"
                                                + this.overflowShelfKey + "> to <" + allowedTemperature + ">");
                            } else {
                                System.out.println("Move the order <" + cookedOrder.getId() + "> from the shelf <"
                                        + this.overflowShelfKey + "> to <" + allowedTemperature
                                        + "> completed, randomly discard an order from the shelf:"
                                        + this.overflowShelfKey);
                                selectedOrder.setOrderStatus(OrderStatus.InTheShelf);
                                randomWastedOrder(singleTempShelf);
                            }
                        } else {
                            System.out.println("The order you choose is not in the shelf:" + this.overflowShelfKey
                                    + ", so skip moving order, now randomly discard an order from the shelf:"
                                    + this.overflowShelfKey);
                            randomWastedOrder(singleTempShelf);
                        }
                    }
                } else {
                    System.out.println(
                            "The " + this.overflowShelfKey + " shelf has rooms so there no need to move any order.");
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
    }

    private void randomWastedOrder(Map<String, CookedOrder> singleTempShelf) {
        Random generator = new Random();
        Object[] values = singleTempShelf.values().toArray();
        CookedOrder randomWastedOrder = (CookedOrder) values[generator.nextInt(values.length)];
        singleTempShelf.values().remove(randomWastedOrder);
        randomWastedOrder.setOrderStatus(OrderStatus.Wasted);
        randomWastedOrder.setFinalValue();
        this.discardOrder(randomWastedOrder);
        this.displayAllOrderStatus("Random discard the order <" + randomWastedOrder.getId() + "> from the shelf <"
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
        synchronized (ordersInShelves) {
            System.out.println("-------------------------Start--------------------------------");
            System.out.println("Event: " + event);
            synchronized (this.deliveredOrders) {
                synchronized (this.deliveredOrders) {
                    for (String key : ordersInShelves.keySet()) {
                        System.out.println(key + ": " + ordersInShelves.get(key).size());
                        Map<String, CookedOrder> singleTempShelf = ordersInShelves.get(key);
                        for (String orderId : singleTempShelf.keySet()) {
                            CookedOrder cookedOrder = singleTempShelf.get(orderId);
                            System.out.println("<" + cookedOrder.getId() + ",Value:" + cookedOrder.getCurrentValue()
                                    + ",In the Shelf:" + cookedOrder.getShelfInfo().getAllowableTemperature() + ">  ");
                        }
                    }

                    System.out.println("Delivered: " + deliveredOrders.size());
                    for (CookedOrder cookedOrder : deliveredOrders) {
                        System.out.println("<" + cookedOrder.getId() + ",Value:" + cookedOrder.getFinalValue()
                                + ",Out of the Shelf:" + cookedOrder.getShelfInfo().getAllowableTemperature() + ">  ");
                    }

                    System.out.println("Wasted: " + wastedOrders.size() + " ");
                    for (CookedOrder cookedOrder : wastedOrders) {
                        System.out.println("<" + cookedOrder.getId() + ",Value:" + cookedOrder.getFinalValue()
                                + ",Out of the Shelf:" + cookedOrder.getShelfInfo().getAllowableTemperature() + ">  ");
                    }
                    System.out.println("--------------------------End-------------------------------");
                    System.out.println();
                }
            }
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
