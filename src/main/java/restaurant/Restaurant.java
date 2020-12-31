package restaurant;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import restaurant.department.Courier;
import restaurant.department.Kitchen;
import restaurant.department.Monitor;
import restaurant.department.OrderManager;
import restaurant.department.OrderManagerImpl;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public class Restaurant {
    private static Logger log = Logger.getLogger(Restaurant.class);
    private String shelfConfig = "shelves.json";
    private String orderConfig = "orders.json";
    private String config = "config.json";
    private List<CookedOrder> orders;
    private JsonObject configJsonObject;
    private OrderManager orderManager;
    // Configuration
    private int oneTimeReceive;
    private int pauseBetweenReceive;
    private int waitMin;
    private int waitMax;
    private CountDownLatch courierCountDownLatch;
    public static boolean Restaurant_Is_Openning = true;
    private Monitor monitor;

    public Restaurant() {
        try {
            Gson gson = new Gson();
            // load order.json file to List<CookedOrder> collections
            String content = readContentString(this.orderConfig);
            this.orders = (List<CookedOrder>) gson.fromJson(content, new TypeToken<List<CookedOrder>>() {
            }.getType());

            // load shelves.json file to Map<String, ShelfInfo> collections
            content = readContentString(this.shelfConfig);
            Map<String, ShelfInfo> shelfInfos = (Map<String, ShelfInfo>) gson.fromJson(content,
                    new TypeToken<Map<String, ShelfInfo>>() {
                    }.getType());

            // load config.json file which holde the normal configurations
            content = readContentString(this.config);
            this.configJsonObject = new Gson().fromJson(content, JsonObject.class);

            JsonObject orderJsonObject = this.configJsonObject.get("order").getAsJsonObject();
            this.oneTimeReceive = orderJsonObject.get("one_time_receive").getAsInt();
            this.pauseBetweenReceive = orderJsonObject.get("receive_interval").getAsInt();
            if (this.oneTimeReceive == 0) {
                log.error("one_time_receive can not be 0, please modify config.json.");
                System.exit(1);
            }
            if (this.pauseBetweenReceive < 0) {
                log.error("receive_interval can not be lower than 0, please modify config.json.");
                System.exit(1);
            }
            JsonObject courierJsonObject = this.configJsonObject.get("courier").getAsJsonObject();
            this.waitMin = courierJsonObject.get("wait_min").getAsInt();
            this.waitMax = courierJsonObject.get("wait_max").getAsInt();
            if (this.waitMin < 0 || this.waitMax < 0) {
                log.error("Nether wait_min nor wait_max can be lower than 0, please modify config.json.");
                System.exit(1);
            }
            if (this.waitMin > this.waitMax) {
                log.error("wait_max must greater than wait_min, please modify config.json.");
                System.exit(1);
            }
            this.orderManager = new OrderManagerImpl(shelfInfos,
                    this.configJsonObject.get("moving_order_time_out").getAsInt());
            JsonObject monitorJsonObject = this.configJsonObject.get("monitor").getAsJsonObject();
            if (monitorJsonObject.get("work").getAsBoolean()) {
                int workInterval = monitorJsonObject.get("work_interval").getAsInt();
                if (workInterval <= 0) {
                    log.error("work_interval must greater than 0, please modify config.json.");
                    System.exit(1);
                }
                this.monitor = new Monitor(this.orderManager, workInterval);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private String readContentString(String filePath) throws Exception {
        InputStream in = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = this.getClass().getClassLoader().getResource(filePath).openStream();
            br = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();

    }

    public static void main(String[] args) {
        log.info("The restaurant starts serving the orders.");
        Restaurant restaurant = new Restaurant();
        try {
            restaurant.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Restaurant_Is_Openning = false;
        log.info("All orders served, the restaurant is closed.");
    }

    private void startService() throws Exception {
        if (this.monitor != null) {
            this.monitor.start();
        }
        int receiveOrderCount = 0;
        courierCountDownLatch = new CountDownLatch(orders.size());
        for (CookedOrder order : orders) {
            new Kitchen(this, this.orderManager, order).start();
            receiveOrderCount++;
            if (receiveOrderCount % this.oneTimeReceive == 0) {
                try {
                    Thread.sleep(this.pauseBetweenReceive * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        this.courierCountDownLatch.await();
    }

    public void notifyCourier(CookedOrder cookedOrder) {
        new Courier(cookedOrder, this.orderManager, this.waitMin, this.waitMax, this.courierCountDownLatch).start();
    }

}
