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
    private double capacity = 0.8;

    // 容器中maxSize
    private int maxSize = 16;

    // 容器中数据个数
    private int count = 0;

    // key的数据长度
    private int keyLength;

    // value的数据长度
    private int valueLength;

    // node节点长度（key + value + 2[key长度标识，value长度标识] + 寄居标识 + 4[hashCode] + 4[下一个节点位置]）
    private int nodeLength;

    // 寻找节点跳跃步长
    private int step = 5;

    /**
     * 构造方法
     * @param keyLength key长度
     * @param valueLength value长度
     */
    public LightMap(int keyLength, int valueLength) {

        this.keyLength = keyLength;
        this.valueLength = valueLength;
        this.nodeLength = this.keyLength + this.valueLength + 2 + 1 + 4 + 4;
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
        this.nodeLength = this.keyLength + this.valueLength + 2 + 1 + 4 + 4;
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
        this.nodeLength = this.keyLength + this.valueLength + 2 + 1 + 4 + 4;
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

        int hashCode = Arrays.hashCode(key);

        // 获取数组中的位置
        int index = Arrays.hashCode(key)%targetSize;

        // jdk的hash算法可能产生负数，需要特殊处理
        if(index < 0) {
            index = ~index;
        }

        // index处于数组后部时，往前跳跃寻找节点
        int step = this.step;
        if(index > targetSize/2) {
            step = - step;
        }

        // 添加寄居标识
        if(targetMap[index * nodeLength] != 0) {
            int targetHashCode = (targetMap[index * nodeLength + nodeLength - 8] & 0xff) |
                    (targetMap[index * nodeLength + nodeLength - 7] & 0xff << 8) |
                    (targetMap[index * nodeLength + nodeLength - 6] & 0xff << 16) |
                    (targetMap[index * nodeLength + nodeLength - 5] & 0xff << 24);

            if (targetHashCode != hashCode) {
                // 当该位置真正的hashcode曾经来过时，讲寄居标识设置为1（初始为0）
                targetMap[index * nodeLength + nodeLength - 9] = 1;
            }
        }

        for(;;) {
            if (targetMap[index * nodeLength] == 0) {

                // index位置为空时，设置key，value
                targetMap[index * nodeLength] = (byte) (key.length & 0xff);
                System.arraycopy(key, 0, targetMap, index * nodeLength + 1, key.length);

                targetMap[index * nodeLength + keyLength + 1] = (byte) (value.length & 0xff);
                System.arraycopy(value, 0, targetMap, index * nodeLength + keyLength + 2, value.length);

                // 设置hashcode
                targetMap[index * nodeLength + nodeLength - 8] = (byte) (hashCode & 0xff);
                targetMap[index * nodeLength + nodeLength - 7] = (byte) (hashCode >> 8 & 0xff);
                targetMap[index * nodeLength + nodeLength - 6] = (byte) (hashCode >> 16 & 0xff);
                targetMap[index * nodeLength + nodeLength - 5] = (byte) (hashCode >> 24 & 0xff);

                // 容器中元素个数+1
                count++;

                return true;
            } else {

                for (;;) {
                    // 取出容器中该位置数据的HashCode
                    int targetHashCode = (targetMap[index * nodeLength + nodeLength - 8] & 0xff) |
                            (targetMap[index * nodeLength + nodeLength - 7] & 0xff << 8) |
                            (targetMap[index * nodeLength + nodeLength - 6] & 0xff << 16) |
                            (targetMap[index * nodeLength + nodeLength - 5] & 0xff << 24);

                    if (targetHashCode == hashCode) {

                        // 取出当前key
                        byte[] nodeKey = new byte[targetMap[index * nodeLength]];
                        System.arraycopy(targetMap, index * nodeLength + 1, nodeKey, 0, targetMap[index * nodeLength]);

                        // 判断key是否相同
                        if (Arrays.equals(nodeKey, key)) {

                            // 替换value
                            targetMap[index * nodeLength + keyLength + 1] = (byte) (value.length & 0xff);
                            System.arraycopy(value, 0, targetMap, index * nodeLength + keyLength + 2, value.length);

                            return true;
                        } else {

                            // key不相同时需要在逻辑链表中向下寻找
                            int next = (targetMap[index * nodeLength + nodeLength - 4] & 0xff) |
                                    (targetMap[index * nodeLength + nodeLength - 3] & 0xff << 8) |
                                    (targetMap[index * nodeLength + nodeLength - 2] & 0xff << 16) |
                                    (targetMap[index * nodeLength + nodeLength - 1] & 0xff << 24);

                            // 初始化时byte数组中数据都为0，但数组下标0是有意义的，为了不进行特殊的初始化增加如下判断
                            // 取出数据为0时，next为-1(标识没有进行设置)
                            // 取出数组下标为-1时，说明已经设定了值，意义为0
                            if (next == 0) {
                                next = -1;
                            } else if (next == -1) {
                                next = 0;
                            }

                            // 不等于0时，存在下一个位置
                            if (next != -1) {
                                index = next;
                            } else {
                                int parentNodeIndex = index;
                                for (index = index + step; index < targetSize - 1 || index >= 0; index = index + step) {

                                    if (targetMap[index * nodeLength] == 0) {

                                        // index位置为空时，设置key，value
                                        targetMap[index * nodeLength] = (byte) (key.length & 0xff);
                                        System.arraycopy(key, 0, targetMap, index * nodeLength + 1, key.length);

                                        targetMap[index * nodeLength + keyLength + 1] = (byte) (value.length & 0xff);
                                        System.arraycopy(value, 0, targetMap, index * nodeLength + keyLength + 2, value.length);

                                        // 设置hashcode
                                        targetMap[index * nodeLength + nodeLength - 8] = (byte) (hashCode & 0xff);
                                        targetMap[index * nodeLength + nodeLength - 7] = (byte) (hashCode >> 8 & 0xff);
                                        targetMap[index * nodeLength + nodeLength - 6] = (byte) (hashCode >> 16 & 0xff);
                                        targetMap[index * nodeLength + nodeLength - 5] = (byte) (hashCode >> 24 & 0xff);

                                        // 由于byte数组中初始数据都为0，借用-1来标注数组下标为0的元素
                                        if (index == 0) {
                                            index = -1;
                                        }
                                        // 为父节点设置next标识
                                        targetMap[parentNodeIndex * nodeLength + nodeLength - 4] = (byte) (index & 0xff);
                                        targetMap[parentNodeIndex * nodeLength + nodeLength - 3] = (byte) (index >> 8 & 0xff);
                                        targetMap[parentNodeIndex * nodeLength + nodeLength - 2] = (byte) (index >> 16 & 0xff);
                                        targetMap[parentNodeIndex * nodeLength + nodeLength - 1] = (byte) (index >> 24 & 0xff);

                                        // 容器中元素个数+1
                                        count++;

                                        return true;
                                    }
                                }

                                // 到达数组尽头都未找到元素时，返回false进行rehash
                                //System.out.println(" ---- hash collisions, key: " + new String(key));
                                return false;
                            }
                        }
                    } else {

                        // 传入参数key的hashCode位置已经被占用了，只能寻找下一个位置
                        // System.out.println("index === " + index + " step === " +step + " maxSize === " + targetSize);
                        index = index + step;
                        if(index > targetSize - 1 || index < 0) {
                            //System.out.println(" ---- array to end");
                            return false;
                        }

                        break;
                    }
                } // 无限循1

            }

        } // 无限循环2


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

        byte[] keyByte = ((String)key).getBytes();

        int hashCode = Arrays.hashCode(keyByte);

        // 获取数组中的位置
        int index = hashCode%this.maxSize;

        // jdk的hash算法可能产生负数，需要特殊处理
        if(index < 0) {
            index = ~index;
        }

        // index处于数组后部时，往前跳跃寻找节点
        if(index > maxSize/2) {
            step = - step;
        }

        for(;;) {
            if (nodeArray[index * nodeLength] == 0) {
                return null;
            } else {

                for (;;) {
                    // 取出容器中该位置数据的HashCode
                    int targetHashCode = (nodeArray[index * nodeLength + nodeLength - 8] & 0xff) |
                            ((nodeArray[index * nodeLength + nodeLength - 7] & 0xff) << 8) |
                            ((nodeArray[index * nodeLength + nodeLength - 6] & 0xff) << 16) |
                            ((nodeArray[index * nodeLength + nodeLength - 5] & 0xff) << 24);

                    if (targetHashCode == hashCode) {

                        boolean isSameKey = true;
                        if (nodeArray[index * nodeLength] == keyByte.length) {
                            for(int i = 0; i < nodeArray[index * nodeLength]; i++) {
                                if(nodeArray[index * nodeLength + 1 + i] != keyByte[i]) {
                                    isSameKey = false;
                                    break;
                                }
                            }
                        } else {
                            isSameKey =false;
                        }

                        // 判断key是否相同
                        if (isSameKey) {

                            byte[] value = new byte[nodeArray[index * nodeLength + keyLength + 1]];
                            System.arraycopy(nodeArray, index * nodeLength + keyLength + 2, value, 0, nodeArray[index * nodeLength + keyLength + 1]);

                            return (V) new String(value);

                        } else {

                            // key不相同时需要在逻辑链表中向下寻找
                            int next = (nodeArray[index * nodeLength + nodeLength - 4] & 0xff) |
                                    (nodeArray[index * nodeLength + nodeLength - 3] & 0xff << 8) |
                                    (nodeArray[index * nodeLength + nodeLength - 2] & 0xff << 16) |
                                    (nodeArray[index * nodeLength + nodeLength - 1] & 0xff << 24);

                            // 初始化时byte数组中数据都为0，但数组下标0是有意义的，为了不进行特殊的初始化增加如下判断
                            // 取出数据为0时，next为-1(标识没有进行设置)
                            // 取出数组下标为-1时，说明已经设定了值，意义为0
                            if (next == 0) {
                                next = -1;
                            } else if (next == -1) {
                                next = 0;
                            }

                            // 不等于0时，存在下一个位置
                            if (next != -1) {
                                index = next;
                            } else {
                                return null;
                            }
                        }
                    } else {

                        // 该位置真正的hashcode值并未来过时，直接返回null
                        if(nodeArray[index * nodeLength + nodeLength - 9] != 1) {
                            return null;
                        }

                        // 传入参数key的hashCode位置已经被占用了，只能寻找下一个位置
                        index = index + step;

                        if(index > maxSize - 1 || index < 0) {
                            return null;
                        }

                        break;
                    }
                } // 无限循1

            }

        } // 无限循环2
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
        nodeArray = null;
        maxSize = 16;
        count = 0;
        capacity = 0.75;
    }


    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends K, ? extends V> m) {
        Set<K> set = (Set<K>) m.keySet();
        for (K node : set) {
            this.put(node, m.get(node));
        }
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
    @SuppressWarnings("unchecked")
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> set = new HashSet<>();
        for(int i = 0; i < maxSize; i++) {
            if(nodeArray[i*nodeLength] != 0) {
                byte[] keyByte = new byte[nodeArray[i * nodeLength]];
                System.arraycopy(nodeArray, i * nodeLength + 1, keyByte, 0, nodeArray[i * nodeLength]);
                LightEntry entry = new LightEntry();
                entry.setKey((K)new String(keyByte));
                entry.setValue(get(keyByte));
                set.add(entry);
            }
        }
        return set;
    }


    /**
     * 测试类，打印byte[]中控制的元素个数
     */
    void printFreeDataCount() {
        int count = 0;
        for(int i = 0; i < maxSize; i++) {
            if(nodeArray[i * nodeLength] == 0) {
                count++;
            }
        }
        System.out.println("  === 总共Node数：" + maxSize + "\r\n  === 空置Node数：" + count + "\r\n  === 空置率：" + count*100/maxSize + " %");
    }

    class LightEntry implements Entry<K,V> {

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
            return this.value = value;
        }

        void setKey(K key) {this.key = key;}
    }
}
