package ict.spark.mllib.linalg

import org.apache.spark.mllib.linalg._
import com.github.fommil.netlib.{ BLAS => NetlibBLAS, F2jBLAS }
import scala.collection.mutable.ArrayBuffer

/**
 * BLAS routines for MLlib's vectors and matrices.
 */
//private[mllib]
object MyBLAS extends Serializable {

  @transient private var _f2jBLAS: NetlibBLAS = _

  // For level-1 routines, we use Java implementation.
  private def f2jBLAS: NetlibBLAS = {
    if (_f2jBLAS == null) {
      _f2jBLAS = new F2jBLAS
    }
    _f2jBLAS
  }

  /**
   * y += a * x
   */
  def axpy(a: Double, x: Vector, y: Vector): Unit = {
    require(x.size == y.size)
    y match {
      case dy: DenseVector =>
        x match {
          case sx: SparseVector =>
            axpy(a, sx, dy)
          case dx: DenseVector =>
            axpy(a, dx, dy)
          case _ =>
            throw new UnsupportedOperationException(
              s"axpy doesn't support x type ${x.getClass}.")
        }
      case _ =>
        throw new IllegalArgumentException(
          s"axpy only supports adding to a dense vector but got type ${y.getClass}.")
    }
  }

  /**
   * y += a * x
   */
  private def axpy(a: Double, x: DenseVector, y: DenseVector): Unit = {
    val n = x.size
    f2jBLAS.daxpy(n, a, x.values, 1, y.values, 1)
  }

  /**
   * y += a * x
   */
  private def axpy(a: Double, x: SparseVector, y: DenseVector): Unit = {
    val nnz = x.indices.size
    if (a == 1.0) {
      var k = 0
      while (k < nnz) {
        y.values(x.indices(k)) += x.values(k)
        k += 1
      }
    } else {
      var k = 0
      while (k < nnz) {
        y.values(x.indices(k)) += a * x.values(k)
        k += 1
      }
    }
  }

  def add(x: Vector, y: Vector): Vector = {
    require(x.size == y.size)
    y match {
      case dy: DenseVector =>
        x match {
          case dx: DenseVector =>
            add(dx, dy)
          case _ =>
            throw new UnsupportedOperationException(
              s"axpy doesn't support x type ${x.getClass}.")
        }
      case sy: SparseVector =>
        x match {
          case sx: SparseVector =>
            add(sx, sy)
          case _ =>
            throw new UnsupportedOperationException(
              s"axpy doesn't support x type ${x.getClass} and y type ${y.getClass}.")
        }

    }
  }
  private def add(x: DenseVector, y: DenseVector) = {
    val n = x.size
    f2jBLAS.daxpy(n, 1, x.values, 1, y.values, 1)
    y
  }
  private def add(x: SparseVector, y: SparseVector) = {
    val xIndicesSize = x.indices.size
    val yIndicesSize = y.indices.size

    var resultIndices: ArrayBuffer[Int] = ArrayBuffer[Int]()
    var resultValues: ArrayBuffer[Double] = ArrayBuffer[Double]()

    var i = 0
    var j = 0
    var xIndices = 0
    var yIndices = 0

    while (i < xIndicesSize && j < yIndicesSize) {
      xIndices = x.indices(i)
      yIndices = y.indices(j)
      if (xIndices < yIndices) {
        resultIndices += xIndices
        resultValues += x.values(i)
        i += 1
      } else if (xIndices > yIndices) {
        resultIndices += yIndices
        resultValues += y.values(j)
        j += 1
      } else {
        resultIndices += xIndices
        resultValues += (x.values(i) + y.values(j))
        i += 1
        j += 1
      }
    }
    while (i < xIndicesSize) {
      resultIndices += x.indices(i)
      resultValues += x.values(i)
      i += 1
    }
    while (j < yIndicesSize) {
      resultIndices += y.indices(j)
      resultValues += y.values(j)
      j += 1
    }
    var resultSparseVector = Vectors.sparse(y.size, resultIndices.toArray, resultValues.toArray)
    resultSparseVector
  }
  /**
   * dot(x, y)
   */
  def dot(x: Vector, y: Vector): Double = {
    require(x.size == y.size)
    (x, y) match {
      case (dx: DenseVector, dy: DenseVector) =>
        dot(dx, dy)
      case (sx: SparseVector, dy: DenseVector) =>
        dot(sx, dy)
      case (dx: DenseVector, sy: SparseVector) =>
        dot(sy, dx)
      case (sx: SparseVector, sy: SparseVector) =>
        dot(sx, sy)
      case _ =>
        throw new IllegalArgumentException(s"dot doesn't support (${x.getClass}, ${y.getClass}).")
    }
  }

  /**
   * dot(x, y)
   */
  private def dot(x: DenseVector, y: DenseVector): Double = {
    val n = x.size
    f2jBLAS.ddot(n, x.values, 1, y.values, 1)
  }

  /**
   * dot(x, y)
   */
  private def dot(x: SparseVector, y: DenseVector): Double = {
    val nnz = x.indices.size
    var sum = 0.0
    var k = 0
    while (k < nnz) {
      sum += x.values(k) * y.values(x.indices(k))
      k += 1
    }
    sum
  }

  /**
   * dot(x, y)
   */
  private def dot(x: SparseVector, y: SparseVector): Double = {
    var kx = 0
    val nnzx = x.indices.size
    var ky = 0
    val nnzy = y.indices.size
    var sum = 0.0
    // y catching x
    while (kx < nnzx && ky < nnzy) {
      val ix = x.indices(kx)
      while (ky < nnzy && y.indices(ky) < ix) {
        ky += 1
      }
      if (ky < nnzy && y.indices(ky) == ix) {
        sum += x.values(kx) * y.values(ky)
        ky += 1
      }
      kx += 1
    }
    sum
  }

  /**
   * y = x
   */
  def copy(x: Vector, y: Vector): Unit = {
    val n = y.size
    require(x.size == n)
    y match {
      case dy: DenseVector =>
        x match {
          case sx: SparseVector =>
            var i = 0
            var k = 0
            val nnz = sx.indices.size
            while (k < nnz) {
              val j = sx.indices(k)
              while (i < j) {
                dy.values(i) = 0.0
                i += 1
              }
              dy.values(i) = sx.values(k)
              i += 1
              k += 1
            }
            while (i < n) {
              dy.values(i) = 0.0
              i += 1
            }
          case dx: DenseVector =>
            Array.copy(dx.values, 0, dy.values, 0, n)
        }
      case _ =>
        throw new IllegalArgumentException(s"y must be dense in copy but got ${y.getClass}")
    }
  }

  /**
   * x = a * x
   */
  def scal(a: Double, x: Vector): Unit = {
    x match {
      case sx: SparseVector =>
        f2jBLAS.dscal(sx.values.size, a, sx.values, 1)
      case dx: DenseVector =>
        f2jBLAS.dscal(dx.values.size, a, dx.values, 1)
      case _ =>
        throw new IllegalArgumentException(s"scal doesn't support vector type ${x.getClass}.")
    }
  }
}