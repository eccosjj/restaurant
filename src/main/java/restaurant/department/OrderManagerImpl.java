package restaurant.department;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;
import restaurante.constants.OrderStatus;

public class OrderManagerImpl implements OrderManager {
    Logger log = Logger.getLogger(OrderManagerImpl.class);

    private Map<String, Map<String, CookedOrder>> ordersInShelves;
    private Map<String, ShelfInfo> shelfInfos;
    private Vector<CookedOrder> deliveredOrders;
    private Vector<CookedOrder> wastedOrders;
    private String overflowShelfKey;
    private int movingOrderTimeOut;

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
                if (allowedTemperature.equals(this.overflowShelfKey)) {
                    this.tryPutIntoShelf(cookedOrder, singleTempShelf);
                } else {
                    putIntoOtherShelf = true;
                }
            }
        }
        if (putIntoOtherShelf) {
            this.tryPutIntoShelf(cookedOrder, singleTempShelf);
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
                log.info("The overflow shelf is full, please choose an order in " + this.movingOrderTimeOut
                        + " seconds, it will be moved to another shelf.");
                for (Integer index = 0; index < selectIndex; index++) {
                    log.info("#No " + index + " --> " + userSelectOptions.get(index.toString()).getId());
                }
                selectedIndex = this.getSelectedIndex();
                if (selectedIndex == null || selectedIndex.trim().equals("")) {
                    log.info("You didn't choose any order.");
                } else {
                    selectedOrder = userSelectOptions.get(selectedIndex);
                }
                if (singleTempShelf.size() == this.ordersInShelves.get(this.overflowShelfKey).size()) {
                    if (selectedOrder == null) {
                        if (selectedIndex != null) {
                            log.info(
                                    "The #No you choose in is not the option, the system will randomly discard an order in the overflow shelf");
                        }
                        this.randomWastedOrder(singleTempShelf);
                    } else {
                        log.info("You pick #No " + selectedIndex + " <" + selectedOrder.getId() + ">");
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
                                this.displayAllOrderStatus("Successfully move the order <" + selectedOrder.getId()
                                        + "> to the " + allowedTemperature + " shelf");
                            } else {
                                log.info("No shelf has rooms now, failed to move order <" + selectedOrder.getId()
                                        + ">, the system will randomly discard an order from the overflow shelf");
                                selectedOrder.setOrderStatus(OrderStatus.InTheShelf);
                                randomWastedOrder(singleTempShelf);
                            }
                        } else {
                            log.info(
                                    "The order you choose might be allready delivered or moved to another shelf, it is not in the overflow shelf anymore.The system will randomly discard an order from the overflow shelf");
                            randomWastedOrder(singleTempShelf);
                        }
                    }
                } else {
                    log.info("The overflow shelf has rooms now so it's no need to move any order.");
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
        log.info("Random discard the order <" + randomWastedOrder.getId() + "> from the overflow shelf");
        this.displayAllOrderStatus("Random discard the order <" + randomWastedOrder.getId() + "> from the shelf <"
                + this.overflowShelfKey + ">");
    }

    public OrderManagerImpl(Map<String, ShelfInfo> shelfInfos, int movingOrderTimeOut) throws Exception {
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
        this.movingOrderTimeOut = movingOrderTimeOut;
    }

    private void displayAllOrderStatus(String event) {
        synchronized (this) {
            log.debug("-------------------------Start--------------------------------");
            log.debug("Event: " + event);
            for (String key : ordersInShelves.keySet()) {
                log.debug(key + ": " + ordersInShelves.get(key).size());
                Map<String, CookedOrder> singleTempShelf = ordersInShelves.get(key);
                for (String orderId : singleTempShelf.keySet()) {
                    CookedOrder cookedOrder = singleTempShelf.get(orderId);
                    log.debug("<" + cookedOrder.getId() + ",Value:" + cookedOrder.getCurrentValue() + ",In the Shelf:"
                            + cookedOrder.getShelfInfo().getAllowableTemperature() + ">  ");
                }
            }

            log.debug("Delivered: " + deliveredOrders.size());
            for (CookedOrder cookedOrder : deliveredOrders) {
                log.debug("<" + cookedOrder.getId() + ",Value:" + cookedOrder.getFinalValue() + ",From Shelf:"
                        + cookedOrder.getShelfInfo().getAllowableTemperature() + ">  ");
            }

            log.debug("Wasted: " + wastedOrders.size() + " ");
            for (CookedOrder cookedOrder : wastedOrders) {
                log.debug("<" + cookedOrder.getId() + ",Value:" + cookedOrder.getFinalValue() + ",From Shelf:"
                        + cookedOrder.getShelfInfo().getAllowableTemperature() + ">  ");
            }
            log.debug("--------------------------End-------------------------------");
            log.debug("");
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

    private String getSelectedIndex() {
        String selectedIndex = null;
        byte[] inputData = new byte[2];
        int readLength = 0;
        try {
            readLength = readInputStreamWithTimeout(System.in, inputData, this.movingOrderTimeOut * 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (readLength > 0 && readLength <= inputData.length) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < inputData.length; i++) {
                sb.append((char) (inputData[i]));
            }
            selectedIndex = sb.toString().replaceAll("\r|\n", "");
        }
        return selectedIndex;
    }

    private int readInputStreamWithTimeout(InputStream is, byte[] buf, int timeoutMillis) throws IOException {
        int bufferOffset = 0; // bufferoffset
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;// max time out

        while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < buf.length) { // time up, buffer is full or
                                                                                          // get System in
            int readLength = Math.min(is.available(), buf.length - bufferOffset); // select buffer length
            int readResult = is.read(buf, bufferOffset, readLength);
            if (readResult == -1) { // input stream is end then break
                break;
            }
            bufferOffset += readResult;
            if (readResult > 0) { // get the content end, break;
                break;
            }
        }
        return bufferOffset;
    }

}
