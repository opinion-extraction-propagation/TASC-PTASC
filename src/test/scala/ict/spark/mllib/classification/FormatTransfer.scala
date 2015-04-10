package ict.spark.mllib.classification

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.storage.StorageLevel
import ict.spark.mllib.classification.MSVMWithSGD
import org.apache.spark.mllib.linalg.Vectors

object FormatTransfer {
  def loadSVMData(parsedData: RDD[String]): RDD[LabeledPoint] = {
    val parsed = parsedData
      .map(_.trim)
      .filter(line => !(line.isEmpty || line.startsWith("#")))
      .map { line =>
        val items = line.split(' ')
        val label = items.head.toDouble
        val (indices, values) = items.tail.filter(_.nonEmpty).map { item =>
          val indexAndValue = item.split(':')
          val index = indexAndValue(0).toInt - 1 // Convert 1-based indices to 0-based.
          val value = indexAndValue(1).toDouble
          (index, value)
        }.unzip
        (label, indices.toArray, values.toArray)
      }

    // Determine number of features.
    val d = {
      parsed.persist(StorageLevel.MEMORY_ONLY)
      parsed.map {
        case (label, indices, values) =>
          indices.lastOption.getOrElse(0)
      }.reduce(math.max) + 1
    }

    parsed.map {
      case (label, indices, values) =>
        LabeledPoint(label, Vectors.sparse(d, indices, values))
    }
  }
  def main(args: Array[String]): Unit = {
    // 
    val conf: SparkConf = new SparkConf().setAppName("FormatTransfer").setMaster("local")
    val sc: SparkContext = new SparkContext(conf)

    val monthMap = Map[String, Int](("spring" -> 1), ("summer" -> 2), ("autumn" -> 3), ("winter" -> 4))
    val dayMap = Map[String, Int](("monday" -> 1), ("tuesday" -> 2), ("wednesday" -> 3), ("thursday" -> 4),
      ("friday" -> 5), ("saturday" -> 6), ("sunday" -> 0))
    val topicMap = Map[String, Int](("obama" -> 1), ("tacobell" -> 2), ("apple" -> 3), ("google" -> 4), ("microsoft" -> 5), ("twitter" -> 6))
    val classMap = Map[String, Int](("positive" -> 1), ("neutral" -> 0), ("negative" -> 2))

    val data = sc.textFile("data/sentimentClassification")
    // {0 3232,5946 0,6137 0,6216 0,6390 0,6549 0,6551 0,6962 0,6963 0,6964 8,6965 autumn,6966 saturday,6967 3,6968 30,6969 0,6970 0,6971 obama,6972  neutral}
    // 1 152:1 153:168 154:242
    // 0 5946:0 6137:0 6216:0 6390:0 6549:0 6551:0 6962:0 6963:0 6964:8 6965:3 6966:6 6967:3 6968:30 6969:0 6970:0 6971:1
    val parsedData: RDD[String] = data.filter(line => !line.contains('@')).filter(line => !line.isEmpty()).map { line =>
      var replacedLine = line
      for ((key, value) <- monthMap)
        replacedLine = replacedLine.replace(key, value.toString)
      for ((key, value) <- dayMap)
        replacedLine = replacedLine.replace(key, value.toString)
      for ((key, value) <- topicMap)
        replacedLine = replacedLine.replace(key, value.toString)
      for ((key, value) <- classMap)
        replacedLine = replacedLine.replace(key, value.toString)
      // {0 3232,6963 0,6964 8,6965 3,6966 6,6967 3,6968 30,6969 0,6970 0,6971 1,6972  0}
      var parts = replacedLine.substring(1, replacedLine.length() - 1).split(',')
      var finalLine = parts.takeRight(1)(0).split(' ').takeRight(1)(0)
      parts = parts.dropRight(1).drop(1)
      for (key <- parts) {
        val arr = key.split(' ')
        val t = arr(0) + ':' + arr(1)
        finalLine += (' ' + t)
      }
      finalLine
    }
    //    parsedData.foreach(println)
    //    parsedData.saveAsTextFile("data/sentimentClassification/svmData")

    // Load training data in LIBSVM format.
    val alldata: RDD[LabeledPoint] = loadSVMData(parsedData)

    // Split data into training (60%) and test (40%).
    val splits: Array[RDD[LabeledPoint]] = alldata.randomSplit(Array(0.6, 0.4), seed = 11L)
    val trainData = splits(0).cache()
    val testData = splits(1)

    // Run training algorithm to build the model
    val numIterations = 1000
    val model = MSVMWithSGD.train(trainData, numIterations)

    // Compute raw scores on the test set. 
    val scoreAndLabels: RDD[((Int, Double, Array[Double]), Double)] = testData.map { point =>
      val score = model.predict(point.features)
      (score, point.label)
    }

    val rightNum = scoreAndLabels.map(item => if (item._1._1 == item._2.toInt) 1 else 0).reduce((t1, t2) => t1 + t2)
    val total = scoreAndLabels.count()
    println("prediction is \t" + rightNum.toDouble / total.toDouble)
    //    println(scoreAndLabels.collect().mkString("\n"))
    //    val weights = model.weightArray
    //    for (weight <- weights) {
    //      weight.toArray.foreach(elem => print(elem + "\t"))
    //      println("\n")
    //    }
    // Get evaluation metrics.
    //    val metrics = new MulticlassMetrics(scoreAndLabels)
    //    val precision = metrics.precision(1.0)
    //    println("precision(0.0) = " + precision)
  }

}
