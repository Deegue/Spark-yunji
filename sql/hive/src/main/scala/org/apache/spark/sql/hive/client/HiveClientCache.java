package org.apache.spark.sql.hive.client;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description : HiveClientCache
 * @Author ： 问天(章艺钟)
 * @Date ： 5:56 PM 2019/9/6
 * @Modified :
 */
public class HiveClientCache {

    public static Map<Thread, String> userNameCache = new HashMap<>();

}
