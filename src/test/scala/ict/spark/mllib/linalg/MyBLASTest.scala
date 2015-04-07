package ict.spark.mllib.linalg

import org.apache.spark.mllib.linalg.Vectors
import ict.spark.mllib.linalg.MyBLAS._
object MyBLASTest {

  def main(args: Array[String]): Unit = {
    val vectorA = Vectors.sparse(5, Array(0, 2), Array(1.0, 2.0))
    val vectorB = Vectors.sparse(5, Array(0, 3), Array(4.0, 8.0))
    val vecC = add(vectorA, vectorB)
    // (5,[0,2,3],[5.0,2.0,8.0])
    println(vecC.toString())
  }

}