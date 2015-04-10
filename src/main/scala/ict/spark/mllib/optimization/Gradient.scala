package ict.spark.mllib.optimization

import org.apache.spark.mllib.optimization.Gradient
import org.apache.spark.mllib.linalg._
import ict.spark.mllib.linalg.MyBLAS.{ axpy, dot, scal }
import ict.spark.mllib.linalg.MyBLAS
import scala.collection.mutable.ArrayBuffer

abstract class MSVMGradient extends Serializable {
  def compute(data: Vector, label: Int, weights: Array[Vector]): Array[Vector]
}
class MSVMLogisticGradient extends MSVMGradient {
  /**
   * Compute the gradient and loss given the features of a single data point.
   *
   * gradient(theta_j) = -1/m sigma(i<-1 to m) sigma(j<-1 to k) f(y^i = j) * x^i * (1-p(y^i = j | x^i ; theta ))
   * p(y^i = j | x^i;theta) = exp(theta_j * x^i) / sigma(l<-1 to k)(theta_l*x^i)
   *
   * @param data features for one data point : plus an additional "1" element for the intercept term R^(n+1)
   * @param label label for this data point
   * @param weights weights/coefficients corresponding to features
   *
   * @return (gradient: Matrix, loss: Double)
   */
  override def compute(data: Vector, label: Int, weights: Array[Vector]): Array[Vector] = {

    val marginArray: Array[Double] = {
      weights.map(elem => Math.exp(MyBLAS.dot(data, elem)))
    }
    val probability: Array[Double] = {

      val marginSum = marginArray.sum
      marginArray.map(elem => elem / marginSum)
    }
    var gradient: Array[Vector] = {
      var loss = probability
      loss(label) = loss(label) - 1.0
      var gradientArray = {
        loss.map(elem => {
          Vectors.dense(data.toArray.map(d => d * elem))
        })
      }
      gradientArray.asInstanceOf[Array[Vector]]
    }

    gradient
  }

}