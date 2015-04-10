package ict.spark.mllib.classification

import org.apache.spark.mllib.linalg.{ Matrix, Vector, Vectors }
import ict.spark.mllib.linalg.MyBLAS.{ scal, dot, axpy }
import ict.spark.mllib.optimization.MSVMGradient
import ict.spark.mllib.optimization.MSVMUpdater
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.Logging
import org.apache.spark.SparkException
import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.util.MLUtils
import ict.spark.mllib.linalg.MyBLAS
import ict.spark.mllib.optimization._
/**
 * Model for Multi-class Support Vector Machines (MSVMs).
 *
 * @param weights Weights computed for every feature.
 * @param intercept Intercept computed for this model.
 */
class MSVMModel(val weightArray: Array[Vector]) extends Serializable {

  def predict(testData: Vector): (Int, Double, Array[Double]) = {
    predictPoint(testData, weightArray)
  }
  def predictPoint(
    data: Vector,
    weightArray: Array[Vector]): (Int, Double, Array[Double]) = {
    val marginArray: Array[Double] = weightArray.map(elem => math.exp(MyBLAS.dot(data, elem)))
    val predictProbs: Array[Double] = {
      val marginSum = marginArray.sum
      marginArray.map(elem => elem / marginSum)
    }
    val maxPred = predictProbs.max
    val classPred = predictProbs.indexOf(maxPred)

    //    (classPred, maxPred)
    (classPred, maxPred, predictProbs)
  }

  def predict(testData: RDD[Vector]): RDD[(Int, Double, Array[Double])] = {
    // A small optimization to avoid serializing the entire model. Only the weightsMatrix
    // and intercept is needed.
    val localWeights = weightArray
    val bcWeights = testData.context.broadcast(localWeights)
    testData.mapPartitions { iter =>
      val w = bcWeights.value
      iter.map(v => predictPoint(v, w))
    }
  }
}
/**
 * Train a Multi-class Support Vector Machine (MSVM) using Stochastic Gradient Descent.
 * NOTE: Labels used in MSVM should be {0, 1,2}.
 */
class MSVMWithSGD private (
  private val numClass: Int,
  private var stepSize: Double,
  private var numIterations: Int,
  private var regParam: Double,
  private var miniBatchFraction: Double) extends Logging with Serializable {

  private val gradient = new MSVMLogisticGradient()
  private val updater = new SimpleMSVMUpdater()
  val optimizer = new MSVMGGradientDescent(gradient, updater)
    .setNumClass(numClass)
    .setStepSize(stepSize)
    .setNumIterations(numIterations)
    .setRegParam(regParam)
    .setMiniBatchFraction(miniBatchFraction)

  /** Whether to add intercept (default: false). */
  protected var addIntercept: Boolean = false
  protected var validateData: Boolean = true
  /**
   * Set if the algorithm should validate data before training. Default true.
   */
  def setValidateData(validateData: Boolean): this.type = {
    this.validateData = validateData
    this
  }
  /**
   * Whether to perform feature scaling before model training to reduce the condition numbers
   * which can significantly help the optimizer converging faster. The scaling correction will be
   * translated back to resulting model weights, so it's transparent to users.
   * Note: This technique is used in both libsvm and glmnet packages. Default false.
   */
  private var useFeatureScaling = false
  protected val validators = List(multiLabelValidator)

  def multiLabelValidator: RDD[LabeledPoint] => Boolean = { data =>
    val numInvalid = data.filter(x => x.label > numClass || x.label < 0.0).count()
    if (numInvalid != 0) {
      logError("Classification labels should be 0 or 1 or -1. Found " + numInvalid + " invalid labels")
    }
    numInvalid == 0
  }
  /**
   * Construct a SVM object with default parameters
   */
  def this() = this(3, 1.0, 100, 1.0, 1.0)

  protected def createModel(weights: Array[Vector]) = {
    new MSVMModel(weights)
  }

  /**
   * Run the algorithm with the configured parameters on an input
   * RDD of LabeledPoint entries.
   */
  def run(input: RDD[LabeledPoint]): MSVMModel = {
    val numFeatures: Int = input.first().features.size

    val initialWeights: Array[Vector] = {
      val tmpArray = new Array[Vector](numClass)
      tmpArray.map(elem => Vectors.dense(new Array[Double](numFeatures)))
    }
    run(input, initialWeights)
  }

  /**
   * Run the algorithm with the configured parameters on an input RDD
   * of LabeledPoint entries starting from the initial weights provided.
   */
  def run(input: RDD[LabeledPoint], initialWeights: Array[Vector]): MSVMModel = {

    if (validateData && !validators.forall(func => func(input))) {
      throw new SparkException("Input validation failed.")
    }

    val scaler = if (useFeatureScaling) {
      (new StandardScaler).fit(input.map(x => x.features))
    } else {
      null
    }

    // Prepend an extra variable consisting of all 1.0's for the intercept.
    val data: RDD[(Double, Vector)] = if (addIntercept) {
      if (useFeatureScaling) {
        input.map(labeledPoint =>
          (labeledPoint.label, MLUtils.appendBias(scaler.transform(labeledPoint.features))))
      } else {
        input.map(labeledPoint => (labeledPoint.label, MLUtils.appendBias(labeledPoint.features)))
      }
    } else {
      if (useFeatureScaling) {
        input.map(labeledPoint => (labeledPoint.label, scaler.transform(labeledPoint.features)))
      } else {
        input.map(labeledPoint => (labeledPoint.label, labeledPoint.features))
      }
    }

    val initialWeightsWithIntercept: Array[Vector] = if (addIntercept) {
      initialWeights.map(elem => MLUtils.appendBias(elem))
    } else {
      initialWeights
    }

    var weightsWithIntercept = optimizer.optimize(data, initialWeightsWithIntercept)

    if (useFeatureScaling) {
      weightsWithIntercept = weightsWithIntercept.map(elem => scaler.transform(elem))
    }

    createModel(weightsWithIntercept)
  }
}
/**
 * Top-level methods for calling MSVM. NOTE: Labels used in MSVM should be {0, 1,2}.
 */
