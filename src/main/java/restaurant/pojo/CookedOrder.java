package restaurant.pojo;

import java.math.BigDecimal;

import restaurant.constants.DiscardReason;
import restaurant.constants.OrderStatus;

/**
 * 
 * This object hold the order information, including before/after cooked
 * information.
 * 
 * @author junjiesun
 *
 */
public class CookedOrder {

    private String id;
    private String name;
    private String temp;
    private float shelfLife;
    private float decayRate;
    private Float finalValue;
    private long orderedTimestamp;
    private ShelfInfo shelfInfo;
    private OrderStatus orderStatus;
    private DiscardReason discardReason;

    @Override
    public String toString() {
        return "CookedOrder [id=" + id + ", CurrentValue=" + getCurrentValue() + ", Temp=" + temp + ", Shelf="
                + getShelfInfo().getName() + ", orderStatus=" + orderStatus + ", discardReason=" + discardReason + "]";
    }

    public String getId() {
        return id;
    }

    public String getTemp() {
        return temp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getShelfLife() {
        return shelfLife;
    }

    public void setShelfLife(float shelfLife) {
        this.shelfLife = shelfLife;
    }

    public float getDecayRate() {
        return decayRate;
    }

    public void setDecayRate(float decayRate) {
        this.decayRate = decayRate;
    }

    public long getOrderedTimestamp() {
        return orderedTimestamp;
    }

    public void setOrderedTimestamp(long orderedTimestamp) {
        this.orderedTimestamp = orderedTimestamp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFinalValue(Float finalValue) {
        this.finalValue = finalValue;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    /**
     * return the current order value, order value is being lower since the OrderAge
     * is growing.
     */
    public float getCurrentValue() {
        if (this.finalValue != null)
            return this.finalValue;
        float orderAge = this.getOrderAge();
        return (this.shelfLife - orderAge - (this.decayRate * orderAge * this.shelfInfo.getShelfDecayModifier()))
                / this.shelfLife;
    }

    public CookedOrder(String id, String name, String temp, float shelfLife, float decayRate, float finalValue,
            long orderedTimestamp, ShelfInfo shelfInfo) {
        super();
        this.id = id;
        this.name = name;
        this.temp = temp;
        this.shelfLife = shelfLife;
        this.decayRate = decayRate;
        this.finalValue = finalValue;
        this.orderedTimestamp = orderedTimestamp;
        this.shelfInfo = shelfInfo;
    }

    /**
     * return the current discard reason
     * 
     * @return discardReason
     */
    public DiscardReason getDiscardReason() {
        return discardReason;
    }

    /**
     * set the discard reason to the order
     * 
     * @param discardReason
     */
    public void setDiscardReason(DiscardReason discardReason) {
        this.discardReason = discardReason;
    }

    /**
     * store the shelfInfo info, it's changing when put different Shelf.
     * 
     * @return ShelfInfo
     */
    public ShelfInfo getShelfInfo() {
        return shelfInfo;
    }

    /**
     * set the shelfInfo info to the order
     * 
     * @param shelfInfo
     */
    public void setShelfInfo(ShelfInfo shelfInfo) {
        this.shelfInfo = shelfInfo;
    }

    /**
     * Order Age is keeping changing along with the time fly, so always use the
     * current timestamp minus orderedTimestamp;
     */
    private float getOrderAge() {
        BigDecimal b = new BigDecimal((System.currentTimeMillis() - this.orderedTimestamp) / 1000);
        return b.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    /**
     * orderedTimestamp store the timestamp when this order is put into the shelf.
     */
    public void setOrderedTimestamp() {
        this.orderedTimestamp = System.currentTimeMillis();
    }

    /**
     * finalValue marked the order value when this order is delivered or discarded.
     */
    public Float getFinalValue() {
        return finalValue;
    }

    public void setFinalValue() {
        this.finalValue = this.getCurrentValue();
    }

    /**
     * return the current order status
     * 
     * @return OrderStatus
     */
    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

}
