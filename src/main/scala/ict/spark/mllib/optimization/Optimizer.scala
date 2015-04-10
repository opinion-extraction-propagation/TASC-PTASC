package ict.spark.mllib.optimization

import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.{ Vectors, Vector }
import ict.spark.mllib.linalg.MyBLAS._
import scala.collection.mutable.ArrayBuffer

trait MSVMOptimizer extends Serializable {
  def optimize(data: RDD[(Double, Vector)], initialWeights: Array[Vector]): Array[Vector]
}
/**
 * ImprovedGradientDescent
 * Class used to solve an optimization problem using Gradient Descent.
 * @param gradient Gradient function to be used.
 * @param updater Updater to be used to update weights after every iteration.
 */
class MSVMGGradientDescent private[mllib] (
  private var gradient: MSVMGradient,
  private var updater: MSVMUpdater)
  extends MSVMOptimizer with Logging {

  private var numClass: Int = 3
  private var stepSize: Double = 1.0
  private var numIterations: Int = 100
  private var regParam: Double = 0.0
  private var miniBatchFraction: Double = 1.0

  def setNumClass(numClass: Int): this.type = {
    this.numClass = numClass
    this
  }
  /**
   * Set the initial step size of SGD for the first step. Default 1.0.
   * In subsequent steps, the step size will decrease with stepSize/sqrt(t)
   */
  def setStepSize(step: Double): this.type = {
    this.stepSize = step
    this
  }

  /**
   * Set fraction of data to be used for each SGD iteration.
   * Default 1.0 (corresponding to deterministic/classical gradient descent)
   */
  def setMiniBatchFraction(fraction: Double): this.type = {
    this.miniBatchFraction = fraction
    this
  }

  /**
   * Set the number of iterations for SGD. Default 100.
   */
  def setNumIterations(iters: Int): this.type = {
    this.numIterations = iters
    this
  }

  /**
   * Set the regularization parameter. Default 0.0.
   */
  def setRegParam(regParam: Double): this.type = {
    this.regParam = regParam
    this
  }

  /**
   * Set the gradient function (of the loss function of one single data example)
   * to be used for SGD.
   */
  def setGradient(gradient: MSVMGradient): this.type = {
    this.gradient = gradient
    this
  }

  /**
   * Set the updater function to actually perform a gradient step in a given direction.
   * The updater is responsible to perform the update from the regularization term as well,
   * and therefore determines what kind or regularization is used, if any.
   */
  def setUpdater(updater: MSVMUpdater): this.type = {
    this.updater = updater
    this
  }

  /**
   * Runs gradient descent on the given training data.
   * @param data training data
   * @param initialWeights initial weights
   * @return solution vector
   */
  def optimize(data: RDD[(Double, Vector)], initialWeights: Array[Vector]): Array[Vector] = {
    val weights = MSVMGGradientDescent.runMiniBatchSGD(
      data,
      gradient,
      updater,
      numClass,
      stepSize,
      numIterations,
      regParam,
      miniBatchFraction,
      initialWeights)
    weights
  }

}

/**
 * Top-level method to run gradient descent.
 */
object MSVMGGradientDescent extends Logging {
  /**
   * Run stochastic gradient descent (SGD) in parallel using mini batches.
   * In each iteration, we sample a subset (fraction miniBatchFraction) of the total data
   * in order to compute a gradient estimate.
   * Sampling, and averaging the subgradients over this subset is performed using one standard
   * spark map-reduce in each iteration.
   *
   * @param data - Input data for SGD. RDD of the set of data examples, each of
   *               the form (label, [feature values]).
   * @param gradient - Gradient object (used to compute the gradient of the loss function of
   *                   one single data example)
   * @param updater - Updater function to actually perform a gradient step in a given direction.
   * @param stepSize - initial step size for the first step
   * @param numIterations - number of iterations that SGD should be run.
   * @param regParam - regularization parameter
   * @param miniBatchFraction - fraction of the input data set that should be used for
   *                            one iteration of SGD. Default value 1.0.
   *
   * @return A tuple containing two elements. The first element is a column matrix containing
   *         weights for every feature, and the second element is an array containing the
   *         stochastic loss computed for every iteration.
   */
  def runMiniBatchSGD(
    data: RDD[(Double, Vector)],
    gradient: MSVMGradient,
    updater: MSVMUpdater,
    numClass: Int,
    stepSize: Double,
    numIterations: Int,
    regParam: Double,
    miniBatchFraction: Double,
    initialWeights: Array[Vector]): Array[Vector] = {

    val stochasticLossHistory = new ArrayBuffer[Double](numIterations)

    val numExamples = data.count()
    val miniBatchSize = numExamples * miniBatchFraction

    // if no data, return initial weights to avoid NaNs
    if (numExamples == 0) {
      logInfo("ImprovedGradientDescent.runMiniBatchSGD returning initial weights, no data found")
      return initialWeights
    }

    // Initialize weights as a column vector
    var weights = initialWeights
    val numFeatures = weights(0).size

    /**
     * For the first iteration, the regVal will be initialized as sum of weight squares
     * if it's L2 updater; for L1 updater, the same logic is followed.
     */

    for (i <- 1 to numIterations) {
      val bcWeights = data.context.broadcast(weights)
      //      val bcWeights = weights
      // Sample a subset (fraction miniBatchFraction) of the total data
      // compute and sum up the subgradients on this subset (this is one map-reduce)
      val sampledData: RDD[(Double, Vector)] = data.sample(false, miniBatchFraction, 42 + i)
      val subGradRDD: RDD[Array[Vector]] = sampledData
        .map(sData => {
          val grad = gradient.compute(sData._2, sData._1.toInt, bcWeights.value)
          grad
        })

      var gradientSum: Array[Vector] = subGradRDD.reduce(
        (subGrad1: Array[Vector], subGrad2: Array[Vector]) => {
          //        axpy(1, subGrad1._1, subGrad2._1)
          var retGrad = subGrad1
          for (i <- 0 to subGrad1.length - 1) {
            retGrad(i) = add(subGrad1(i), subGrad2(i))
          }
          retGrad
        })

      // scal 
      gradientSum.map(elem => scal(1 / miniBatchSize, elem))

      val update = updater.compute(weights, gradientSum, stepSize, i, regParam)

      weights = update
    }

    weights

  }

}