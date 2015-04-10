package com.ict.sentimentclassify.classify

import scala.util.control.Breaks._
import org.apache.log4j.Logger
import com.ict.sentimentclassify.config.ConfigReader
import com.ict.sentimentclassify.util.FileUtil
import java.text.SimpleDateFormat
import scala.collection.mutable.HashMap
import java.util.Date
import scala.collection.mutable.HashSet
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import ict.spark.mllib.classification.MSVMWithSGD
import ict.spark.mllib.classification.MSVMModel
import breeze.linalg.min
import com.ict.sentimentclassify.util.AppendContent2File
import java.util.concurrent.TimeUnit
import breeze.linalg.sum

class AdaptiveCotrainClassify(@transient sc: SparkContext,
  val configFileName: String, numIterations: Int = 100, numSlices: Int = 2) extends Serializable {

  val logger = Logger.getLogger(AdaptiveCotrainClassify.toString())
  val configReader = new ConfigReader(configFileName)
  val configInfo = configReader.ReadConfig()

  val topicwordArray: Array[String] = scala.io.Source.fromFile(configInfo.getTopicwordFile()).getLines().toArray
  val positiveTopicWordFile = configInfo.getPositiveTopicWordWeightFile()
  val neutralTopicWordFile = configInfo.getNeutraltopicwordweightfile()
  val negativeTopicWordFile = configInfo.getNegativetopicwordweightfile()

  val testTweetWordStat: HashMap[Int, Array[String]] = {
    val testTweetWordStatFile = configInfo.getTweetWordStatFile()
    val testTweetWordStatArray: Array[String] = scala.io.Source.fromFile(testTweetWordStatFile).getLines().toArray
    val t = HashMap[Int, Array[String]]()
    for (topicWordStat <- testTweetWordStatArray) {
      val arr = topicWordStat.split(" ")
      val tweetId = arr(0).toInt
      val wordFreq = arr.takeRight(arr.length - 1)
      t(tweetId) = wordFreq
    }
    t
  }
  val trainTweetWordStat: HashMap[Int, Array[String]] = {
    val trainTweetWordStatFile = configInfo.getTrainTweetWordStatFile()
    val trainTweetWordStatArray: Array[String] = scala.io.Source.fromFile(trainTweetWordStatFile).getLines().toArray
    val t = HashMap[Int, Array[String]]()
    for (topicWordStat <- trainTweetWordStatArray) {
      val arr = topicWordStat.split(" ")
      val tweetId = arr(0).toInt
      val wordFreq = arr.takeRight(arr.length - 1)
      t(tweetId) = wordFreq
    }
    t
  }

  val testTopicwordTweetFreqStat = {
    val testTopicWordFreqStatFile = configInfo.getTopicWordFreqStatFile()
    val testTopicWordFreqStatArray: Array[String] = scala.io.Source.fromFile(testTopicWordFreqStatFile).getLines().toArray
    val t = HashMap[Int, Array[String]]()
    for (topicWordFreqStat <- testTopicWordFreqStatArray) {
      val arr = topicWordFreqStat.split(" ")
      val wordNo = arr(0).toInt
      val tweetFreq = arr.takeRight(arr.length - 1)
      t(wordNo) = tweetFreq
    }
    t
  }
  val trainTopicwordTweetFreqStat = {
    val trainTopicWordFreqStatFile = configInfo.getTrainTopicWordFreqStatFile()
    val trainTopicWordFreqStatArray: Array[String] = scala.io.Source.fromFile(trainTopicWordFreqStatFile).getLines().toArray
    val t = HashMap[Int, Array[String]]()
    for (topicWordFreqStat <- trainTopicWordFreqStatArray) {
      val arr = topicWordFreqStat.split(" ")
      val wordNo = arr(0).toInt
      val tweetFreq = arr.takeRight(arr.length - 1)
      t(wordNo) = tweetFreq
    }
    t
  }

  /**
   *
   * update topic word weight
   * topicWordFreq : tweetId feature which appears topic word
   * @param LabeledData : feature vectors
   * @param wordNo2WeightMap : updated topic-word weight, word No is from 1
   * @param numPublicWord : number of public word
   * @return feature vectors which topic-word weight had been updated
   *
   */
  def updateTestDataTopicWordWeight(LabeledData: RDD[LabeledPoint],
    wordNo2WeightMap: HashMap[Int, Double],
    numPublicWord: Int): RDD[LabeledPoint] = {

    val labeleDataArray: Array[LabeledPoint] = LabeledData.toArray

    for (wordNoWeight <- wordNo2WeightMap) {
      val wordNo = wordNoWeight._1
      val updatedWeight = wordNoWeight._2
      val tweetIdFreqArray = testTopicwordTweetFreqStat(wordNo - numPublicWord)
      for (tweetIdFreq <- tweetIdFreqArray) {
        val arr = tweetIdFreq.split(":")
        val pos = {
          val tweetId = arr(0).toInt
          tweetId - 1
        }
        val wordFreq = arr(1).toDouble

        val featureVector = labeleDataArray(pos).features.toArray
        featureVector(wordNo - 1) = updatedWeight
        labeleDataArray(pos) = LabeledPoint(labeleDataArray(pos).label, Vectors.dense(featureVector))
      }
    }
    sc.parallelize(labeleDataArray, numSlices)
  }
  def updateTrainDataTopicWordWeight(LabeledData: RDD[LabeledPoint],
    wordNo2WeightMap: HashMap[Int, Double],
    numPublicWord: Int): RDD[LabeledPoint] = {

    val labeleDataArray: Array[LabeledPoint] = LabeledData.toArray

    for (wordNoWeight <- wordNo2WeightMap) {
      val wordNo = wordNoWeight._1
      val updatedWeight = wordNoWeight._2
      val tweetIdFreqArray = trainTopicwordTweetFreqStat(wordNo - numPublicWord)
      for (tweetIdFreq <- tweetIdFreqArray) {
        val arr = tweetIdFreq.split(":")
        val pos = {
          val tweetId = arr(0).toInt
          tweetId - 1
        }
        val wordFreq = arr(1).toDouble

        val featureVector = labeleDataArray(pos).features.toArray
        featureVector(wordNo - 1) = updatedWeight
        labeleDataArray(pos) = LabeledPoint(labeleDataArray(pos).label, Vectors.dense(featureVector))
      }
    }
    sc.parallelize(labeleDataArray, numSlices)
  }

  /**
   * split dataset feature into text and non-text feature
   * @param: LabledData: dataset
   * @return: (text feature,non-text feature)
   */
  def splitDataSet(LabeledData: RDD[LabeledPoint]): (RDD[LabeledPoint], RDD[LabeledPoint]) = {
    val emocValuePos = configInfo.getEmocValuePosition()
    val topicPosition = emocValuePos + configInfo.getNumNontextFeature() - 1

    var textDataSet = LabeledData.map(labelData => {
      val feature: Array[Double] = labelData.features.toArray
      for (pos <- emocValuePos - 1 to topicPosition - 1) {
        feature(pos) = 0.0
      }
      LabeledPoint(labelData.label, Vectors.dense(feature))
    })

    var nontextDataSet = LabeledData.map(labelData => {
      val feature: Array[Double] = labelData.features.toArray
      for (pos <- 0 to emocValuePos - 2) {
        feature(pos) = 0.0
      }
      LabeledPoint(labelData.label, Vectors.dense(feature))
    })

    (textDataSet, nontextDataSet)
  }

  /**
   * predict test data based on msvm model
   * @model : MSVM model
   * @testData
   * @return : predict result (tweetId,(predict class, predict score))
   * and predict result stat  (accuracy, avgPrecision, avgRecall,classifyRight, numTestData, numUnknown)
   * and predict score distribution (negative predict score,neutral predict score,negative predict score)
   */
  def predictTestData(model: MSVMModel, testData: RDD[LabeledPoint]): (RDD[(Int, Double, Array[Double])], (Double, Double, Double, Int, Int, Int)) = {

    val predictResult: RDD[((Int, Double, Array[Double]), (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int))] = testData.map { point =>
      val classAndscore: (Int, Double, Array[Double]) = model.predict(point.features)
      val predictClass: Int = classAndscore._1
      val predictScore: Double = classAndscore._2
      val predictScoreDistribute: Array[Double] = classAndscore._3

      val humanLabel: Int = point.label.toInt

      if (predictClass == 0) {
        if (humanLabel == 0)
          ((predictClass, predictScore, predictScoreDistribute), (1, 0, 0, 0, 0, 0, 0, 0, 0, 0)) // TruePositive
        else if (humanLabel == 1)
          ((predictClass, predictScore, predictScoreDistribute), (0, 1, 0, 0, 0, 0, 0, 0, 0, 0)) // FalsePositiveNeutral
        else if (humanLabel == 2)
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 1, 0, 0, 0, 0, 0, 0, 0)) // FalsePositiveNegative
        else
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 0, 0, 0, 0, 1)) // human label is "Unknown"
      } else if (predictClass == 1) {
        if (humanLabel == 0)
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 1, 0, 0, 0, 0, 0, 0)) // FalseNeutralPositive
        else if (humanLabel == 1)
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 1, 0, 0, 0, 0, 0)) // TrueNeutral
        else if (humanLabel == 2)
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 1, 0, 0, 0, 0)) // FalseNeutralNegative
        else
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 0, 0, 0, 0, 1)) // human label is "Unknown"
      } else if (predictClass == 2) {
        if (humanLabel == 0)
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 0, 1, 0, 0, 0)) // FalseNegativePositive
        else if (humanLabel == 1)
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 0, 0, 1, 0, 0)) // FalseNegativeNeutral
        else if (humanLabel == 2)
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 0, 0, 0, 1, 0)) // TrueNegative
        else
          ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 0, 0, 0, 0, 1)) // human label is "Unknown"
      } else {
        ((predictClass, predictScore, predictScoreDistribute), (0, 0, 0, 0, 0, 0, 0, 0, 0, 1)) // predict class is "Unknown" is impossible
      }
    }
    //  (accuracy, avgPrecision, avgRecall,classifyRight, numTestData, numUnknown)
    val resultStat: (Double, Double, Double, Int, Int, Int) = {
      val result = predictResult.map(tuple => tuple._2)
      val tuple = result.reduce((r1, r2) => (r1._1 + r2._1, r1._2 + r2._2, r1._3 + r2._3,
        r1._4 + r2._4, r1._5 + r2._5, r1._6 + r2._6,
        r1._7 + r2._7, r1._8 + r2._8, r1._9 + r2._9, r1._10 + r2._10))

      val numTruePositive = tuple._1
      val numFalsePositiveNeutral = tuple._2
      val numFalsePositiveNegative = tuple._3

      val numFalseNeutralPositive = tuple._4
      val numTrueNeutral = tuple._5
      val numFalseNeutralNegative = tuple._6

      val numFalseNegativePositive = tuple._7
      val numFalseNegativeNeutral = tuple._8
      val numTrueNegative = tuple._9

      val numUnknown = tuple._10

      val numTestData = tuple._1 + tuple._2 + tuple._3 + tuple._4 + tuple._5 + tuple._6 + tuple._7 + tuple._8 + tuple._9
      val classifyRight = numTruePositive + numTrueNeutral + numTrueNegative
      val accuracy = classifyRight.toDouble / numTestData.toDouble

      // calculate average precision
      val numClassifyPositive = (numTruePositive + numFalsePositiveNeutral + numFalsePositiveNegative)
      val numClassifyNeutral = (numFalseNeutralPositive + numTrueNeutral + numFalseNeutralNegative)
      val numClassifyNegative = (numFalseNegativePositive + numFalseNegativeNeutral + numTrueNegative)
      var avgPrecision = 0.0
      var numLabelClass = 0
      if (numClassifyPositive != 0) {
        avgPrecision += numTruePositive.toDouble / numClassifyPositive.toDouble
        numLabelClass = numLabelClass + 1
      }
      if (numClassifyNeutral != 0) {
        avgPrecision += numTrueNeutral.toDouble / numClassifyNeutral.toDouble
        numLabelClass = numLabelClass + 1
      }
      if (numClassifyNegative != 0) {
        avgPrecision += numTrueNegative.toDouble / numClassifyNegative.toDouble
        numLabelClass = numLabelClass + 1
      }
      avgPrecision /= numLabelClass.toDouble
      // calculate average recall
      val numLabelPositive = (numTruePositive + numFalseNeutralPositive + numFalseNegativePositive)
      val numLabelNeutral = (numFalsePositiveNeutral + numTrueNeutral + numFalseNegativeNeutral)
      val numLabelNegative = (numFalsePositiveNegative + numFalseNeutralNegative + numTrueNegative)

      var avgRecall = 0.0
      numLabelClass = 0
      if (numLabelPositive != 0) {
        avgRecall += numTruePositive.toDouble / numLabelPositive.toDouble
        numLabelClass = numLabelClass + 1
      }

      if (numLabelNeutral != 0) {
        avgRecall += numTrueNeutral.toDouble / numLabelNeutral.toDouble
        numLabelClass = numLabelClass + 1
      }

      if (numLabelNegative != 0) {
        avgRecall += numTrueNegative.toDouble / numLabelNegative.toDouble
        numLabelClass = numLabelClass + 1
      }

      avgRecall /= numLabelClass.toDouble
      (accuracy, avgPrecision, avgRecall, classifyRight, numTestData, numUnknown)
    }
    val predictClassAndScore: RDD[(Int, Double, Array[Double])] = predictResult.map(tuple => tuple._1)
    (predictClassAndScore, resultStat)
  }

  /**
   * select topK test data whose predict score larger than threshold
   * @param: textPred : (text predict class : score)
   * @param: nontextPred : (non-text predict class : score)
   * @param: tweetIdAdded : already added tweet id
   * @param: threshold
   * @return : selecte tweetIds RDD[(Int, Int, Double)] : (tweetId, classify Class, classify score)
   */
  def selectTopKTestData(textPred: RDD[(Int, Double, Array[Double])],
    nontextPred: RDD[(Int, Double, Array[Double])], //(predict class, predict score)
    tweetIdAdded: HashSet[Integer],
    threshold: Double): RDD[(Int, Int, Double, Array[Double])] = {

    val predictResult: RDD[(Int, Int, Double, Array[Double], Int, Double, Array[Double])] = {
      val textPredArray: Array[(Int, Double, Array[Double])] = textPred.collect()
      val nontextPredArray: Array[(Int, Double, Array[Double])] = nontextPred.collect()
      var predArray = new Array[(Int, Int, Double, Array[Double], Int, Double, Array[Double])](textPredArray.size) // tweetId and two classify results
      for (pos <- 0 to textPredArray.size - 1) {
        predArray(pos) = (pos + 1, textPredArray(pos)._1, textPredArray(pos)._2, textPredArray(pos)._3,
          nontextPredArray(pos)._1, nontextPredArray(pos)._2, nontextPredArray(pos)._3)
      }
      sc.parallelize(predArray, numSlices)
    }
    val selectedData: RDD[(Int, Int, Double, Array[Double])] = predictResult
      .filter(elem => (!tweetIdAdded.contains(elem._1) && elem._2 == elem._5 &&
        elem._3 >= threshold && elem._6 >= threshold))
      .map(elem => {
        if (elem._3 > elem._6)
          (elem._1, elem._2, elem._6, elem._7)
        else
          (elem._1, elem._2, elem._3, elem._4)
      }).sortBy(_._3, false)
    selectedData
  }
  /**
   * 1.transfer test data to train data
   * 2.transfer testTweetWordStat to trainTweetWordStat
   * 3.transfer testTopicwordTweetFreqStat to trainTopicwordTweetFreqStat
   * @param: testData
   * @param: trainData
   * @param: curIterAddedData (added tweet Id,predict class)
   * return : updated train data
   */
  def transfer(testData: RDD[LabeledPoint],
    trainData: RDD[LabeledPoint],
    curIterAddedData: HashMap[Int, Int],
    numTrainData: Int): RDD[LabeledPoint] = {

    var trainDataNo = numTrainData + 1
    var transferedArray: Array[(Int, LabeledPoint)] = {
      val testDataArray = testData.collect()
      var xx = new Array[(Int, LabeledPoint)](testDataArray.size)
      for (pos <- 0 to testDataArray.size - 1) {
        xx(pos) = (pos + 1, testDataArray(pos)) // tweetId , LabeledPoint
      }
      val ptestData = sc.parallelize(xx, numSlices)
      ptestData.filter(elem => curIterAddedData.contains(elem._1)).toArray
    }
    for (transferedata <- transferedArray) {
      val tweetId = transferedata._1
      if (testTweetWordStat.contains(tweetId)) {
        val wordNoFreqArray = testTweetWordStat(tweetId)
        trainTweetWordStat(trainDataNo) = wordNoFreqArray
        for (wordNoFreq <- wordNoFreqArray) {
          val arr = wordNoFreq.split(":")
          val wordNo = arr(0).toInt
          val freq = arr(1).toInt
          trainTopicwordTweetFreqStat(wordNo) =
            trainTopicwordTweetFreqStat(wordNo) ++ Array(trainDataNo.toString + ":" + freq.toString)
        }
      }
      trainDataNo = trainDataNo + 1
    }
    val transferedData: RDD[LabeledPoint] = {
      for (pos <- 0 to transferedArray.length - 1) {
        val tweetId = transferedArray(pos)._1
        val labelpoint = transferedArray(pos)._2
        val classifyClass: Int = curIterAddedData(tweetId)
        transferedArray(pos) = (tweetId, LabeledPoint(classifyClass, labelpoint.features))
      }
      sc.parallelize(transferedArray, numSlices).map(elem => elem._2)
    }

    trainData ++ transferedData
  }
  /**
   * calculate each topic-word positive, neutral and negative weight
   * @param: selectedData : selected Data (tweetId, predict class, score)
   * @param: numPublicWord: number of public word
   * @param: numTopKTopicWord
   *
   * @return: (topic positive wordNo, weight), (neutral wordNo,weight),(negative wordNo,weight)
   */
  def selectTopKTopicWord(selectedData: RDD[(Int, Int, Double, Array[Double])],
    numPublicWord: Int,
    numTopKTopicWord: Int): (Array[(Int, Double)], Array[(Int, Double)], Array[(Int, Double)]) = {

    //    val numTopicWord = topicWordFreq.collect().length
    val numTopicWord = testTopicwordTweetFreqStat.keySet.size
    val tweetWordFreqsRDD = sc.broadcast(testTweetWordStat)
    val finalwordNo2Weight: HashMap[Integer, (Double, Double, Double)] = selectedData.map(elem => {
      val wordNo2Weight = HashMap[Integer, (Double, Double, Double)]()
      for (i <- 1 to numTopicWord) {
        wordNo2Weight(i) = (0.0, 0.0, 0.0)
      }
      val tweetWordFreqsrdd = tweetWordFreqsRDD.value
      val tweetId = elem._1
      val predictClass = elem._2
      val predictScore = elem._3
      val predictScoreDistribution = elem._4
      if (tweetWordFreqsrdd.contains(tweetId)) {
        val wordFreqArray: Array[String] = tweetWordFreqsrdd(tweetId)
        for (wordFreq <- wordFreqArray) {
          val arr = wordFreq.split(":")
          val wordNo = arr(0).toInt
          val freq = arr(1).toDouble
          wordNo2Weight(wordNo) = (wordNo2Weight(wordNo)._1 + predictScoreDistribution(0) * freq,
            wordNo2Weight(wordNo)._2 + predictScoreDistribution(1) * freq,
            wordNo2Weight(wordNo)._3 + predictScoreDistribution(2) * freq)
        }
      }
      wordNo2Weight
    }).reduce((s1, s2) => {
      val r = HashMap[Integer, (Double, Double, Double)]()
      for (key <- s1.keySet) {
        r(key) = (s1(key)._1 + s2(key)._1, s1(key)._2 + s2(key)._2, s1(key)._3 + s2(key)._3)
      }
      r
    }).map(elem => {
      val sumElem = elem._2._1 + elem._2._2 + elem._2._3
      (elem._1, (elem._2._1 / sumElem, elem._2._2 / sumElem, elem._2._3 / sumElem))
    })

    val wordNo2WeightRDD: RDD[(Integer, (Double, Double, Double))] = {
      val t: RDD[(Integer, (Double, Double, Double))] = sc.parallelize(finalwordNo2Weight.toSeq, numSlices)
      val x = t.map(elem => {
        if (elem._2._1 >= elem._2._2 && elem._2._1 > elem._2._3)
          (elem._1, (elem._2._1, 0.0, 0.0))
        else if (elem._2._3 > elem._2._1 && elem._2._3 >= elem._2._2)
          (elem._1, (0.0, 0.0, elem._2._3))
        else
          (elem._1, (0.0, elem._2._2, 0.0))
      })
      x
    }
    val topKPositiveWord = wordNo2WeightRDD
      .filter(e => e._2._1 > 0.0)
      .map(elem => (elem._1 + numPublicWord, elem._2._1))
      .sortBy(_._2, false).take(numTopKTopicWord)

    val topKNeutralWord = wordNo2WeightRDD
      .filter(e => e._2._2 > 0.0)
      .map(elem => (elem._1 + numPublicWord, elem._2._2))
      .sortBy(_._2, false).take(numTopKTopicWord)
    val topKNegativeWord = wordNo2WeightRDD
      .filter(e => e._2._3 > 0.0)
      .map(elem => (elem._1 + numPublicWord, elem._2._3))
      .sortBy(_._2, false).take(numTopKTopicWord)
    (topKPositiveWord, topKNeutralWord, topKNegativeWord)
  }
  def init() = {
    FileUtil.createDir(this.configInfo.getClassifyStatResultDir())
  }

  def start() = {
    val startTime = System.nanoTime()

    val dataFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm")
    val resultFileName: String = FileUtil.mergeFileDirAndName(configInfo.getClassifyStatResultDir(), dataFormat.format(new Date()))

    val resultWriter = new AppendContent2File(resultFileName)
    resultWriter.appendContent2File("parameter numTopKTestData2TrainSet:\t" + configInfo.getNumTopKTestData2TrainSet())
    resultWriter.appendContent2File("parameter classifyResultThreshold:\t" + configInfo.getClassifyResultThreshold())
    resultWriter.appendContent2File("parameter maxIteration:\t" + configInfo.getMaxIteration())
    resultWriter.appendContent2File("parameter numTopKPrivateWord:\t" + configInfo.getNumTopKPrivateWord())
    resultWriter.appendContent2File("Iteration Result:\tNumTestData:\taccuracy\tprecision\trecall")

    val positiveTopicWordWriter = new AppendContent2File(positiveTopicWordFile)
    val neutralTopicWordWriter = new AppendContent2File(neutralTopicWordFile)
    val negativeTopicWordWriter = new AppendContent2File(negativeTopicWordFile)

    val MaxIteration = configInfo.getMaxIteration()
    // (tweetId, classify Class, classify score)
    var selectedData: RDD[(Int, Int, Double, Array[Double])] = sc.parallelize(Array((1, 0, 0.0, Array(0.0, 0.0, 0.0))), numSlices)
    val wordNo2WeightMap = HashMap[Int, Double]()

    val curPeriodTrainFileName: String = configInfo.getTrainFileName()
    var curTrainData = {
      val curTrainFile = curPeriodTrainFileName //FileUtil.mergeFileDirAndName(configInfo.getTrainFileDir(), curPeriodTrainFileName)
      MLUtils.loadLibSVMFile(sc, curTrainFile)
    }
    var numTrainData = curTrainData.count().toInt

    val tweetIdAdded = new HashSet[Integer]() // selected tweetId during iteration
    tweetIdAdded.clear()
    var curTestData = {
      //      val testFileDir = configInfo.getTestFileDir()
      val testFileName = configInfo.getTestFileName() //+ Integer.toString(curIteration)
      val curTestFile = testFileName //FileUtil.mergeFileDirAndName(testFileDir, testFileName)
      MLUtils.loadLibSVMFile(sc, curTestFile)
    }
    for (dayNo <- 1 to configInfo.getNumPeriod()) {
      var curIteration = 0
      var curdaySelectedData: RDD[(Int, Int, Double, Array[Double])] = sc.parallelize(Array((1, 0, 0.0, Array(0.0, 0.0, 0.0))), numSlices)

      breakable {
        for (curIteration <- 0 to MaxIteration - 1) {
          // update private word weight 
          curTrainData = updateTrainDataTopicWordWeight(curTrainData, wordNo2WeightMap, configInfo.getNumPublicSentimentWord())
          curTestData = updateTestDataTopicWordWeight(curTestData, wordNo2WeightMap, configInfo.getNumPublicSentimentWord())

          // create model using train data set and predict test data
          val combineModel = MSVMWithSGD.train(curTrainData, numIterations)
          val combinePrediction = predictTestData(combineModel, curTestData)
          resultWriter.appendContent2File("period " + dayNo + "\t" + "iteration " + curIteration + "\t" + "classify result: "
            + "numTrain " + curTrainData.count() + "\t"
            + "classify right " + combinePrediction._2._4 + "\t"
            + "Labeled numTest " + combinePrediction._2._5 + "\t"
            + "numUnknownInTest " + combinePrediction._2._6 + "\t"
            + "accuracy " + combinePrediction._2._1 + "\t"
            + "average precision " + combinePrediction._2._2 + "\t"
            + "average recall " + combinePrediction._2._3)

          // split traindata into text feature data and non-text feature data
          val (textTrainData, nontextTrainDatå) = splitDataSet(curTrainData)
          // train model based on text feature
          val textModel = MSVMWithSGD.train(textTrainData, numIterations)
          val textPrediction = predictTestData(textModel, curTestData)
          // train model based on non-text feature
          val nontextModel = MSVMWithSGD.train(nontextTrainDatå, numIterations)
          val nontextPrediction = predictTestData(nontextModel, curTestData)

          //select topK test data which classified results are same
          val curselectedData: RDD[(Int, Int, Double, Array[Double])] = selectTopKTestData(textPrediction._1, nontextPrediction._1, tweetIdAdded, configInfo.getClassifyResultThreshold())
          curdaySelectedData = curdaySelectedData ++ curselectedData
          val topK = configInfo.getNumTopKTestData2TrainSet()

          val selectedPositiveData = curselectedData.filter(elem => elem._2 == 0).sortBy(_._3, false, numSlices).take(topK)
          val selectedNeutralData = curselectedData.filter(elem => elem._2 == 1).sortBy(_._3, false, numSlices).take(topK)
          val selectedNegativeData = curselectedData.filter(elem => elem._2 == 2).sortBy(_._3, false, numSlices).take(topK)

          if (selectedPositiveData.size == 0 && selectedNeutralData.size == 0 && selectedNegativeData.size == 0) {
            logger.info("no more test data whose classify result confidence is enough larger than threshold")
            break
          }

          val curIterAddedData = new HashMap[Int, Int]()
          resultWriter.appendContent2File("selected positive data(tweetId score):")
          for (sData <- selectedPositiveData) {
            tweetIdAdded.add(sData._1)
            curIterAddedData(sData._1) = sData._2
            resultWriter.appendContent2File(sData._1 + "\t" + sData._3)
          }
          resultWriter.appendContent2File("\n")

          resultWriter.appendContent2File("selected neutral data(tweetId score):")
          for (sData <- selectedNeutralData) {
            tweetIdAdded.add(sData._1)
            curIterAddedData(sData._1) = sData._2
            resultWriter.appendContent2File(sData._1 + "\t" + sData._3)
          }
          resultWriter.appendContent2File("\n")

          resultWriter.appendContent2File("selected negative data(tweetId score):")
          for (sData <- selectedNegativeData) {
            tweetIdAdded.add(sData._1)
            curIterAddedData(sData._1) = sData._2
            resultWriter.appendContent2File(sData._1 + "\t" + sData._3)
          }
          resultWriter.appendContent2File("\n")

          curTrainData = transfer(curTestData, curTrainData, curIterAddedData, numTrainData)
          numTrainData += curIterAddedData.keySet.size

        } //for (curIteration <- 0 to MaxIteration)
      } //breakable
      selectedData = selectedData ++ curdaySelectedData
      /**
       * select non-public sentiment word Map each non-public sentiment
       * word to weight distribute in positive, neutral and negative, and
       * then decide non-public sentiment word weight
       */
      // (tweetId, classify Class, classify score)
      val numPublicWord = configInfo.getNumPublicSentimentWord()
      val (topKPositiveWord, topKNeutralWord, topKNegativeWord) =
        selectTopKTopicWord(curdaySelectedData, numPublicWord, configInfo.getNumTopKPrivateWord())

      // insert topK topic-word into wordNo2WeightMap
      val selectedTweetIds = selectedData.map(e => e._1).toArray
      resultWriter.appendContent2File("selected positive sentiment word(wordNo weight):")
      for (wordNoWeight <- topKPositiveWord) {
        val wordNo = wordNoWeight._1
        val weight = wordNoWeight._2
        resultWriter.appendContent2File(wordNo + "\t" + weight)
        val topicword = topicwordArray(wordNo - numPublicWord - 1)
        positiveTopicWordWriter.appendContent2File(topicword + "\t" + weight)

        wordNo2WeightMap(wordNo) = weight
      }
      resultWriter.appendContent2File("\n")

      resultWriter.appendContent2File("selected neutral sentiment word(wordNo weight):")
      for (wordNoWeight <- topKNeutralWord) {
        val wordNo = wordNoWeight._1
        val weight = wordNoWeight._2
        resultWriter.appendContent2File(wordNo + "\t" + weight)
        val topicword = topicwordArray(wordNo - numPublicWord - 1)
        neutralTopicWordWriter.appendContent2File(topicword + "\t" + weight)

        wordNo2WeightMap(wordNo) = weight
      }
      resultWriter.appendContent2File("\n")

      resultWriter.appendContent2File("selected negative sentiment word(wordNo weight):")
      for (wordNoWeight <- topKNegativeWord) {
        val wordNo = wordNoWeight._1
        val weight = wordNoWeight._2
        resultWriter.appendContent2File(wordNo + "\t" + weight)
        val topicword = topicwordArray(wordNo - numPublicWord - 1)
        negativeTopicWordWriter.appendContent2File(topicword + "\t" + weight)

        wordNo2WeightMap(wordNo) = weight
      }
      // update train data set topic word weight
      curTrainData = updateTrainDataTopicWordWeight(curTrainData, wordNo2WeightMap, configInfo.getNumPublicSentimentWord())
      curTestData = updateTestDataTopicWordWeight(curTestData, wordNo2WeightMap, configInfo.getNumPublicSentimentWord())

      val curPeriodModel = MSVMWithSGD.train(curTrainData, numIterations)
      val curPeriodPrediction = predictTestData(curPeriodModel, curTestData)

      resultWriter.appendContent2File("period " + dayNo + "\t" + "final " + "\t" + "classify result: "
        + "numTrain " + curTrainData.count() + "\t"
        + "classify right " + curPeriodPrediction._2._4 + "\t"
        + "Labeled numTest " + curPeriodPrediction._2._5 + "\t"
        + "numUnknownInTest " + curPeriodPrediction._2._6 + "\t"
        + "accuracy " + curPeriodPrediction._2._1 + "\t"
        + "average precision " + curPeriodPrediction._2._2 + "\t"
        + "average recall " + curPeriodPrediction._2._3)
    } //for (dayNo <- 1 to configInfo.getNumPeriod())

    val estimatedTime = System.nanoTime() - startTime
    resultWriter.appendContent2File("algorithm running time:\t" + (TimeUnit.NANOSECONDS.toSeconds(estimatedTime)).toString)

    resultWriter.closeFileWriter()
    positiveTopicWordWriter.closeFileWriter()
    neutralTopicWordWriter.closeFileWriter()
    negativeTopicWordWriter.closeFileWriter()
    logger.info("algorithm stop")
  }
}