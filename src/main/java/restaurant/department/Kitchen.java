package restaurant.department;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;
import restaurante.constants.DiscardReason;
import restaurante.constants.OrderStatus;

public class Kitchen implements Callable<CookedOrder> {
    private static Logger log = Logger.getLogger(Kitchen.class);
    private static Object lockedObj = new Object();
    private CookedOrder cookedOrder;
    private OrderManager orderManager;
    private String overflowShelfKey;
    private int movingOrderTimeOut;

    public Kitchen(OrderManager orderManager, CookedOrder order, int movingOrderTimeOut) {
        this.cookedOrder = order;
        this.orderManager = orderManager;
        this.overflowShelfKey = this.orderManager.getOverflowShelfKey();
        this.movingOrderTimeOut = movingOrderTimeOut;

    }

    @Override
    public CookedOrder call() {
        this.cookedOrder.setOrderStatus(OrderStatus.Cooked);
        return this.tryPutIntoShelf(this.cookedOrder);
    }

    private CookedOrder tryPutIntoShelf(CookedOrder cookedOrder) {
        boolean putSuccess = false;
        ShelfInfo shelfInfo = this.orderManager.getShelfInfos().get(cookedOrder.getTemp());
        ShelfInfo overflowShelfInfo = this.orderManager.getShelfInfos().get(this.overflowShelfKey);
        Map<String, CookedOrder> singleTempShelf;
        while (!putSuccess) {
            switch (cookedOrder.getOrderStatus()) {
            case Cooked:
                cookedOrder.setOrderStatus(OrderStatus.PutIntoSpecificShelf);
                if (shelfInfo == null) {
                    log.trace("Could not find the shelf allow the temperature " + cookedOrder.getTemp()
                            + "now try to put the order " + cookedOrder.getId() + " into "
                            + overflowShelfInfo.getName());
                } else {
                    this.orderManager.takeCurrentSnapshot(
                            "The kitchen " + Thread.currentThread().getId() + " receive and cooked the order:"
                                    + cookedOrder.getId() + " try to put into " + shelfInfo.getName());
                    cookedOrder.setOrderedTimestamp();
                    singleTempShelf = this.orderManager.getSingleTempShelf(this.cookedOrder.getTemp());
                    putSuccess = this.putIntoShelf(cookedOrder, shelfInfo, singleTempShelf);
                }
                break;
            case PutIntoSpecificShelf:
                cookedOrder.setOrderStatus(OrderStatus.PutIntoOverflowShelf);
                singleTempShelf = this.orderManager.getSingleTempShelf(this.overflowShelfKey);
                if (shelfInfo != null) {
                    log.trace("The kitchen " + Thread.currentThread().getId() + " is failed to put the order:"
                            + cookedOrder.getId() + " into " + shelfInfo.getName() + ", now try to put it into "
                            + overflowShelfInfo.getName());
                }
                putSuccess = this.putIntoShelf(cookedOrder, overflowShelfInfo, singleTempShelf);
                break;
            default:
                singleTempShelf = this.orderManager.getSingleTempShelf(this.overflowShelfKey);
                log.trace("The kitchen " + Thread.currentThread().getId() + " is failed to put the order:"
                        + cookedOrder.getId() + " into " + overflowShelfInfo.getName());
                putSuccess = this.moveOutOrderFromOverFlow(cookedOrder, overflowShelfInfo, singleTempShelf);
                break;
            }
        }
        this.orderManager.takeCurrentSnapshot("The kitchen " + Thread.currentThread().getId() + " put the order:"
                + cookedOrder.getId() + " into " + cookedOrder.getShelfInfo().getName() + " and call the courier");
        return this.cookedOrder;
    }

