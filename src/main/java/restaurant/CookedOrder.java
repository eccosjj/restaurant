package restaurant;

import java.math.BigDecimal;

/*
 * This object hold the order information, including before/after cooked information.
 * */
public class CookedOrder {

    private String id;
    private String name;
    private String temp;
    private float shelfLife;
    private float decayRate;
    // deliveredValue marked the order value when this order is delivered or
    // discarded.
    private float finalValue;
    // orderedTimestamp store the timestamp when this order is put into the shelf.
    private long orderedTimestamp;
    // store the shelfInfo info, it's changing when put different Shelf.
    private ShelfInfo shelfInfo;

    private OrderStatus orderStatus;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
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

    // return the order value, order value is keep being lower since the OrderAge is
    // growing.
    public float getCurrentValue() {
        float orderAge = this.getOrderAge();
        return (this.shelfLife - orderAge - (this.decayRate * orderAge * this.shelfInfo.getShelfDecayModifier()))
                / this.shelfLife;
    }

    @Override
    public String toString() {
        return "CookedOrder [id=" + id + ", name=" + name + ", temp=" + temp + ", shelfLife=" + shelfLife
                + ", decayRate=" + decayRate + ", finalValue=" + finalValue + ", orderedTimestamp=" + orderedTimestamp
                + ", shelfInfo=" + shelfInfo + ", orderStatus=" + orderStatus + ", getCurrentValue()="
                + getCurrentValue() + "]";
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

    public ShelfInfo getShelfInfo() {
        return shelfInfo;
    }

    public void setShelfInfo(ShelfInfo shelfInfo) {
        this.shelfInfo = shelfInfo;
    }

    // Order Age is keeping changing along with the time fly, so always use the
    // current timestamp minus orderedTimestamp;
    private float getOrderAge() {
        BigDecimal b = new BigDecimal((System.currentTimeMillis() - this.orderedTimestamp) / 1000);
        return b.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    public long getOrderedTimestamp() {
        return orderedTimestamp;
    }

    public void setOrderedTimestamp(long orderedTimestamp) {
        this.orderedTimestamp = orderedTimestamp;
    }

    public float getFinalValue() {
        return finalValue;
    }

    public void setFinalValue() {
        this.finalValue = this.getCurrentValue();
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

}
