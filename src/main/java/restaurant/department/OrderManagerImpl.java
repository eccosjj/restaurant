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
import restaurante.constants.DiscardReason;
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
        CookedOrder returnOrder = singleTempShelf.get(orderId);
        if (returnOrder != null) {
            singleTempShelf.remove(orderId);
            returnOrder.setFinalValue();
            if (returnOrder.getFinalValue() < 0) {
                returnOrder.setOrderStatus(OrderStatus.Wasted);
                returnOrder.setDiscardReason(DiscardReason.ValueIsBelowZero);
                this.wastedOrders.add(returnOrder);
            } else {
                returnOrder.setOrderStatus(OrderStatus.Delivered);
                this.deliveredOrders.add(returnOrder);
            }
            // this.displayAllOrderStatus();

        }
        return returnOrder;
    }

    public boolean putIntoShelf(CookedOrder cookedOrder, String allowedTemperature) {
        Map<String, CookedOrder> singleTempShelf = this.ordersInShelves.get(allowedTemperature);
        if (singleTempShelf == null) {
            return false;
        }
        ShelfInfo shelfInfo = this.shelfInfos.get(allowedTemperature);
        int allowedCapacity = shelfInfo.getCapacity();
        if (singleTempShelf.size() == allowedCapacity) {
            return false;
        }
        synchronized (singleTempShelf) {
            if (singleTempShelf.size() < allowedCapacity) {
                OrderStatus currentOrderStatus = cookedOrder.getOrderStatus();
                cookedOrder.setShelfInfo(shelfInfo);
                cookedOrder.setOrderStatus(OrderStatus.InTheShelf);
                cookedOrder.setShelfInfo(shelfInfo);
                // cookedOrder.setFinalValue();
                // cookedOrder.setOrderedTimestamp();
                singleTempShelf.put(cookedOrder.getId(), cookedOrder);
                if (currentOrderStatus.equals(OrderStatus.InTheShelf)) {
                    log.info("Successfully move the order " + cookedOrder.getId() + " to the " + shelfInfo.getName());
                } else {
                    log.trace("Successfully put the order " + cookedOrder.getId() + " to the " + shelfInfo.getName());
                }
                return true;
            } else {
                return false;
            }
        }

    }

    public synchronized boolean moveOutOrderFromOverFlow(CookedOrder cookedOrder) {
        String selectedIndex = null;
        Integer selectIndex = 0;
        CookedOrder selectedOrder = null;
        Map<String, CookedOrder> singleTempShelf = this.ordersInShelves.get(this.overflowShelfKey);
        ShelfInfo overflowShelfInfo = shelfInfos.get(this.overflowShelfKey);
        Map<String, CookedOrder> userSelectOptions = new HashMap<String, CookedOrder>();
        boolean reallyFull = false;
        synchronized (singleTempShelf) {
            if (singleTempShelf.size() == overflowShelfInfo.getCapacity()) {
                reallyFull = true;
                for (String orderId : singleTempShelf.keySet()) {
                    userSelectOptions.put(selectIndex.toString(), singleTempShelf.get(orderId));
                    selectIndex++;
                }
                log.info("The " + overflowShelfInfo.getName() + " is full " + singleTempShelf.size() + ":"
                        + overflowShelfInfo.getCapacity() + ", please choose an order in " + this.movingOrderTimeOut
                        + " seconds, it will be moved to another shelf.");
                for (Integer index = 0; index < selectIndex; index++) {
                    log.info("#No " + index + " --> " + userSelectOptions.get(index.toString()).getId());
                }
            }
        }
        if (reallyFull) {
            selectedIndex = this.getSelectedIndex();
            if (selectedIndex == null || selectedIndex.trim().equals("")) {
                log.info("You didn't choose any order.");
            } else {
                selectedOrder = userSelectOptions.get(selectedIndex);
                if (selectedOrder == null) {
                    if (selectedIndex != null) {
                        log.info("The #No you choose in is not the option list");
                    }
                } else {
                    log.info("You pick #No " + selectedIndex + " <" + selectedOrder.getId() + ">");
                }
            }
        }
        synchronized (singleTempShelf) {
            if (singleTempShelf.size() == overflowShelfInfo.getCapacity()) {
                if (selectedOrder == null) {
                    this.randomWastedOrder(singleTempShelf, overflowShelfInfo);
                } else {
                    if (singleTempShelf.containsKey(selectedOrder.getId())) {
                        boolean putSuccess = false;
                        for (String key : this.shelfInfos.keySet()) {
                            if (key.equals(this.overflowShelfKey))
                                continue;
                            if (putIntoShelf(selectedOrder, key)) {
                                putSuccess = true;
                                break;
                            }
                        }
                        if (putSuccess) {
                            singleTempShelf.remove(selectedOrder.getId());
                        } else {
                            log.info("No shelf has rooms now, failed to move order " + selectedOrder.getId()
                                    + ", the system will randomly discard an order from the "
                                    + overflowShelfInfo.getName());
                            randomWastedOrder(singleTempShelf, overflowShelfInfo);
                        }
                    } else {
                        log.info(
                                "The order you choose might be allready delivered or moved to another shelf, it is not in the "
                                        + overflowShelfInfo.getName()
                                        + " anymore.The system will randomly discard an order from the "
                                        + overflowShelfInfo.getName());
                        randomWastedOrder(singleTempShelf, overflowShelfInfo);
                    }
                }
            } else if (reallyFull) {
                log.info("The " + overflowShelfInfo.getName()
                        + " has rooms by the time you select,so the order you choose will stay in the "
                        + overflowShelfInfo.getName());
            }
            return putIntoShelf(cookedOrder, this.overflowShelfKey);
        }
    }

    private void randomWastedOrder(Map<String, CookedOrder> singleTempShelf, ShelfInfo overflowShelfInfo) {
        Random generator = new Random();
        Object[] values = singleTempShelf.values().toArray();
        CookedOrder randomWastedOrder = (CookedOrder) values[generator.nextInt(values.length)];
        singleTempShelf.values().remove(randomWastedOrder);
        randomWastedOrder.setOrderStatus(OrderStatus.Wasted);
        randomWastedOrder.setDiscardReason(DiscardReason.RandomlyChoose);
        randomWastedOrder.setFinalValue();
        this.wastedOrders.add(randomWastedOrder);
        log.info("Random discard the order " + randomWastedOrder.getId() + " from the " + overflowShelfInfo.getName());
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

    public void displayAllOrderStatus() {
        log.debug("-------------------------Start--------------------------------");
        // log.debug("Event: " + event);
        for (String key : ordersInShelves.keySet()) {
            ShelfInfo shelfInfo = shelfInfos.get(key);
            log.debug(shelfInfo.getName() + ": Capacity:" + shelfInfo.getCapacity() + " Ocupied:"
                    + ordersInShelves.get(key).size());
            Map<String, CookedOrder> singleTempShelf = ordersInShelves.get(key);
            for (String orderId : singleTempShelf.keySet()) {
                CookedOrder cookedOrder = singleTempShelf.get(orderId);
                // log.debug("<" + cookedOrder.getId() + ">,<OrderStatus:" +
                // cookedOrder.getOrderStatus().toString()
                // + ">,<Value:" + cookedOrder.getCurrentValue() + ">,<Temp:" +
                // cookedOrder.getTemp()
                // + ">,<In the:" + cookedOrder.getShelfInfo().getName() + "> ");
                log.debug(cookedOrder.toString());

            }
        }
        synchronized (deliveredOrders) {
            log.debug("Delivered: " + deliveredOrders.size());

            for (CookedOrder cookedOrder : deliveredOrders) {
                // log.debug("<" + cookedOrder.getId() + ">,<OrderStatus:" +
                // cookedOrder.getOrderStatus().toString()
                // + ">,<Value:" + cookedOrder.getFinalValue() + ">,<Temp:" +
                // cookedOrder.getTemp() + ">,<From:"
                // + cookedOrder.getShelfInfo().getName() + "> ");
                log.debug(cookedOrder.toString());
            }
        }
        synchronized (wastedOrders) {
            log.debug("Wasted: " + wastedOrders.size() + " ");
            for (CookedOrder cookedOrder : wastedOrders) {
                // log.debug("<" + cookedOrder.getId() + ">,<OrderStatus:" +
                // cookedOrder.getOrderStatus().toString()
                // + ">,<Value:" + cookedOrder.getFinalValue() + ">,<Temp:" +
                // cookedOrder.getTemp() + ">,<From:"
                // + cookedOrder.getShelfInfo().getName() + "> ");
                log.debug(cookedOrder.toString());
            }
        }
        log.debug("--------------------------End-------------------------------");
        log.debug("");

    }

    public String getOverflowShelfKey() {
        return overflowShelfKey;
    }

    public Map<String, ShelfInfo> getShelfInfos() {
        return shelfInfos;
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
