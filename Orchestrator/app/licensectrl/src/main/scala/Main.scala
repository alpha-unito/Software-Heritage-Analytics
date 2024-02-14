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
//import org.apache.spark.internal.Logging

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

import Utils._


object licensectrl {

  val licenseGraph = new DirectedGraph()

  var logger = LogManager.getLogger(getClass().getName());

  var cumulativeRDD: RDD[(String, Int)] = _

  def main(args: Array[String]): Unit = {

    println(
      s"arg0 = ${args(0)}, arg1 = ${args(1)}, arg2 = ${args(2)}, arg3 = ${args(3)}, arg4 = ${args(4)} "
    )

    val scancode_exe_path = args(5)
    val scancode_index_file = args(6)
    val ramdisk_path = args(7)
    val graph_path = args(8)
    val output_path = args(9)

    licenseGraph.loadFromFile(graph_path)

    println(licenseGraph)

    Configurator.setRootLevel(Level.FATAL)

    val conf = new SparkConf().setAppName("licensectrl")

    // Print all the configuration settings
    val configSettings = conf.getAll
    println("SparkConf Settings:")
    configSettings.foreach { case (key, value) =>
      println(s"$key: $value")
    }

    val ssc = new StreamingContext(conf, Seconds(Integer.parseInt(args(3))))
    // "/beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/spark_output/"
    val outputPath =output_path + args(4)

    // Get the SparkContext's statusTracker
    val statusTracker = ssc.sparkContext.statusTracker
    // Get application id
    val id = ssc.sparkContext.applicationId

    println(s"spark application id: application_$id")

    // Create a DStream that will connect to hostname:port, like localhost:9999
    println(
      "Connect to " + args(1) + ":" + args(2) + " " + Integer.parseInt(args(2))
    )

    val schema = new StructType()
      .add("project_id", StringType, nullable = true)
      .add("file_basename", StringType, nullable = true)
      .add("data", StringType, nullable = true)
      .add("path", StringType, nullable = true)

    val schemaMap = StructType(
      Array(
        StructField("project_id", StringType, nullable = true),
        StructField("file_basename", StringType, nullable = true),
        StructField("license", StringType, nullable = true),
        StructField("category", StringType, nullable = true),
        StructField("path", StringType, nullable = true)
      )
    )
    val rawStreams = (1 to args(0).toInt).map(_ =>
      ssc.socketTextStream(
        args(1),
        Integer.parseInt(args(2)),
        StorageLevel.MEMORY_ONLY
      )
    )

    val spark = SparkSession.builder().getOrCreate()

    val filePath = scancode_index_file
    // "/beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/app/licensectrl/src/main/scala/scancode_index.json"
    val licenseMap: Map[String, String] = readJsonFileToMap(filePath, spark)

    // licenseMap.foreach { case (key, value) =>
    //   // println(s"Key: $key, Value: $value")
    //   logger.fatal(s"Key: $key, Value: $value")
    // }

    val union = ssc.union(rawStreams)

    val EOSCounter = spark.sparkContext.longAccumulator("EOSCounter")
    val EMPTYCounter = spark.sparkContext.longAccumulator("EMPTYCounter")
    var was_empty: Boolean = false
    // val streamCounters = new Array[Int](args(0).toInt)
    // val currentStreamIndex = new java.util.concurrent.atomic.AtomicInteger(0) // Variabile condivisa per tenere traccia dello stream corrente

    // TODO: Path salvataggio file (Da settare al lancio)
    val file_path = ramdisk_path
    // "/beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/ramdisk"

    var broadcastVar = spark.sparkContext.broadcast(Seq.empty[Row])

    union.foreachRDD { rdd =>
      logger.info("NEW RDD")
      if (!rdd.isEmpty()) {
        was_empty = false
        EMPTYCounter.reset()
        logger.info("NOT EMPTY RDD")

        val rowRDD = rdd.map { item =>
          var project_id = ""
          var file_withpath = ""
          var file_basename = ""
          var data = ""
          try {
            var json = parse(item).asInstanceOf[JObject]
            project_id =
              compact(render((json \ "project_id"))).replace("\"", "")
            file_withpath =
              compact(render((json \ "file_name"))).replace("\"", "")
            file_basename =
              compact(render((json \ "file_basename"))).replace("\"", "")
            data = compact(render((json \ "data"))).replace("\"", "")

            if (project_id == "EOS") {
              EOSCounter.add(1)
            }
          } catch {
            case e: Exception =>
              logger.info(s"ITEM: $item")
              logger.info(s"EXCEPTION: $e")
          }
          Row(project_id, file_withpath, file_basename, data)
        }

        // new dataframe
        val rddDataFrameToClean = spark.createDataFrame(rowRDD, schema)
        // rddDataFrameToClean.show()
        val rddDataFrame = rddDataFrameToClean
          .where(!col("project_id").contains("EOS"))
          .where(!col("project_id").contains("WAIT"))
        // rddDataFrame.show()
        if (rddDataFrame.count().toInt > 0) {

          // rddDataFrame.show()

          val rddData = rddDataFrame.rdd.map { row =>
            import org.apache.spark.sql.functions._
            import spark.implicits._
            // Get data from row
            val project_id = row.getString(0)
            val file_withpath = row.getString(1)
            val file_basename = row.getString(2)
            val data_64 = row.getString(3)

            val file_name = generateSHA1(project_id + "_" + file_withpath)

            // Decode base 64
            val data_bytes = Base64.getDecoder.decode(data_64)
            val data = new String(data_bytes, "UTF-8")

            // New file
            val new_file = s"$file_path/$file_name"

            // Write file, necessary for scancode
            val outputFile = new File(new_file)
            val writer = new BufferedWriter(new FileWriter(outputFile, true))
            writer.write(data)
            writer.close()

            // Get license
            val json_file = s"$file_path/$file_name.json"
            var file_exists = Files.exists(Paths.get(json_file))

            if (!file_exists) {
            //"/beegfs/home/gspinate/slurm_tools/scancode-toolkit/scancode",
              val command = Seq(
                scancode_exe_path,
                "-clpeui",
                "-q",
                "-n",
                "1",
                "--json-pp",
                json_file,
                new_file
              )
              val exitCode = command.!
            }

            var license = ""
            var category = "Unstated License"

            val file_content =
              Source.fromFile(json_file).getLines().mkString("\n")

            val file_content_json = parse(file_content).asInstanceOf[JObject]

            // Get "license_detections" as JValue Object
            var license_detections: JArray = JArray(Nil)

            try {
              license_detections =
                (file_content_json \ "license_detections").asInstanceOf[JArray]
            } catch {
              case e: Exception =>
                logger.fatal(s"ECCEZIONE: $e")
            }

            // Get "license_expression" from "license_detections"
            val licenses = license_detections.arr.map { obj =>
              (obj \ "license_expression").asInstanceOf[JString].s
            }

            if (licenses.nonEmpty) {
              license = licenses(0)
              val found: Option[String] = licenseMap.get(license)

              found match {
                case Some(value) => category = value
                case None        => category = "Unknown"
              }
            }
            // }

            Row(project_id, file_basename, license, category, file_withpath)
          }

          // Get value from broadcast variable
          val existingData = broadcastVar.value

          // Merge data
          val combinedData = existingData ++ rddData.collect()

          // Update broadcast variable with new data
          broadcastVar.unpersist() // Remove existing data from cache
          broadcastVar = spark.sparkContext.broadcast(combinedData)

        }
      } else {
        logger.info("RDD IS EMPTY")
        if (!was_empty) {
          was_empty = true
        } else {
          EMPTYCounter.add(1)
        }
        if (EOSCounter.value >= args(0).toInt || EMPTYCounter.value < 0) {
          logger.info(
            s"Los treaming è terminato, sono arrivati ${broadcastVar.value.length} files. Start reduce"
          )
          val broadcastValue = broadcastVar.value
          val completeDF = spark
            .createDataFrame(
              spark.sparkContext.parallelize(broadcastValue),
              schemaMap
            )
            .toDF("project_id", "file_basename", "license", "category", "path")

          import org.apache.spark.sql.expressions.Window
          import org.apache.spark.sql.functions._

          val declared_license_files = List(
            "LICENSE",
            "LICENSE.txt",
            "COPYING",
            "COPYING.TXT",
            "NOTICE",
            "README",
            "README.md"
          )

          // Filtering orginal DataFrame to find declared licenses
          val filteredDeclaredData: DataFrame = completeDF.filter(
            col("file_basename").isin(declared_license_files: _*)
          )

          val declaredLicenseDF = filteredDeclaredData
            .groupBy("project_id")
            .agg(
              collect_set(struct(col("license"), col("category")))
                .as("project_declared_licenses"),
              count("*").as("n_project_declared_licenses"),
              collect_set("category").as("distinct_category_declared"),
              collect_set("license").as("distinct_license_declared")
            )

          val filteredLicenseDF: DataFrame = completeDF.filter(
            !col("file_basename").isin(declared_license_files: _*)
          )

          val incodeLicenseDF = filteredLicenseDF
            .select("project_id", "license", "category")
            .distinct()
            .groupBy("project_id")
            .agg(
              collect_list(struct(col("license"), col("category")))
                .as("project_incode_licenses"),
              collect_set("category").as("distinct_category_incode"),
              collect_set("license").as("distinct_license_incode")
            )

          val incodeTotLicenseDF = filteredLicenseDF
            .filter(col("license") =!= "")
            .groupBy("project_id")
            .agg(
              collect_list("license").as("tot_license")
            )

          // Count of different expression types and categories for each project_id
          val risultatoFinale = completeDF
            .groupBy("project_id")
            .agg(
              count("*").as("n_project_files")
            )

          // Make join between completeDFWithLicenseType and result using "project_id" as join key
          val risultatoUnione = risultatoFinale
            .join(declaredLicenseDF, Seq("project_id"), "left")
            .join(incodeLicenseDF, Seq("project_id"), "left")
            .join(incodeTotLicenseDF, Seq("project_id"), "left")
            .withColumn("n_project_incode_licenses", size(col("tot_license")))

          val updatedDF = risultatoUnione
            .withColumn(
              "n_project_declared_licenses",
              when(
                col("n_project_declared_licenses").isNull || col(
                  "n_project_declared_licenses"
                ) === -1,
                0
              ).otherwise(col("n_project_declared_licenses"))
            )
            .withColumn(
              "project_declared_licenses",
              when(col("project_declared_licenses").isNull, array()).otherwise(
                col("project_declared_licenses")
              )
            )
            .withColumn(
              "n_project_incode_licenses",
              when(
                col("n_project_incode_licenses").isNull || col(
                  "n_project_incode_licenses"
                ) === -1,
                0
              ).otherwise(col("n_project_incode_licenses"))
            )
            .withColumn(
              "project_incode_licenses",
              when(col("project_incode_licenses").isNull, array()).otherwise(
                col("project_incode_licenses")
              )
            )
            .withColumn(
              "distinct_category_declared",
              when(col("distinct_category_declared").isNull, array())
                .otherwise(col("distinct_category_declared"))
            )
            .withColumn(
              "distinct_category_incode",
              when(col("distinct_category_incode").isNull, array())
                .otherwise(col("distinct_category_incode"))
            )
            .withColumn(
              "distinct_license_declared",
              when(col("distinct_license_declared").isNull, array())
                .otherwise(col("distinct_license_declared"))
            )
            .withColumn(
              "distinct_license_incode",
              when(col("distinct_license_incode").isNull, array())
                .otherwise(col("distinct_license_incode"))
            )

          // updatedDF.show()

          // Define a UDF that uses your findInconsistency function.
          val findInconsistencyUDF =
            udf((lista1: List[String], lista2: List[String]) => {
              val out: List[(String, String, Boolean, Boolean, Boolean)] =
                findInconsistency(lista1, lista2)
              out
            })
          val findInconsistencySelfUDF = udf((lista1: List[String]) => {
            val out: List[(String, String, Boolean, Boolean, Boolean)] =
              findInconsistency(lista1)
            out
          })

          // Define a UDF that uses your findConflict function.
          val findConflictUDF =
            udf((lista1: List[String], lista2: List[String]) => {
              val out: List[(String, String)] =
                findConflict(lista1, lista2, licenseGraph)
              out
            })
          val findConflictSelfUDF = udf((lista1: List[String]) => {
            val out: List[(String, String)] = findConflict(lista1, licenseGraph)
            out
          })

          val aggragetedAnalisys = updatedDF
            .withColumn(
              "between_declared_inconsistencies",
              findInconsistencySelfUDF(col("distinct_category_declared"))
            )
            .withColumn(
              "between_incode_inconsistencies",
              findInconsistencySelfUDF(col("distinct_category_incode"))
            )
            .withColumn(
              "between_declared_incode_inconsistencies",
              findInconsistencyUDF(
                col("distinct_category_declared"),
                col("distinct_category_incode")
              )
            )
            .withColumn(
              "between_declared_conflicts",
              findConflictSelfUDF(col("distinct_license_declared"))
            )
            .withColumn(
              "between_incode_conflicts",
              findConflictSelfUDF(col("distinct_license_incode"))
            )
            .withColumn(
              "between_declared_incode_conflicts",
              findConflictUDF(
                col("distinct_license_declared"),
                col("distinct_license_incode")
              )
            )

          saveOnFile(aggragetedAnalisys, outputPath)

          ssc.stop(false)
          System.exit(0)
          logger.info("Application END")
        }
      }
    }

    ssc.start() // Start the computation
    ssc.awaitTermination() // Wait for the computation to terminate
    System.exit(0)
  }
}
