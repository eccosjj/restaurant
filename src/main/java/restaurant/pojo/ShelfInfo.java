package restaurant.pojo;

/*
 * The Shelf pojo.
 * */
public class ShelfInfo {

    private String name;
    private String allowableTemperature;
    private int capacity;
    private int shelfDecayModifier;
    private boolean isOverFlowShelf;

    public boolean isOverFlowShelf() {
        return isOverFlowShelf;
    }

    public void setOverFlowShelf(boolean isOverFlowShelf) {
        this.isOverFlowShelf = isOverFlowShelf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getAllowableTemperature() {
        return allowableTemperature;
    }

    public void setAllowableTemperature(String allowableTemperature) {
        this.allowableTemperature = allowableTemperature;
    }

    @Override
    public String toString() {
        return "ShelfInfo [name=" + name + ", allowableTemperature=" + allowableTemperature + ", capacity=" + capacity
                + ", shelfDecayModifier=" + shelfDecayModifier + ", isOverFlowShelf=" + isOverFlowShelf + "]";
    }

    public ShelfInfo(String name, String allowableTemperature, int capacity, int shelfDecayModifier,
            boolean isOverFlowShelf) {
        super();
        this.name = name;
        this.allowableTemperature = allowableTemperature;
        this.capacity = capacity;
        this.shelfDecayModifier = shelfDecayModifier;
        this.isOverFlowShelf = isOverFlowShelf;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getShelfDecayModifier() {
        return shelfDecayModifier;
    }

    public void setShelfDecayModifier(int shelfDecayModifier) {
        this.shelfDecayModifier = shelfDecayModifier;
    }
}
