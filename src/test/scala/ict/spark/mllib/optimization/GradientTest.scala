package ict.spark.mllib.optimization

import org.apache.spark.mllib.linalg.Vectors

object GradientTest {

  def main(args: Array[String]): Unit = {
    val msvmGradient = new MSVMLogisticGradient()
    val data = Vectors.dense(Array[Double](1.2, 0.8))
    val label = 1
    val weights = {
      val weight1 = Vectors.dense(Array[Double](0.1, 0.2))
      val weight2 = Vectors.dense(Array[Double](0.2, 0.3))
      val weight3 = Vectors.dense(Array[Double](0.3, 0.1))

      Array(weight1, weight2, weight3)
    }
    val gradient = msvmGradient.compute(data, label, weights)
    for (elem <- gradient) {
      for (ei <- elem.toArray) {
        print(ei + "\t")
      }
      println("\n")
    }
  }

}