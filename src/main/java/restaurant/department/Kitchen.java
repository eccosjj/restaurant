package restaurant.department;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import restaurant.constants.DiscardReason;
import restaurant.constants.OrderStatus;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

/**
 * The Kitchen class, a thread class put order into shelf, or move order out of
 * overflow to any other available shelf
 * 
 * @author junjiesun
 *
 */
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

    /**
     * the thread method, put the order status Cooked then try to put into specific
     * shelf
     */
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
                // first time put, just put it into specific shelf according to the order's
                // temp.
                cookedOrder.setOrderStatus(OrderStatus.PutIntoSpecificShelf);
                if (shelfInfo == null) {
                    // the order's temp is invalid then try to put it into overflow shelf.
                    log.trace("Could not find the shelf allow the temperature " + cookedOrder.getTemp()
                            + "now try to put the order " + cookedOrder.getId() + " into "
                            + overflowShelfInfo.getName());
                } else {
                    // first time put, just put it into specific shelf according to the order's
                    // temp.
                    this.orderManager.takeCurrentSnapshot(
                            "The kitchen " + Thread.currentThread().getId() + " receive and cooked the order:"
                                    + cookedOrder.getId() + " try to put into " + shelfInfo.getName());
                    cookedOrder.setOrderedTimestamp();
                    singleTempShelf = this.orderManager.getSingleTempShelf(this.cookedOrder.getTemp());
                    putSuccess = this.putIntoShelf(cookedOrder, shelfInfo, singleTempShelf);
                }
                break;
            case PutIntoSpecificShelf:
                // try to put into specific shelf failed, now try to put into the overflow shelf
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
                // try to put into overflow shelf failed, now try to move out an order before
                // put in.
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
        // lock the moving action to make the choosing action linear.
        synchronized (lockedObj) {
            // lock the overflow shelf
            synchronized (singleTempShelf) {
                // if the overflow shelf has rooms then just put in the order
                if (singleTempShelf.size() < overflowShelfInfo.getCapacity()) {
                    return this.putIntoShelf(cookedOrder, overflowShelfInfo, singleTempShelf);
                } else {
                    // display the order list of the overflow shelf and let the user choose one to
                    // move out.
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
            // unlock the overflow shelf and let the user choose.
            selectedIndex = this.getSelectedIndex();
            if (selectedIndex == null || selectedIndex.trim().equals("")) {
                // user didn't choose or time out.
                log.info("You didn't choose any order.");
            } else {
                selectedOrder = userSelectOptions.get(selectedIndex);
                if (selectedOrder == null) {
                    if (selectedIndex != null) {
                        // the user choose index is invalid
                        log.info("The #No you choose is not in the option list");
                    }
                } else {
                    // display the user choose order id
                    log.info("You pick #No " + selectedIndex + " <" + selectedOrder.getId() + ">");
                }
            }
            // lock the overflow again
            synchronized (singleTempShelf) {
                // if the overflow shelf has rooms then just put in the order, it's possible
                // because the overflow is not locked during the user choose process
                if (singleTempShelf.size() < overflowShelfInfo.getCapacity()) {
                    log.info("The " + overflowShelfInfo.getName()
                            + " has rooms by the time you select,so the order you choose will stay in the "
                            + overflowShelfInfo.getName());
                } else {
                    if (selectedOrder == null) {
                        // the user choose index is invalid, so just randomly discard an order from the
                        // overflow shelf
                        randomWastedOrder = this.randomWastedOrder(singleTempShelf, overflowShelfInfo);
                    } else {
                        // check the user choose order is still in the overflow shelf
                        if (singleTempShelf.containsKey(selectedOrder.getId())) {
                            boolean putSuccess = false;
                            Map<String, ShelfInfo> shelfInfos = this.orderManager.getShelfInfos();
                            for (String key : shelfInfos.keySet()) {
                                if (key.equals(this.overflowShelfKey))
                                    continue;
                                ShelfInfo availableShelfInfo = shelfInfos.get(key);
                                // try to put the order into any other shelf
                                Map<String, CookedOrder> availableTempShelf = this.orderManager.getSingleTempShelf(key);
                                if (putIntoShelf(selectedOrder, availableShelfInfo, availableTempShelf)) {
                                    putSuccess = true;
                                    break;
                                }
                            }
                            if (putSuccess) {
                                // put success, remove the user choose order from overflow shelf
                                singleTempShelf.remove(selectedOrder.getId());
                            } else {
                                // put failed, no shelf has rooms, randomly discard an order from the
                                // overflow shelf
                                log.info("No shelf is available now, The kitchen " + Thread.currentThread().getId()
                                        + " will randomly discard an order from the " + overflowShelfInfo.getName()
                                        + ", and then put in the order:" + cookedOrder.getId());
                                randomWastedOrder = randomWastedOrder(singleTempShelf, overflowShelfInfo);
                            }
                        } else {
                            // the user choose order is not in the overflow shelf, randomly discard an order
                            // from the overflow shelf
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
                // start put in the orignal order.
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
        // lock the shelf
        synchronized (singleTempShelf) {
            // check if the shelf has rooms
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

    /**
     * Randomly discard an order from overflow shelf
     * 
     * @param singleTempShelf
     * @param overflowShelfInfo
     * @return
     */
    private CookedOrder randomWastedOrder(Map<String, CookedOrder> singleTempShelf, ShelfInfo overflowShelfInfo) {
        Random generator = new Random();
        Object[] values = singleTempShelf.values().toArray();
        // random select a order from overflow shelf
        CookedOrder randomWastedOrder = (CookedOrder) values[generator.nextInt(values.length)];
        // discard order
        singleTempShelf.values().remove(randomWastedOrder);
        randomWastedOrder.setOrderStatus(OrderStatus.Wasted);
        randomWastedOrder.setDiscardReason(DiscardReason.RandomlyChoose);
        randomWastedOrder.setFinalValue();
        // put the order into the wasted order list
        this.orderManager.getWastedOrders().add(randomWastedOrder);
        log.info("The kitchen " + Thread.currentThread().getId() + " randomly discard the order "
                + randomWastedOrder.getId() + " from the " + overflowShelfInfo.getName());
        return randomWastedOrder;
    }

    /**
     * The system will accept System.in as the user input. it will be interrupted by
     * the time out method readInputStreamWithTimeout
     * 
     * @return string
     */
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

    /**
     * this method interrupt the System.in when time out
     * 
     * @param is
     * @param buf
     * @param timeoutMillis
     * @return
     * @throws IOException
     */
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
