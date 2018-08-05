package com.songge.demo;

import net.sourceforge.sizeof.SizeOf;

import java.util.HashMap;
import java.util.Map;

public class TestInteger {

    public static void main(String[] arg) {

        int count = 150000;

        System.out.println("-------------------LM test start------------------");
        LightMap<String, Integer> lightMap = new LightMap<>(20, 4);
        long light_map_put_start = System.currentTimeMillis();
        for(int i = 0; i < count;  i++) {
            lightMap.put(RandomString.getRandomString(20), i);
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

        lightMap.put("check value", 666);
        System.out.println(lightMap.get("check value"));

        System.out.println("-------------------LM test end------------------");
        System.out.println("-------------------HM test start------------------");
        Map<String, Integer> map = new HashMap<>();
        long hash_map_pu_start = System.currentTimeMillis();
        for(int i = 0; i < count;  i++) {
            map.put(RandomString.getRandomString(20), i);
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
