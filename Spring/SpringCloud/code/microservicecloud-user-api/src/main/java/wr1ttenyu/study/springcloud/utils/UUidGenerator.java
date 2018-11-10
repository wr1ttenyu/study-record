package wr1ttenyu.study.springcloud.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UUidGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UUidGenerator.class);

    /**
     * 停止id生产阈值
     */
    private static final int MAX_ID_CONTAINER_SIZE = 5000;

    /**
     * 触发id生产阈值
     */
    private static final int MIN_ID_CONTAINER_SIZE = 1000;

    /**
     * 生产者工作状态
     */
    private static AtomicBoolean CREATING = new AtomicBoolean(false);

    /**
     * id生产者的数量
     */
    private static final int DEFAULT_PRODUCER_SIZE = 5;

    /**
     * id自增器
     */
    private static AtomicInteger COUNT = new AtomicInteger(0);

    private static ConcurrentSkipListSet<String> ID_SUFFIX_CONTAINER = new ConcurrentSkipListSet();

    private static ArrayList<idGeneratorProducer> ID_SUFFIX_GENERATOR_PRODUCERS = new ArrayList<>(DEFAULT_PRODUCER_SIZE);

    private static ExecutorService executorService = Executors.newFixedThreadPool(DEFAULT_PRODUCER_SIZE);

    /**
     * 本机ip后两段
     */
    private static String formatIp;

    static {
        for (int i = 0; i < DEFAULT_PRODUCER_SIZE; i++) {
            ID_SUFFIX_GENERATOR_PRODUCERS.add(new idGeneratorProducer());
        }

        try {
            formatIp = formatIp();
        } catch (Exception e) {
            LOGGER.error("uuid生成器,本机ip初始化失败,请检查!");
        }

        for (int i = 0; i < DEFAULT_PRODUCER_SIZE; i++) {
            executorService.execute(ID_SUFFIX_GENERATOR_PRODUCERS.get(i));
        }
    }

    private static String generatorId() {
        LOGGER.info("开始获取id...");
        String suffix = ID_SUFFIX_CONTAINER.pollFirst();
        if (ID_SUFFIX_CONTAINER.size() < MIN_ID_CONTAINER_SIZE) {
            LOGGER.info("id存量小于阈值,开始生产...");
            for (int i = 0; i < DEFAULT_PRODUCER_SIZE; i++) {
                idGeneratorProducer idGeneratorProducer = ID_SUFFIX_GENERATOR_PRODUCERS.get(i);
                synchronized (idGeneratorProducer) {
                    ID_SUFFIX_GENERATOR_PRODUCERS.get(i).notify();
                }
            }
        }
        if (StringUtils.isEmpty(suffix)) {
            LOGGER.info("存量为0,直接生产获取后缀...");
            suffix = generateSuffix();
        }
        return getCurrentTimeFormat() + suffix;
    }

    private static String getCurrentTimeFormat() {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(System.currentTimeMillis()));
    }

    // yyyyMMddHHmmssSSS  17 +
    // 机器ip 后两段 确保集群下  id不重复
    private static String generateSuffix() {
        //  ip   + count
        //  6    +   9
        int count = COUNT.addAndGet(1);
        return formatIp + new StringBuilder("000000000").replace(9 - String.valueOf(count).length(), 9,
                String.valueOf(count));
    }

    public static String formatIp() throws Exception {

        byte[] ip_bytes = InetAddress.getLocalHost().getAddress();

        StringBuilder builder = new StringBuilder("");

        int data = Math.abs(ip_bytes[2]);

        builder.append(new StringBuilder("000").replace(3 - String.valueOf(data).length(), 3,
                String.valueOf(data)));

        data = Math.abs(ip_bytes[3]);

        builder.append(new StringBuilder("000").replace(3 - String.valueOf(data).length(), 3,
                String.valueOf(data)));

        return builder.toString();
    }

    private static class idGeneratorProducer implements Runnable {
        @Override
        public void run() {
            LOGGER.info(Thread.currentThread().getName() + ",开始工作生产id后缀...");
            synchronized (this) {
                while (ID_SUFFIX_CONTAINER.size() < MAX_ID_CONTAINER_SIZE) {
                    ID_SUFFIX_CONTAINER.add(generateSuffix());
                }
                LOGGER.info(Thread.currentThread().getName() + ",当前后缀数量:{}", ID_SUFFIX_CONTAINER.size());
                if(ID_SUFFIX_CONTAINER.size() > MAX_ID_CONTAINER_SIZE) {
                    LOGGER.info(Thread.currentThread().getName() + "停止工作生产id后缀...当前后缀数量:{}", ID_SUFFIX_CONTAINER.size());
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        LOGGER.error("id生产者wait()异常,error info:{}", e.getLocalizedMessage());
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        UUidGenerator.generatorId();
        try {
            Thread.currentThread().sleep(10000000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
