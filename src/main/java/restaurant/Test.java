package restaurant;

import java.util.HashMap;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception {
        // String json =
        // "{'3':{'id':'3','name':'QIAOZHI','age':25,'address':'山西太原'},'2':{'id':'2','name':'QIAOZHI','age':25,'address':'山西太原'},'1':{'id':'1','name':'QIAOZHI','age':25,'address':'山西太原'}}";
        // Map<String, User> map = new HashMap<String, User>();
        // Gson gson = new Gson();
        // map = (Map<String, User>) gson.fromJson(json, new TypeToken<Map<String,
        // User>>() {
        // }.getType());
        // System.out.println(map);
        // // {3={id=3, name=QIAOZHI, age=25.0, address=山西太原}, 2={id=2, name=QIAOZHI,
        // // age=25.0, address=山西太原}, 1={id=1, name=QIAOZHI, age=25.0, address=山西太原}}
        // User user = map.get("1");
        // System.out.println(user);
        // // {id=1, name=QIAOZHI, age=25.0, address=山西太原}
        // File shelfConfig = new File("user.json");
        // Long shelfConfigLength = shelfConfig.length();
        // byte[] fileContent = new byte[shelfConfigLength.intValue()];
        // FileInputStream in = new FileInputStream(shelfConfig);
        // in.read(fileContent);
        // Map<String, User> map = new HashMap<String, User>();
        // Gson gson = new Gson();
        // map = (Map<String, User>) gson.fromJson(new String(fileContent), new
        // TypeToken<Map<String, User>>() {
        // }.getType());
        // System.out.println(map);
        //
        // // float f = 2.546546546454564f;
        //
        // long f1 = System.currentTimeMillis();
        // Thread.sleep(3000);
        // long f2 = System.currentTimeMillis();
        // float result = (f2 - f1) / 1000;
        // System.out.println(result);
        // BigDecimal b = new BigDecimal(result);
        // float f = b.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
        // System.out.println(f);

        Map<Integer, User> a = new HashMap();
        for (int i = 0; i < 3; i++) {
            a.put(i, new User(i + "", i + "", i, ""));
        }
        User u = (User) a.get(1);
        u = setName(u);
        for (int k : a.keySet()) {
            System.out.println(a.get(k).toString());
        }

    }

    private static User setName(User a) {
        a.setName("asd");
        return a;
    }

}
