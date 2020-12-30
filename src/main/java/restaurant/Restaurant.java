package restaurant;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import restaurant.department.Courier;
import restaurant.department.Kitchen;
import restaurant.department.OrderManagerImpl;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public class Restaurant {
    private static Logger log = Logger.getLogger(Restaurant.class);
    private String shelfConfig = "shelves.json";
    private String orderConfig = "orders.json";
    private String config = "config.json";
    private OrderManagerImpl orderManager;
    private List<CookedOrder> orders;
    private JsonObject configJsonObject;
    // BlockingQueue<CookedOrder> queue = new LinkedBlockingQueue<CookedOrder>(10);

    private int oneTimeReceive;
    private int pauseBetweenReceive;
    private int waitMin;
    private int waitMax;

    public Restaurant() {
        Gson gson = new Gson();
        // load order.json file to List<CookedOrder> collections
        String content = readContentString(this.orderConfig);
        this.orders = (List<CookedOrder>) gson.fromJson(content, new TypeToken<List<CookedOrder>>() {
        }.getType());

        // load config.json file which holde the normal configurations
        content = readContentString(this.config);
        this.configJsonObject = new Gson().fromJson(content, JsonObject.class);

        // load shelves.json file to Map<String, ShelfInfo> collections
        content = readContentString(this.shelfConfig);
        Map<String, ShelfInfo> shelfInfos = (Map<String, ShelfInfo>) gson.fromJson(content,
                new TypeToken<Map<String, ShelfInfo>>() {
                }.getType());
        // initialize OrderManager
        try {
            this.orderManager = new OrderManagerImpl(shelfInfos,
                    this.configJsonObject.get("moving_order_time_out").getAsInt());
        } catch (Exception e) {
            System.out.print(e.getMessage());
            System.exit(1);
        }

    }

    private static String readContentString(String filePath) {
        FileInputStream in = null;
        Long shelfConfigLength = null;
        byte[] fileContent = null;
        try {
            File shelfConfig = new File(filePath);
            shelfConfigLength = shelfConfig.length();
            fileContent = new byte[shelfConfigLength.intValue()];
            in = new FileInputStream(shelfConfig);
            in.read(fileContent);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new String(fileContent);
    }

    public static void main(String[] args) {
        log.trace("start");
        Restaurant restaurant = new Restaurant();
        try {
            restaurant.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startService() throws Exception {
        JsonObject orderJsonObject = this.configJsonObject.get("order").getAsJsonObject();
        this.oneTimeReceive = orderJsonObject.get("one_time_receive").getAsInt();
        this.pauseBetweenReceive = orderJsonObject.get("pause_between_receive").getAsInt();
        if (this.oneTimeReceive == 0) {
            throw new Exception("one_time_receive can not be 0, please modify config.json.");
        }
        if (this.pauseBetweenReceive < 0) {
            throw new Exception("pause_between_receive can not be lower than 0, please modify config.json.");
        }
        JsonObject courierJsonObject = this.configJsonObject.get("courier").getAsJsonObject();
        this.waitMin = courierJsonObject.get("wait_min").getAsInt();
        this.waitMax = courierJsonObject.get("wait_max").getAsInt();
        if (this.waitMin < 0 || this.waitMax < 0) {
            throw new Exception("Nether wait_min nor wait_max can be lower than 0, please modify config.json.");
        }
        if (this.waitMin > this.waitMax) {
            throw new Exception("wait_max must greater wait_min, please modify config.json.");
        }
        int receiveOrderCount = 0;
        for (CookedOrder order : orders) {
            new Kitchen(this, orderManager, order).start();
            receiveOrderCount++;
            // if (receiveOrderCount % this.oneTimeReceive == 0) {
            // try {
            // Thread.sleep(this.pauseBetweenReceive * 1000);
            // } catch (InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // }
        }
    }

    public void notifyCourier(CookedOrder cookedOrder) {
        new Courier(cookedOrder, this.orderManager, this.waitMin, this.waitMax).start();
    }

}
