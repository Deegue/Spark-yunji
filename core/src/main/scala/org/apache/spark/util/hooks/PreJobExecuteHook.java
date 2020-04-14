package org.apache.spark.util.hooks;

/**
 * @Description :
 * @Author ： 问天(章艺钟)
 * @Date ： 4:19 PM 2020/4/1
 * @Modified :
 */
public interface PreJobExecuteHook {
    void execute(String queue);
}
