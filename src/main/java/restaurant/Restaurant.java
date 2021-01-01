package restaurant;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import restaurant.department.Courier;
import restaurant.department.Kitchen;
import restaurant.department.OrderManager;
import restaurant.department.OrderManagerImpl;
import restaurant.pojo.CookedOrder;
import restaurant.pojo.ShelfInfo;

public class Restaurant {
    static Logger log = Logger.getLogger(Restaurant.class);
    private String shelfConfig = "shelves.json";
    private String orderConfig = "orders.json";
    private String config = "config.json";
    public List<CookedOrder> orders;
    public JsonObject configJsonObject;
    public OrderManager orderManager;
    // Configuration
    public int oneTimeReceive;
    public int pauseBetweenReceive;
    public int waitMin;
    public int waitMax;
    public int movingOrderTimeOut;
    public CountDownLatch courierCountDownLatch;
    public ListeningExecutorService listeningExecutorService;
    public ExecutorService courierExecutorService;

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
            if (this.oneTimeReceive <= 0) {
                log.error("one_time_receive must be greater than 0, please modify config.json.");
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
            this.orderManager = new OrderManagerImpl(shelfInfos);
            this.movingOrderTimeOut = this.configJsonObject.get("moving_order_time_out").getAsInt();
            if (movingOrderTimeOut < 0) {
                log.error("moving_order_time_out must greater than 0, please modify config.json.");
                System.exit(1);
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
        log.info("All orders served, the restaurant is closed.");
    }

    public void startService() throws InterruptedException {
        int receiveOrderCount = 0;
        courierCountDownLatch = new CountDownLatch(orders.size());
        listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(orders.size()));
        courierExecutorService = Executors.newFixedThreadPool(orders.size());
        for (CookedOrder order : orders) {
            ListenableFuture<CookedOrder> listenableFuture = listeningExecutorService
                    .submit(new Kitchen(orderManager, order, movingOrderTimeOut));
            Futures.addCallback(listenableFuture, new FutureCallback<CookedOrder>() {
                @Override
                public void onSuccess(CookedOrder order) {
                    courierExecutorService
                            .submit(new Courier(order, orderManager, waitMin, waitMax, courierCountDownLatch));

                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, listeningExecutorService);

            receiveOrderCount++;
            if (receiveOrderCount % this.oneTimeReceive == 0) {
                Thread.sleep(this.pauseBetweenReceive * 1000);
            }
        }
        this.courierCountDownLatch.await();
        listeningExecutorService.shutdown();
        courierExecutorService.shutdown();
    }

}
