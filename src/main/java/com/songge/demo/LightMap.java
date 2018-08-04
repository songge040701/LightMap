package com.songge.demo;

import java.util.*;

/**
 * <p>
 *   低内存消耗的Map容器
 * <p/>
 *
 * @author SongGe
 * @version V1.0
 * date 2018/7/26 14:57
 */
public class LightMap<K,V> implements Map<K,V> {

    // 存储结构使用byte数组
    private byte[] nodeArray;

    // 扩容因子
    private double capacity = 0.75;

    // 容器中maxSize
    private int maxSize = 16;

    // 容器中数据个数
    private int count = 0;

    // key的数据长度
    private int keyLength;

    // value的数据长度
    private int valueLength;

    // node节点长度（key + value + 2[key长度标识，value长度标识]）
    private int nodeLength;

    /**
     * 构造方法
     * @param keyLength key长度
     * @param valueLength value长度
     */
    public LightMap(int keyLength, int valueLength) {

        this.keyLength = keyLength;
        this.valueLength = valueLength;
        this.nodeLength = this.keyLength + this.valueLength + 2;
        nodeArray = new byte[this.maxSize * nodeLength];

    }

    /**
     * 构造方法
     * @param keyLength key长度
     * @param valueLength value长度
     * @param maxSize maxSize
     */
    public LightMap(int keyLength, int valueLength, int maxSize) {

        this.maxSize = maxSize;
        this.keyLength = keyLength;
        this.valueLength = valueLength;
        this.nodeLength = this.keyLength + this.valueLength + 2;
        nodeArray = new byte[this.maxSize * nodeLength];

    }

    /**
     * 构造方法
     * @param keyLength key长度
     * @param valueLength value长度
     * @param maxSize maxSize
     * @param capacity 扩充因子
     */
    public LightMap(int keyLength, int valueLength, int maxSize, double capacity) {

        this.maxSize = maxSize;
        this.capacity = capacity;
        this.keyLength = keyLength;
        this.valueLength = valueLength;
        this.nodeLength = this.keyLength + this.valueLength + 2;
        nodeArray = new byte[this.maxSize * nodeLength];
    }

    /**
     * 添加元素方法，目前只支持最常用的String类型，K/V范型方便后期扩充类型
     * @param keyStr key长度
     * @param valueStr value长度
     * @return null
     */
    public V put(K keyStr, V valueStr) {
        if(!(keyStr instanceof String)) {
            System.out.println("put失败，暂时不支 String 以外持其他数据类型。");
            return null;
        }
        if(!(valueStr instanceof String)) {
            System.out.println("put失败，暂时不支 String 以外持其他数据类型。");
            return null;
        }

        if(((String) keyStr).getBytes().length > keyLength) {
            System.out.println("put失败，参数key长度过大" + keyLength + "。====== key: " + keyStr);
            return null;
        }

        if(((String) valueStr).getBytes().length > valueLength) {
            System.out.println("put失败，参数value长度过大" + valueLength + "。====== value: " + keyStr);
            return null;
        }

        // count数量过大直接进行rehash
        if(count > maxSize * capacity) {
            rehash();
        }

        // 添加数据失败时进行rehash
        while(!putTargetMap(nodeArray, ((String)keyStr).getBytes(), ((String)valueStr).getBytes(), maxSize)) {
            rehash();
        }

        return null;
    }

    /**
     * 向指定的map容器中添加元素
     * @param targetMap 指的的map容器
     * @param key key
     * @param value value
     * @param targetSize 指定map容器的size
     * @return 添加成功返回true，添加失败返回false（需要ReHash）
     */
    private boolean putTargetMap(byte[] targetMap, byte[] key, byte[] value, int targetSize) {

        // 获取数组中的位置
        int index = Arrays.hashCode(key)%targetSize;

        // jdk的hash算法可能产生负数，需要特殊处理
        if(index < 0) {
            index = ~index;
        }

        // index阶梯递增寻找可放置的位置
        for(; index + 10 < targetSize - 1; index = index + 10) {

            if(targetMap[index * nodeLength] == 0) {

                // index位置为空时，设置key，value
                targetMap[index * nodeLength] = (byte) (key.length & 0xff);
                System.arraycopy(key, 0, targetMap, index * nodeLength + 1, key.length);

                targetMap[index * nodeLength + keyLength + 1] = (byte) (value.length & 0xff);
                System.arraycopy(value, 0, targetMap, index * nodeLength + keyLength + 2, value.length);

                // 容器中元素个数+1
                count++;

                return true;
            } else {

                // index位置不为空时
                byte[] nodeKey = new byte[targetMap[index * nodeLength]];
                System.arraycopy(targetMap, index * nodeLength + 1, nodeKey, 0, targetMap[index * nodeLength]);

                // 判断key是否相同
                if(Arrays.equals(nodeKey, key)) {

                    // 替换value
                    targetMap[index * nodeLength + keyLength + 1] = (byte) (value.length & 0xff);
                    System.arraycopy(value, 0, targetMap, index * nodeLength + keyLength + 2, value.length);

                    return true;
                }
            }
        }

        //System.out.println(" ---- hash collisions, key: " + new String(key));

        // 未设置成功，返回false
        return false;
    }


