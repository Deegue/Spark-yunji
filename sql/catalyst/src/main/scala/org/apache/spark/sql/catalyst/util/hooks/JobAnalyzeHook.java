package org.apache.spark.sql.catalyst.util.hooks;

import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;

/**
 * @Description :
 * @Author ： 问天(章艺钟)
 * @Date ： 4:19 PM 2020/5/11
 * @Modified :
 */
public interface JobAnalyzeHook {
    void postAnalyze(LogicalPlan plan, String tables, String sql, String username);
}
