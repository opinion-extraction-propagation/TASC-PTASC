package ict.spark.mllib.classification

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import java.util.concurrent.TimeUnit

object MSVMTest {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SimpleMSVM Application").setMaster("local")
    val sc = new SparkContext(conf)
    // Load training data in LIBSVM format.
    //    val trainData = MLUtils.loadLibSVMFile(sc, "data/mllib/libmsvm_train.txt")
    //    val testData = MLUtils.loadLibSVMFile(sc, "data/mllib/libmsvm_test.txt")
    val trainData = MLUtils.loadLibSVMFile(sc, "data/train/trainFile")
    val testData = MLUtils.loadLibSVMFile(sc, "data/test/day1/testFile")

    // Split data into training (60%) and test (40%).
    //    val splits = data.randomSplit(Array(0.5, 0.5), seed = 11L)
    //    val training = splits(0).cache()
    //    val test = splits(1)

    // Run training algorithm to build the model
    val numIterations = 1000
    val modelStartTime = System.nanoTime()
    val model = MSVMWithSGD.train(trainData, numIterations)
    val modelEstimatedTime = System.nanoTime() - modelStartTime;

    // Clear the default threshold.
    //    model.clearThreshold()

    // Compute raw scores on the test set. 
    val predictStartTime = System.nanoTime()
    val scoreAndLabels: RDD[((Int, Double, Array[Double]), Double, Int)] = testData.map { point =>
      val score = model.predict(point.features)
      if (score._1 == point.label.toInt)
        (score, point.label, 1)
      else
        (score, point.label, 0)
    }
    val numUnknownData = testData.map(data =>
      if (data.label.toInt == 3)
        1
      else
        0).reduce((t1, t2) => t1 + t2)
    val numAccuracy = scoreAndLabels.reduce((s1, s2) => (s1._1, s1._2, s1._3 + s2._3))._3
    val predictEstimatedTime = System.nanoTime() - predictStartTime;
    println("create model running time:\t" + TimeUnit.NANOSECONDS.toSeconds(modelEstimatedTime))
    println("predict " + testData.count() + " test data running time:\t" + TimeUnit.NANOSECONDS.toSeconds(predictEstimatedTime))
    //    println(scoreAndLabels.collect().mkString("\n"))
    println("accuracy : " + (numAccuracy.toDouble / (testData.count - numUnknownData).toDouble))

    //    val weights = model.weightArray
    //    for (weight <- weights) {
    //      weight.toArray.foreach(elem => print(elem + "\t"))
    //      println("\n")
    //    }

    // Get evaluation metrics.
    //    val metrics = new BinaryClassificationMetrics(scoreAndLabels)
    //    val auROC = metrics.areaUnderROC()
    //
    //    println("Area under ROC = " + auROC)
  }

}