package com.ict.sentimentclassify.classify

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.util.MLUtils
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import ict.spark.mllib.classification.MSVMWithSGD
import com.ict.sentimentclassify.util.FileUtil
import scala.collection.immutable.Map

object AdaptiveCotrainClassifyTest {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SimpleMSVM Application").setMaster("local")
    val sc = new SparkContext(conf)
    val configFile = "config.xml"

    val adaptiveCotrainClassify: AdaptiveCotrainClassify = new AdaptiveCotrainClassify(sc, configFile)

    /**
     * test update private word function
     * wordNo2WeightMap : 1->0.23 4->0.31
     * LabeledData : 1 1:0.11 2:0.22 3:0.33 4:0.43 5:0.45
     *
     */
    //    val curTrainFile = "data/TestCase/trainFile"
    //    var curTrainData = MLUtils.loadLibSVMFile(sc, curTrainFile)
    //    val wordNo2WeightMap = HashMap[Int, Double]()
    //    wordNo2WeightMap(5932) = 0.23
    //    println(curTrainData.collect().mkString("\n"))
    //    curTrainData = adaptiveCotrainClassify.updateTrainDataTopicWordWeight(curTrainData, wordNo2WeightMap, adaptiveCotrainClassify.configInfo.getNumPublicSentimentWord())
    //    println(curTrainData.collect().mkString("\n"))

    //    /**
    //     * testcase: transfer test data to train data
    //     */
    //    val curTestFile = "data/TestCase/testFile"
    //    var curTestData = MLUtils.loadLibSVMFile(sc, curTestFile)
    //    val curIterAddedData = HashSet[Int]()
    //    curIterAddedData.add(0)
    //    curTrainData = adaptiveCotrainClassify.transfer(curTestData, curTrainData, curIterAddedData)
    //    println(curTrainData.collect().mkString("\n"))
    //    /**
    //     * testcase: predict test data
    //     */
    //    val numIterations = 100
    //    val combineModel = MSVMWithSGD.train(curTrainData, numIterations)
    //    val combinePrediction = adaptiveCotrainClassify.predictTestData(combineModel, curTestData)
    //    println("accuracy :\t" + combinePrediction._2._1 +
    //      "average precision :\t" + combinePrediction._2._2 +
    //      "average recall :\t" + combinePrediction._2._3)
    //    println("predict result\t")
    //    println(combinePrediction._1.collect().mkString("\n"))
    //
    //    /**
    //     * testcase: split feature into text feature and non-text feature
    //     */
    //    val (textFeature, nontextFeature) = adaptiveCotrainClassify.splitDataSet(curTrainData)
    //    println("text feature:")
    //    println(textFeature.collect().mkString("\n"))
    //    println("non-text feature:")
    //    println(nontextFeature.collect().mkString("\n"))
    //
    //    val textModel = MSVMWithSGD.train(textFeature, numIterations)
    //    val textPrediction = adaptiveCotrainClassify.predictTestData(textModel, curTestData)
    //    println("accuracy :\t" + textPrediction._2._1 +
    //      "average precision :\t" + textPrediction._2._2 +
    //      "average recall :\t" + textPrediction._2._3)
    //    println("text predict result\t")
    //    println(textPrediction._1.collect().mkString("\n"))
    //
    //    val nontextModel = MSVMWithSGD.train(nontextFeature, numIterations)
    //    val nontextPrediction = adaptiveCotrainClassify.predictTestData(nontextModel, curTestData)
    //    println("accuracy :\t" + nontextPrediction._2._1 +
    //      "average precision :\t" + nontextPrediction._2._2 +
    //      "average recall :\t" + nontextPrediction._2._3)
    //    println("non text predict result\t")
    //    println(nontextPrediction._1.collect().mkString("\n"))
    //    /**
    //     * testcase: select topK test data
    //     */
    //    var tweetIdAdded = new HashSet[Integer]()
    //    val selectedData = adaptiveCotrainClassify.selectTopKTestData(
    //      textPrediction._1, nontextPrediction._1, tweetIdAdded,
    //      adaptiveCotrainClassify.configInfo.getClassifyResultThreshold())
    //    val selectedPositiveData = selectedData.filter(elem => elem._1 == 0).sortBy(_._3, false, 2)
    //    val selectedNeutralData = selectedData.filter(elem => elem._1 == 1).sortBy(_._3, false, 2)
    //    val selectedNegativeData = selectedData.filter(elem => elem._1 == 2).sortBy(_._3, false, 2)
    //
    //    val topK = adaptiveCotrainClassify.configInfo.getNumTopKTestData2TrainSet()
    //    val selectedTopKPositiveData = selectedPositiveData.top(topK)
    //    val selectedTopKNeutralData = selectedNeutralData.top(topK)
    //    val selectedTopKNegativeData = selectedNegativeData.top(topK)
    //    println("selected positive data")
    //    println(selectedPositiveData.collect().mkString("\n"))
    //    println(selectedNeutralData.collect().mkString("\n"))
    //    println(selectedNegativeData.collect().mkString("\n"))
    //    if (selectedTopKPositiveData.size == 0 && selectedTopKNeutralData.size == 0 && selectedTopKNegativeData.size == 0) {
    //      println("no more data is selected")
    //    }
    //    /**
    //     * testcase: select topK topic-word
    //     */
    //    val arr = Array((10, 0, 0.97), (3, 0, 0.9)) //tweetId, classify Class, classify score
    //    val selectedData = sc.parallelize(arr, 2)
    //
    //    //    println(topicWordFreq.collect().mkString("\n"))
    //    val (topKPositiveWord, topKNeutralWord, topKNegativeWord) =
    //      adaptiveCotrainClassify.selectTopKTopicWord(selectedData,
    //        adaptiveCotrainClassify.configInfo.getNumPublicSentimentWord(),
    //        adaptiveCotrainClassify.configInfo.getNumTopKPrivateWord())
    //    println("selected positive word")
    //    for (pos <- 0 to topKPositiveWord.size - 1) {
    //      println(topKPositiveWord(pos)._1 + "\t" + topKPositiveWord(pos)._2)
    //    }

  }

}