object MSVMWithSGD {

  /**
   * Train a MSVM model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of gradient descent using the specified step size. Each iteration uses
   * `miniBatchFraction` fraction of the data to calculate the gradient. The weights used in
   * improved gradient descent are initialized using the initial weights provided.
   *
   * NOTE: Labels used in MSVM should be {0, 1,2}.
   *
   * @param input RDD of (label, array of features) pairs.
   * @param numIterations Number of iterations of gradient descent to run.
   * @param stepSize Step size to be used for each iteration of improved gradient descent.
   * @param regParam Regularization parameter.
   * @param miniBatchFraction Fraction of data to be used per iteration.
   * @param initialWeights Initial set of weights to be used. Array should be equal in size to
   *        the number of features in the data.
   */
  def train(
    input: RDD[LabeledPoint],
    numClass: Int,
    numIterations: Int,
    stepSize: Double,
    regParam: Double,
    miniBatchFraction: Double,
    initialWeights: Array[Vector]): MSVMModel = {
    new MSVMWithSGD(numClass, stepSize, numIterations, regParam, miniBatchFraction)
      .run(input, initialWeights)
  }

  /**
   * Train a MSVM model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of improved gradient descent using the specified step size. Each iteration uses
   * `miniBatchFraction` fraction of the data to calculate the gradient.
   * NOTE: Labels used in MSVM should be {0, 1,2}
   *
   * @param input RDD of (label, array of features) pairs.
   * @param numIterations Number of iterations of improved gradient descent to run.
   * @param stepSize Step size to be used for each iteration of gradient descent.
   * @param regParam Regularization parameter.
   * @param miniBatchFraction Fraction of data to be used per iteration.
   */
  def train(
    input: RDD[LabeledPoint],
    numClass: Int,
    numIterations: Int,
    stepSize: Double,
    regParam: Double,
    miniBatchFraction: Double): MSVMModel = {
    new MSVMWithSGD(numClass, stepSize, numIterations, regParam, miniBatchFraction).run(input)
  }

  /**
   * Train a MSVM model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of improved gradient descent using the specified step size.
   * We use the entire data set to
   * update the gradient in each iteration.
   * NOTE: Labels used in SVM should be {0, 1,2}
   *
   * @param input RDD of (label, array of features) pairs.
   * @param stepSize Step size to be used for each iteration of improved Gradient Descent.
   * @param regParam Regularization parameter.
   * @param numIterations Number of iterations of gradient descent to run.
   * @return a MSVMModel which has the weights and offset from training.
   */
  def train(
    input: RDD[LabeledPoint],
    numClass: Int,
    numIterations: Int,
    stepSize: Double,
    regParam: Double): MSVMModel = {
    train(input, numClass, numIterations, stepSize, regParam, 1.0)
  }
  def train(input: RDD[LabeledPoint], numClass: Int, numIterations: Int): MSVMModel = {
    train(input, numClass, numIterations, 1.0, 1.0, 1.0)
  }
  /**
   * Train a MSVM model given an RDD of (label, features) pairs. We run a fixed number
   * of iterations of improved gradient descent using a step size of 1.0. We use the entire data set to
   * update the gradient in each iteration.
   * NOTE: Labels used in MSVM should be {0, 1}
   *
   * @param input RDD of (label, array of features) pairs.
   * @param numIterations Number of iterations of improved gradient descent to run.
   * @return a MSVMModel which has the weights and offset from training.
   */
  def train(input: RDD[LabeledPoint], numIterations: Int): MSVMModel = {
    train(input, 3, numIterations, 1.0, 1.0, 1.0)
  }
}