    private boolean moveOutOrderFromOverFlow(CookedOrder cookedOrder, ShelfInfo overflowShelfInfo,
            Map<String, CookedOrder> singleTempShelf) {
        String selectedIndex = null;
        Integer selectIndex = 0;
        CookedOrder selectedOrder = null;
        CookedOrder randomWastedOrder = null;
        Map<String, CookedOrder> userSelectOptions = new HashMap<String, CookedOrder>();
        synchronized (lockedObj) {
            synchronized (singleTempShelf) {
                if (singleTempShelf.size() < overflowShelfInfo.getCapacity()) {
                    return this.putIntoShelf(cookedOrder, overflowShelfInfo, singleTempShelf);
                } else {
                    for (String orderId : singleTempShelf.keySet()) {
                        userSelectOptions.put(selectIndex.toString(), singleTempShelf.get(orderId));
                        selectIndex++;
                    }
                    log.info(
                            "---------------------------------------------------------------------------------------------------------------");
                    log.info("Warning: the " + overflowShelfInfo.getName() + " is full " + singleTempShelf.size() + ":"
                            + overflowShelfInfo.getCapacity() + ", please move out an order in "
                            + this.movingOrderTimeOut + " seconds before move in the order:" + cookedOrder.getId());
                    log.info(
                            "---------------------------------------------------------------------------------------------------------------");
                    for (Integer index = 0; index < selectIndex; index++) {
                        log.info("#No " + index + " --> " + userSelectOptions.get(index.toString()).getId());
                    }
                }
            }
            selectedIndex = this.getSelectedIndex();
            if (selectedIndex == null || selectedIndex.trim().equals("")) {
                log.info("You didn't choose any order.");
            } else {
                selectedOrder = userSelectOptions.get(selectedIndex);
                if (selectedOrder == null) {
                    if (selectedIndex != null) {
                        log.info("The #No you choose is not in the option list");
                    }
                } else {
                    log.info("You pick #No " + selectedIndex + " <" + selectedOrder.getId() + ">");
                }
            }
            synchronized (singleTempShelf) {
                if (singleTempShelf.size() < overflowShelfInfo.getCapacity()) {
                    log.info("The " + overflowShelfInfo.getName()
                            + " has rooms by the time you select,so the order you choose will stay in the "
                            + overflowShelfInfo.getName());
                } else {
                    if (selectedOrder == null) {
                        randomWastedOrder = this.randomWastedOrder(singleTempShelf, overflowShelfInfo);
                    } else {
                        if (singleTempShelf.containsKey(selectedOrder.getId())) {
                            boolean putSuccess = false;
                            Map<String, ShelfInfo> shelfInfos = this.orderManager.getShelfInfos();
                            for (String key : shelfInfos.keySet()) {
                                if (key.equals(this.overflowShelfKey))
                                    continue;
                                ShelfInfo availableShelfInfo = shelfInfos.get(key);
                                Map<String, CookedOrder> availableTempShelf = this.orderManager.getSingleTempShelf(key);
                                if (putIntoShelf(selectedOrder, availableShelfInfo, availableTempShelf)) {
                                    putSuccess = true;
                                    break;
                                }
                            }
                            if (putSuccess) {
                                singleTempShelf.remove(selectedOrder.getId());
                            } else {
                                log.info("No shelf is available now, The kitchen " + Thread.currentThread().getId()
                                        + " will randomly discard an order from the " + overflowShelfInfo.getName()
                                        + ", and then put in the order:" + cookedOrder.getId());
                                randomWastedOrder = randomWastedOrder(singleTempShelf, overflowShelfInfo);
                            }
                        } else {
                            log.info(
                                    "The order you choose might be delivered or moved to another shelf allready, but the "
                                            + overflowShelfInfo.getName() + " is still full. The kitchen "
                                            + Thread.currentThread().getId()
                                            + " will randomly discard an order from the " + overflowShelfInfo.getName()
                                            + ", and then put in the order:" + cookedOrder.getId());
                            randomWastedOrder = randomWastedOrder(singleTempShelf, overflowShelfInfo);
                        }
                    }
                }
                this.putIntoShelf(cookedOrder, overflowShelfInfo, singleTempShelf);
            }
            if (randomWastedOrder != null)
                this.orderManager.takeCurrentSnapshot(
                        "The kitchen " + Thread.currentThread().getId() + " randomly discard the order "
                                + randomWastedOrder.getId() + " from the " + overflowShelfInfo.getName());
        }
        return true;
    }

    private boolean putIntoShelf(CookedOrder cookedOrder, ShelfInfo shelfInfo,
            Map<String, CookedOrder> singleTempShelf) {
        if (singleTempShelf == null) {
            return false;
        }
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
                singleTempShelf.put(cookedOrder.getId(), cookedOrder);
                if (currentOrderStatus != OrderStatus.InTheShelf) {
                    log.trace("The kitchen " + Thread.currentThread().getId() + "successfully put the order "
                            + cookedOrder.getId() + " to the " + shelfInfo.getName());
                } else {
                    log.info("You successfully move the order " + cookedOrder.getId() + " to the "
                            + shelfInfo.getName());
                }
                return true;
            } else {
                return false;
            }
        }

    }

    private CookedOrder randomWastedOrder(Map<String, CookedOrder> singleTempShelf, ShelfInfo overflowShelfInfo) {
        Random generator = new Random();
        Object[] values = singleTempShelf.values().toArray();
        CookedOrder randomWastedOrder = (CookedOrder) values[generator.nextInt(values.length)];
        singleTempShelf.values().remove(randomWastedOrder);
        randomWastedOrder.setOrderStatus(OrderStatus.Wasted);
        randomWastedOrder.setDiscardReason(DiscardReason.RandomlyChoose);
        randomWastedOrder.setFinalValue();
        this.orderManager.getWastedOrders().add(randomWastedOrder);
        log.info("The kitchen " + Thread.currentThread().getId() + " randomly discard the order "
                + randomWastedOrder.getId() + " from the " + overflowShelfInfo.getName());
        return randomWastedOrder;
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
