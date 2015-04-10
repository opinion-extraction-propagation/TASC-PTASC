package com.ict.sentimentclassify.application

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.Logger
import com.ict.sentimentclassify.config.ConfigReader
import com.ict.sentimentclassify.classify.AdaptiveCotrainClassify
import com.ict.sentimentclassify.config.ParaConfig

object AdaptiveCotrainMain {
  val logger = Logger.getLogger(AdaptiveCotrainMain.toString());
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SimpleMSVM Application").setMaster("local[4]")
    val sc = new SparkContext(conf)
    //    val configFile = "config.xml"

    val parser = new scopt.OptionParser[ParaConfig]("AdaptiveCotrain") {
      head("AdaptiveCotrainMain", "1.0")
      opt[Int]("partitions") optional () action { (x, c) =>
        c.copy(partitions = x)
      } validate { x => if (x > 0) success else failure("Option --partitions must be >0")
      } text ("partitions is the min number of RDD's split parts,default is 2")

      opt[Int]("maxIteration") required () action { (x, c) =>
        c.copy(maxIteration = x)
      } validate { x => if (x > 0) success else failure("Option --maxIteration must be >0")
      } text ("maxIteration is the max iteration of SVM")

      opt[String]("configXML") required () action { (x, c) =>
        c.copy(configXML = x)
      } text ("configXML is config.xml of the app")

    }
    // parser.parse returns Option[Config]
    val para = parser.parse(args, ParaConfig()).get

    val partitions: Int = para.partitions
    val maxIteration: Int = para.maxIteration
    val configFile: String = para.configXML

    val adaptiveCotrainClassify: AdaptiveCotrainClassify = new AdaptiveCotrainClassify(sc, configFile, maxIteration, partitions)

    if (false == adaptiveCotrainClassify.init()) {
      logger.error("Adaptivecotrain initialize error");
      return ;
    }
    logger.info("Adaptivecotrain algorithm start");
    adaptiveCotrainClassify.start()

    logger.info("Adaptivecotrain algorithm stop");
  }

}