/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.hive.thriftserver

import java.util

import org.apache.commons.lang3.StringUtils

import org.apache.spark.internal.Logging
import org.apache.spark.scheduler._

class SparkHiveLog extends SparkListener with Logging{

  private class SparkStageInfo(val stageId: Int, val totalTask: Int) {

    var completedTask = 0

    var status = "Running"
  }

  private var jobGroupMap : Map[String, Set[Int]] = Map[String, Set[Int]]()
  private var jobListMap : Map[Int, Seq[Int]] = Map[Int, Seq[Int]]()
  private var stageMap: Map[Int, SparkStageInfo] = Map[Int, SparkStageInfo]()


  private var taskCPUs = 1

  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    val groupId = jobStart.properties.getProperty("spark.jobGroup.id")
    taskCPUs = jobStart.properties.getProperty("spark.task.cpus", "1").toInt
    logInfo("AAAAAA onJobStart")

    jobListMap += (jobStart.jobId -> jobStart.stageIds)
    if (jobGroupMap.contains(groupId)) {
      jobGroupMap(groupId) + jobStart.jobId
    } else {
      jobGroupMap += (groupId -> Set(jobStart.jobId))
    }
  }

  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit = {
    val stageId = stageSubmitted.stageInfo.stageId
    val numTasks = stageSubmitted.stageInfo.numTasks

    val stageInfo = new SparkStageInfo(stageId, numTasks)
    stageMap = stageMap + (stageId -> stageInfo)
    logInfo("AAAAAA onStageSubmitted, stageMap:" + stageMap)
  }


  override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = {
    val stageId = stageCompleted.stageInfo.stageId
    logInfo("AAAAAA onStageCompleted, onStageCompleted:" + stageMap)
    if (stageMap.contains(stageId)) {
      val stageInfo = stageMap.get(stageId).get
      stageInfo.status = "Completed"
    }
  }


  override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = {
    val stageId = taskEnd.stageId

    if (stageMap.contains(stageId)) {
      val stageInfo = stageMap.get(stageId).get
      if (taskEnd.taskInfo.successful) {
        val completeTask = stageInfo.completedTask
        stageInfo.completedTask = completeTask + 1
      }
    }
    logInfo("AAAAAA onTaskEnd, stageId:" + stageId)
  }

  private def getStageInfo(stageId: Int) : String = {
    val sb = new StringBuffer("stage")
    if (!stageMap.contains(stageId)) {
      return ""
    }
    val stageInfo = stageMap.get(stageId).get
    logInfo("AAAAAA getStageInfo, stageInfo" + stageInfo)
    sb.append(stageId).append("(").append(stageInfo.completedTask).append("/")
    sb.append(stageInfo.totalTask).append(")")
    sb.toString
  }

  private def getJobInfo(jobId: Int): String = {
    val sb = new StringBuffer("Job")
    val map = new util.TreeMap[Int, String]()
    logInfo("AAAAAA getJobInfo, jobId" + jobId)

    if (jobListMap.contains(jobId)) {
      for (stageId <- jobListMap.get(jobId).get) {
        val stageInfo = getStageInfo(stageId)
        if (stageInfo != null && stageInfo != "") {
          map.put(stageId, getStageInfo(stageId))
        }
      }
    }
    sb.append(jobId).append("/").append(formatString(map, "/"))
    sb.toString
  }

  def getLogInfo(groupId: String) : String = {
    val sb = new StringBuffer()
    logInfo("AAAAAA getLogInfo, groupId" + groupId)

    if (jobGroupMap.contains(groupId)) {
      val map = new util.TreeMap[Int, String]()
      val iter = jobGroupMap(groupId).iterator
      while (iter.hasNext) {
        val jobId = iter.next()
        map.put(jobId, getJobInfo(jobId))
      }
      sb.append(formatString(map, ";"))
    }
    sb.toString
  }

  private def formatString(map: util.TreeMap[Int, String], split: String): String = {
    val list = new util.ArrayList[String]()
    val iter = map.keySet().iterator()
    while (iter.hasNext) {
      list.add(map.get(iter.next()))
    }
    StringUtils.join(list, split)
  }


  def clearCaches(groupId: String) : Unit = {
    logInfo("AAAAAA clearCaches, groupId" + groupId)

    if (jobGroupMap.contains(groupId)) {
      val iter = jobGroupMap.get(groupId).get.iterator
      while (iter.hasNext) {
        val jobId = iter.next()
        val iter1 = jobListMap.get(jobId).get.iterator
        while (iter1.hasNext) {
          stageMap = stageMap - (iter1.next())
        }
        jobListMap = jobListMap - (jobId)
      }
      jobGroupMap = jobGroupMap - (groupId)
    }
  }

}

object SparkHiveLog {

  private var sparkHiveLog : SparkHiveLog = null

  def getSparkHiveLog() : SparkHiveLog = {
    if (sparkHiveLog == null) {
      sparkHiveLog = new SparkHiveLog()
    }
    sparkHiveLog
  }
}
