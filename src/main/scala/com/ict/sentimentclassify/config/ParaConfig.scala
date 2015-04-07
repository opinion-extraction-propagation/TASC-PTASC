package com.ict.sentimentclassify.config

case class ParaConfig(
  partitions: Int = 8,
  maxIteration: Int = 100,
  configXML: String = "config.xml")