    /**
     * 容器扩容并重新设置元素
     */
    private void rehash() {

        // 清空容器元素数
        count = 0;

        // 容器扩容
        byte[] newNodeArray = new byte[this.maxSize * 2 * nodeLength];

        // 循环将原容器中的元素设置进新容器
        for(int i = 0; i < this.maxSize; i++) {

            // 判断key是否存在
            if(nodeArray[i * nodeLength] != 0) {

                // 取出key/value
                byte[] key = new byte[nodeArray[i * nodeLength]];
                byte[] value = new byte[nodeArray[i * nodeLength + keyLength + 1]];
                System.arraycopy(nodeArray, i * nodeLength + 1, key, 0, key.length);
                System.arraycopy(nodeArray, i * nodeLength + keyLength + 2, value, 0, value.length);

                // 将元素加入到新容器
                putTargetMap(newNodeArray, key, value, maxSize * 2);
            }
        }

        // 替换容器
        nodeArray = newNodeArray;
        this.maxSize = this.maxSize * 2;

        //System.out.println(" ---- rehash end.");
    }

    /**
     * 取得元素
     * @param key key
     * @return value
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {

        // 获取数组中的位置
        int index = Arrays.hashCode(((String)key).getBytes())%this.maxSize;

        // jdk的hash算法可能产生负数，需要特殊处理
        if(index < 0) {
            index = ~index;
        }

        // 循环递增step进行匹配
        for(; index + 10 < maxSize - 1; index = index + 10) {

            // 不存在key时，直接返回null
            if(nodeArray[index * nodeLength] == 0) {
                return null;
            }

            // 出去容器中的key
            byte[] nodeKey = new byte[nodeArray[index * nodeLength]];
            System.arraycopy(nodeArray, index * nodeLength + 1, nodeKey, 0, nodeArray[index * nodeLength]);

            // 参数key与容器中的key相同判定
            if(Arrays.equals(nodeKey, ((String)key).getBytes())) {

                // 取出value
                byte[] value = new byte[nodeArray[index * nodeLength + keyLength + 1]];
                System.arraycopy(nodeArray, index * nodeLength + keyLength + 2, value, 0, nodeArray[index * nodeLength + keyLength + 1]);

                return (V) new String(value);
            }
        }

        // 容器中没有key时，返回null
        return null;
    }

    /**
     * 判断容器中是否包含key
     * @param key key
     * @return 包含返回true，不包含返回false
     */
    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * 判断容器中是否包含value
     * @param value value
     * @return 包含返回true，不包含返回false
     */
    @Override
    public boolean containsValue(Object value) {

        // 需要全量数据循环
        for(int i = 0; i < maxSize; i++) {

            // 判断是否存在value
            if(nodeArray[i*nodeLength + 1 + keyLength] != 0) {

                // 取出value
                byte[] valueByte = new byte[nodeArray[i * nodeLength + keyLength + 1]];
                System.arraycopy(nodeArray, i * nodeLength + keyLength + 2, valueByte, 0, nodeArray[i * nodeLength + keyLength + 1]);

                // 判断是否相同
                if(Arrays.equals(valueByte, ((String)value).getBytes())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        int index = Arrays.hashCode(((String)key).getBytes())%this.maxSize;

        for(; index + 10 < maxSize - 1; index = index + 10) {
            byte[] nodeKey = new byte[nodeArray[index * nodeLength]];
            System.arraycopy(nodeArray, index * nodeLength + 1, nodeKey, 0, nodeArray[index * nodeLength]);
            if(Arrays.equals(nodeKey, ((String)key).getBytes())) {
                byte[] value = new byte[nodeArray[index * nodeLength + keyLength + 1]];
                System.arraycopy(nodeArray, index * nodeLength + keyLength + 2, value, 0, nodeArray[index * nodeLength + keyLength + 1]);
                nodeArray[index * nodeLength] = 0;
                nodeArray[index * nodeLength + 1 + keyLength] = 0;
                count--;
                return (V) new String(value);
            }
        }
        return null;
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.maxSize; i++) {
            nodeArray[i * nodeLength] = 0;
            nodeArray[i * nodeLength + 1 + keyLength] = 0;
        }
    }


    @Deprecated
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        Set<K> set = new HashSet<>();
        for(int i = 0; i < maxSize; i++) {
            if(nodeArray[i*nodeLength] != 0) {
                byte[] keyByte = new byte[nodeArray[i * nodeLength]];
                System.arraycopy(nodeArray, i * nodeLength + 1, keyByte, 0, nodeArray[i * nodeLength]);
                set.add((K)new String(keyByte));
            }
        }
        return set;
    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> values() {
        Set<K> set = new HashSet<>();
        for(int i = 0; i < maxSize; i++) {
            if(nodeArray[i*nodeLength + 1 + keyLength] != 0) {
                byte[] valueByte = new byte[nodeArray[i * nodeLength + keyLength + 1]];
                System.arraycopy(nodeArray, i * nodeLength + keyLength + 2, valueByte, 0, nodeArray[i * nodeLength + keyLength + 1]);
                set.add((K)new String(valueByte));
            }
        }
        return (Collection<V>)set;
    }

    @Deprecated
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> set = new HashSet<>();
        for(int i = 0; i < maxSize; i++) {
            if(nodeArray[i*nodeLength] != 0) {
                byte[] keyByte = new byte[nodeArray[i * nodeLength]];
                System.arraycopy(nodeArray, i * nodeLength + 1, keyByte, 0, nodeArray[i * nodeLength]);
                Entry<K, V> entry = new Entry<K, V>() {
                    K key;

                    V value;

                    @Override
                    public K getKey() {
                        return key;
                    }

                    @Override
                    public V getValue() {
                        return value;
                    }

                    @Override
                    public V setValue(V value) {
                        return null;
                    }

                };
                entry.setValue(get(keyByte));
                set.add(entry);
            }
        }
        return set;
    }


    /**
     * 测试类，打印byte[]中控制的元素个数
     */
    public void printFreeDataCount() {
        int count = 0;
        for(int i = 0; i < maxSize; i++) {
            if(nodeArray[i * (this.keyLength + this.valueLength)] == 0) {
                count++;
            }
        }
        System.out.println(" ---- 空置Node数：" + count);
    }

}
