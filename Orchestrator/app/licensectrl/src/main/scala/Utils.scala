import org.apache.spark._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.rdd.RDD

import org.apache.spark.sql.{Row, SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.streaming._
import org.apache.spark.{SparkConf, SparkContext}

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.Level
import org.apache.log4j.PropertyConfigurator
import org.apache.logging.log4j.core.config.Configurator

import java.net.Socket
import java.nio.charset.StandardCharsets
import java.io._
import java.util.Base64

import scala.sys.process._
import scala.language.postfixOps
import scala.io.Source

import org.json4s._
import org.json4s.JValue
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.JObject

import java.nio.file.{Paths, Files}

import graph.DirectedGraph

import java.security.MessageDigest

import java.lang.management.ManagementFactory

import java.nio.file.StandardCopyOption._

object Utils {
  def sendResults(rdd: RDD[(String, Int)]): Unit = {
    if (!rdd.isEmpty()) {
      val sock = new Socket("localhost", 9998)
      sock.getOutputStream.write(
        rdd.collect().toString.getBytes(StandardCharsets.UTF_8)
      )
      sock.close()
    }
  }

  def readJsonFileToMap(
      filePath: String,
      spark: SparkSession
  ): Map[String, String] = {
    import scala.collection.mutable.Map
    val jsonStringsRDD = Source.fromFile(filePath).getLines().mkString("\n")
    val json =
      parse(jsonStringsRDD).asInstanceOf[JArray].arr.asInstanceOf[Seq[JObject]]

    val resultMap = json.map { item =>
      val license_key = (item \ "license_key").asInstanceOf[JString].s
      val category = (item \ "category").asInstanceOf[JString].s
      (license_key, category)
    }.toMap

    resultMap
  }

  def extractUniqueCategories(
      df: DataFrame,
      spark: SparkSession
  ): Set[String] = {
    val categorySet = df
      .select("category")
      .distinct()
      .collect()
      .map(row => row.getString(0))
      .toSet
    categorySet
  }

  def extractUniqueLicense(df: DataFrame, project_id: String): DataFrame = {
    val filtroProjectID = df.filter(col("project_id") === project_id)

    val espressioniDistinct = filtroProjectID.select("license").distinct()

    espressioniDistinct
  }

  def findInconsistency(
      lista1: List[String],
      lista2: List[String]
  ): List[(String, String, Boolean, Boolean, Boolean)] = {
    var listaOutput: Set[(String, String, Boolean, Boolean, Boolean)] = Set()
    var hybridInconsistency = false
    var unstatedLicense = false
    var inconsistency = false
    if (lista1.length > 0 && lista2.length > 0) {
      for (i <- 0 until lista1.length) {
        hybridInconsistency = false
        unstatedLicense = false
        inconsistency = false
        for (j <- 0 until lista2.length) {
          val licenseType1 = lista1(i)
          val licenseType2 = lista2(j)
          val FREE_LICENSES = Set(
            "Public Domain",
            "Permissive",
            "Copyleft Limited",
            "Copyleft",
            "Proprietary Free"
          )
          val NON_FREE_LICENSES = Set(
            "Commercial",
            "Source-available",
            "Patent License",
            "Free Restricted"
          )

          if (
            licenseType1 == "Unstated License" || licenseType2 == "Unstated License"
          ) {
            unstatedLicense = true
          }

          if (
            licenseType1 == "Public Domain" || licenseType2 == "Public Domain"
          ) {
            inconsistency = false
          } else {
            if (
              FREE_LICENSES.contains(licenseType1) && FREE_LICENSES.contains(
                licenseType2
              ) ||
              NON_FREE_LICENSES.contains(licenseType1) && NON_FREE_LICENSES
                .contains(licenseType2)
            ) {
              inconsistency = true
            } else {
              hybridInconsistency = true
            }
          }
          val tupla =
            if (licenseType1 < licenseType2)
              (
                licenseType1,
                licenseType2,
                inconsistency,
                hybridInconsistency,
                unstatedLicense
              )
            else
              (
                licenseType2,
                licenseType1,
                inconsistency,
                hybridInconsistency,
                unstatedLicense
              )
          listaOutput += tupla
        }
      }
    }
    listaOutput.toList
  }

  def findConflict(
      lista1: List[String],
      lista2: List[String],
      licenseGraph: DirectedGraph
  ): List[(String, String)] = {
    var listaOutput: Set[(String, String)] = Set()
    if (lista1.length > 0 && lista2.length > 0) {
      for (i <- 0 until lista1.length) {
        for (j <- 0 until lista2.length) {
          val license1 = lista1(i)
          val license2 = lista2(j)
          val areOnSamePath =
            licenseGraph.areNodesOnSamePath(license1, license2)

          if (!areOnSamePath) {
            val coppia =
              if (license1 < license2) (license1, license2)
              else (license2, license1)
            listaOutput += coppia
          }
        }
      }
    }
    listaOutput.toList
  }

  def findInconsistency(
      lista: List[String]
  ): List[(String, String, Boolean, Boolean, Boolean)] = {
    var listaOutput: List[(String, String, Boolean, Boolean, Boolean)] = List()
    var hybridInconsistency = false
    var unstatedLicense = false
    var inconsistency = false
    if (lista.length > 0) {
      for (i <- 0 until lista.length) {
        hybridInconsistency = false
        unstatedLicense = false
        inconsistency = false
        for (j <- i + 1 until lista.length) {
          val licenseType1 = lista(i)
          val licenseType2 = lista(j)
          val FREE_LICENSES = Set(
            "Public Domain",
            "Permissive",
            "Copyleft Limited",
            "Copyleft",
            "Proprietary Free"
          )
          val NON_FREE_LICENSES = Set(
            "Commercial",
            "Source-available",
            "Patent License",
            "Free Restricted"
          )

          if (
            licenseType1 == "Unstated License" || licenseType2 == "Unstated License"
          ) {
            unstatedLicense = true
          }

          if (
            licenseType1 == "Public Domain" || licenseType2 == "Public Domain"
          ) {
            inconsistency = false
          } else {
            if (
              FREE_LICENSES.contains(licenseType1) && FREE_LICENSES.contains(
                licenseType2
              ) ||
              NON_FREE_LICENSES.contains(licenseType1) && NON_FREE_LICENSES
                .contains(licenseType2)
            ) {
              inconsistency = true
            } else {
              hybridInconsistency = true
            }
          }
          val tupla = (
            licenseType1,
            licenseType2,
            inconsistency,
            hybridInconsistency,
            unstatedLicense
          )
          listaOutput = tupla :: listaOutput
        }
      }
    }
    listaOutput
  }

  def findConflict(
      lista: List[String],
      licenseGraph: DirectedGraph
  ): List[(String, String)] = {
    var listaOutput: List[(String, String)] = List()
    if (lista.length > 0) {
      for (i <- 0 until lista.length) {
        for (j <- i + 1 until lista.length) {
          val license1 = lista(i)
          val license2 = lista(j)
          val areOnSamePath =
            licenseGraph.areNodesOnSamePath(license1, license2)
          if (!areOnSamePath) {
            var tupla = (license1, license2)
            listaOutput = tupla :: listaOutput
          }
        }
      }
    }
    listaOutput
  }

  def fakeConfilct(lista: List[String]): List[(String, String)] = {
    List(
      ("stringa1", "stringa2"),
      ("stringa3", "stringa4"),
      ("stringa5", "stringa6")
    )
  }

  def logDataFrame(df: DataFrame, logger: Logger): Unit = {
    import org.apache.logging.log4j.Logger;
    df.foreach { row =>
      val rowAsString = row.mkString(", ")
      logger.info(
        rowAsString
      ) 
    }
  }

  def saveOnFile(df: DataFrame, outputPath: String) = {
    val dfToSave = df.select(
      "project_id",
      "n_project_files",
      "n_project_declared_licenses",
      "n_project_incode_licenses",
      "project_declared_licenses",
      "project_incode_licenses",
      "between_declared_inconsistencies",
      "between_incode_inconsistencies",
      "between_declared_incode_inconsistencies",
      "between_declared_conflicts",
      "between_incode_conflicts",
      "between_declared_incode_conflicts"
    )

    println(s"Abbiamo ${dfToSave.count()} da salvare!!")
    dfToSave.show()
    dfToSave.write.format("json").mode("overwrite").save(outputPath)
  }

  def generateSHA1(input: String): String = {
    val md = MessageDigest.getInstance("SHA-1")
    val byteArray = md.digest(input.getBytes("UTF-8"))

    val sb = new StringBuilder
    byteArray.foreach { byte =>
      sb.append(String.format("%02x", byte & 0xff))
    }

    sb.toString
  }
}
