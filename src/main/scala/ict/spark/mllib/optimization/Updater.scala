package ict.spark.mllib.optimization

import org.apache.spark.mllib.optimization.Updater
import org.apache.spark.mllib.linalg._
import ict.spark.mllib.linalg.MyBLAS

abstract class MSVMUpdater extends Serializable {
  def compute(
    weightsOld: Array[Vector],
    gradient: Array[Vector],
    stepSize: Double,
    iter: Int,
    regParam: Double): Array[Vector]
}
class SimpleMSVMUpdater extends MSVMUpdater {
  /**
   * Compute an updated value for weights given the gradient, stepSize, iteration number and
   * regularization parameter. Also returns the regularization value regParam * R(w)
   * computed using the *updated* weights.
   *
   * @param weightsOld - Column matrix of size dx1 where d is the number of features.
   * @param gradient - Column matrix of size dx1 where d is the number of features.
   * @param stepSize - step size across iterations
   * @param iter - Iteration number
   * @param regParam - Regularization parameter
   *
   * @return A tuple of 2 elements. The first element is a column matrix containing updated weights,
   *         and the second element is the regularization value computed using updated weights.
   */
  override def compute(
    weightsOld: Array[Vector],
    gradient: Array[Vector],
    stepSize: Double,
    iter: Int,
    regParam: Double): Array[Vector] = {

    val thisIterStepSize = stepSize / math.sqrt(iter)

    var updatedWeights = weightsOld
    for (i <- 0 to weightsOld.length - 1) {
      MyBLAS.axpy(-thisIterStepSize, gradient(i), updatedWeights(i))
    }

    updatedWeights
  }
}