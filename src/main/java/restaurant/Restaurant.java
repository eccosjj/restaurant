package restaurant;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Restaurant implements Delivery {
    private String shelfConfig = "shelves.json";
    private String orderConfig = "orders.json";
    private OrderManagerImpl orderManager;
    private List<CookedOrder> orders;
    public static boolean Restaurant_Is_Open = true;
    // BlockingQueue<CookedOrder> queue = new LinkedBlockingQueue<CookedOrder>(10);

    public Restaurant() {
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
        // initialize OrderManager
        try {
            this.orderManager = new OrderManagerImpl(shelfInfos);
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
        Restaurant restaurant = new Restaurant();
        restaurant.startService();
    }

    private void startService() {
        // new Monitor(this.orderManager).start();
        // Monitor monitor = new Monitor(this.orderManager);
        final CountDownLatch countKitchen = new CountDownLatch(orders.size());
        final CountDownLatch countCourier = new CountDownLatch(orders.size());
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        int receiveOrderCount = 0;
        for (CookedOrder order : orders) {
            executorService.submit(new Kitchen(this, orderManager, order, countKitchen, countCourier));
            receiveOrderCount++;
            if (receiveOrderCount % 2 == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        try {
            countKitchen.await();
            countCourier.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        executorService.shutdown();
        Restaurant_Is_Open = false;

    }

    public void notifyCourier(CookedOrder cookedOrder, CountDownLatch countCourier) {
        new Courier(cookedOrder, orderManager, countCourier).start();
    }

}
