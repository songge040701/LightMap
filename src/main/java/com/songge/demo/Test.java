package com.songge.demo;

import net.sourceforge.sizeof.SizeOf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 *   测试类
 * <p/>
 *
 * @author SongGe
 * @version V1.0
 * date 2018/7/26 14:57
 */
public class Test {

    public static void main(String arg[]) {

        int count = 4000000;

        System.out.println("-------------------LM test start------------------");
        LightMap<String, String> lightMap = new LightMap<>(20, 20);
        long light_map_put_start = System.currentTimeMillis();
        for(int i = 0; i < count;  i++) {
            lightMap.put(RandomString.getRandomString(20), RandomString.getRandomString(20));
        }
        long light_map_put_end = System.currentTimeMillis();
        System.out.println("LightMap put用时：" + (light_map_put_end - light_map_put_start) + " ms。");
        System.out.println("LightMap JVM堆内存消耗：" + SizeOf.deepSizeOf(lightMap) + " byte。");

        long light_map_get_start = System.currentTimeMillis();
        for(int i = 0; i < count;  i++) {
            lightMap.get(RandomString.getRandomString(20));
        }
        long light_map_get_end = System.currentTimeMillis();
        System.out.println("LightMap get用时：" + (light_map_get_end - light_map_get_start) + " ms。");

        lightMap.printFreeDataCount();

        System.out.println("-------------------LM test end------------------");
        System.out.println("-------------------HM test start------------------");
        Map<String, String> map = new HashMap<>();
        long hash_map_pu_start = System.currentTimeMillis();
        for(int i = 0; i < count;  i++) {
            map.put(RandomString.getRandomString(20), RandomString.getRandomString(20));
        }
        long hash_map_pu_end = System.currentTimeMillis();
        System.out.println("HashMap put用时：" + (hash_map_pu_end - hash_map_pu_start) + " ms。");
        System.out.println("HashMap JVM堆内存消耗：" + SizeOf.deepSizeOf(map) + " byte。");


        long hash_map_get_start = System.currentTimeMillis();
        for(int i = 0; i < count;  i++) {
            map.get(RandomString.getRandomString(20));
        }
        long hash_map_get_end = System.currentTimeMillis();
        System.out.println("HashMap get用时：" + (hash_map_get_end - hash_map_get_start) + " ms。");

        System.out.println("-------------------HM test end------------------");


    }

}
