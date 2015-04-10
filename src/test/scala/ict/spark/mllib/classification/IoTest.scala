package ict.spark.mllib.classification

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import akka.dispatch.Foreach
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import scala.collection.mutable.HashMap
import breeze.linalg.sum

object IoTest {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Test Application").setMaster("local[4]")
    val sc = new SparkContext(conf)
    //    val authorArray: Array[String] = scala.io.Source.fromFile("configInfo/superbowl/author.name").getLines().toArray
    //    val topicwordArray: Array[String] = scala.io.Source.fromFile("configInfo/superbowl/superbowlTopicWord").getLines().toArray
    //
    //    for (author <- topicwordArray) {
    //      println(author)
    //    }

    //    val topicWordFreqFile = "configInfo/superbowl/topicWordFreqStat"
    //    val topicWordFreq = MLUtils.loadLibSVMFile(sc, topicWordFreqFile)
    //
    //    var selectedData: RDD[(Int, Int, Double)] = sc.parallelize(Array((1, 0, 0.0)), 2)
    //
    //    val tweetIds: RDD[Int] = {
    //      val x = topicWordFreq.filter(elem => elem.label == 6100 - 5929).take(1)
    //      val z = x(0).features.toArray
    //      selectedData.filter(elem => z(elem._1 - 1) > 0.0).map(e => e._1)
    //    }
    //    val topicWordStatFile = "configInfo/superbowl/tweetWordStat"
    //    val topicWordStatArray: Array[String] = scala.io.Source.fromFile(topicWordStatFile).getLines().toArray
    //    val tweetWordFreqs = HashMap[Int, Array[String]]()
    //    for (topicWordStat <- topicWordStatArray) {
    //      val arr = topicWordStat.split(" ")
    //      val tweetId = arr(0).toInt
    //      val wordFreq = arr.takeRight(arr.length - 1)
    //      tweetWordFreqs(tweetId) = wordFreq
    //    }
    //    for (keyvalue <- tweetWordFreqs) {
    //      println(keyvalue._1.toString + "xx\t" + keyvalue._2.mkString("\t"))
    //    }

    //    val testTweetWordStat: HashMap[Int, Array[String]] = {
    //      val testTweetWordStatFile = "configInfo/superbowl/tweetWordStat"
    //      val testTweetWordStatArray: Array[String] = scala.io.Source.fromFile(testTweetWordStatFile).getLines().toArray
    //      val t = HashMap[Int, Array[String]]()
    //      for (topicWordStat <- testTweetWordStatArray) {
    //        val arr = topicWordStat.split(" ")
    //        val tweetId = arr(0).toInt
    //        val wordFreq = arr.takeRight(arr.length - 1)
    //        t(tweetId) = wordFreq
    //      }
    //      t
    //    }
    //    println(testTweetWordStat(1856).mkString("\n"))
    val t = Array(1.0, 2.0, 3.0)
    println(sum(t))
  }

}