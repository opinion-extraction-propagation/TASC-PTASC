package ict.spark.mllib.optimization

import org.apache.spark.mllib.linalg.Vectors

object UpdaterTest {

  def main(args: Array[String]): Unit = {
    val msvmGradient = new MSVMLogisticGradient()
    val msvmUpdater = new SimpleMSVMUpdater()
    val data = Vectors.dense(Array[Double](1.2, 0.8))
    val label = 1
    val oldWeights = {
      val weight1 = Vectors.dense(Array[Double](0.1, 0.2))
      val weight2 = Vectors.dense(Array[Double](0.2, 0.3))
      val weight3 = Vectors.dense(Array[Double](0.3, 0.1))

      Array(weight1, weight2, weight3)
    }
    val gradient = msvmGradient.compute(data, label, oldWeights)
    /*
     *	0.35346996450679946	0.23564664300453297	

		-0.7682708104246179	-0.5121805402830786	

		0.4148008459178184	0.2765338972785456	
     * 
     */
    val stepSize = 0.9
    val iter = 5
    val regParam = 0.1
    val newWeights = msvmUpdater.compute(oldWeights, gradient, stepSize, iter, regParam)
    for (elem <- newWeights) {
      for (ei <- elem.toArray) {
        print(ei + "\t")
      }
      println("\n")
    }

  }

}