import org.apache.spark._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, from_json, unbase64}
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}
import org.apache.spark.streaming._

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.apache.spark.internal.Logging

import java.net.Socket
import java.nio.charset.StandardCharsets

import scala.sys.process._
import scala.language.postfixOps

object NetworkGrep {

  var cumulativeRDD: RDD[(String, Int)] = _

  def main(args: Array[String]): Unit = {

    //Logger.getLogger("org").setLevel(Level.OFF)
    //Logger.getLogger("akka").setLevel(Level.OFF)
    //setStreamingLogLevels()
    Configurator.setRootLevel(Level.WARN)

    val conf = new SparkConf().setAppName("NetworkGrep")
    val ssc = new StreamingContext(conf, Seconds( Integer.parseInt(args(3)) ))
    //ssc.checkpoint("checkpoint-folder")
    //ssc.sparkContext.setLogLevel("FATAL")



    // Create a DStream that will connect to hostname:port, like localhost:9999
    //val lines = ssc.socketTextStream("131.114.3.199", 9999)
    println("Connect to " + args(1) + ":"+args(2)+ " " + Integer.parseInt(args(2))  )
    //val lines = ssc.socketTextStream(args(1), Integer.parseInt(args(2)))

    val schema = new StructType()
      .add("project_id", StringType, nullable = true)
      .add("file_basename", StringType, nullable = true)
      .add("file_name", StringType, nullable = true)
      .add("file_type", StringType, nullable = true)
      .add("data", StringType, nullable = true)


    //val rawStreams = (1 to Integer.parseInt(args(0))).map(_ =>
                      //issc.rawSocketStream[String](args(1), Integer.parseInt(args(2)), StorageLevel.MEMORY_ONLY_SER_2)).toArray
    val rawStreams = (1 to args(0).toInt).map(_ =>
                      ssc.socketTextStream(args(1), Integer.parseInt(args(2)), StorageLevel.MEMORY_ONLY_SER_2))
    val spark = SparkSession.builder.getOrCreate()

    val union = ssc.union(rawStreams)

    val EOSCounter = spark.sparkContext.longAccumulator("EOSCounter")

    union.foreachRDD { rdd =>
      //val spark = SparkSession.builder.config(rdd.sparkContext.getConf).getOrCreate()

      //val EOSCounter = spark.sparkContext.longAccumulator("EOSCounter")

      import spark.implicits._
      // Create a temporary view
      if (!rdd.isEmpty()) {
         // Convert RDD[String] to DataFrame
        val wordsDataFrame = rdd.toDF("raw_data")

        val dfJSON = wordsDataFrame.withColumn("json", from_json(col("raw_data"), schema)).select("json.*")


        EOSCounter.add(dfJSON.filter(col("project_id").contains("EOS")).count())
        val dfJSON_cleaned = dfJSON.na.drop()


        val dfJSON_decoded = dfJSON_cleaned.withColumn("decoded_data", unbase64(col("data")).cast("string"))

        dfJSON_decoded.show()

        val cmd = "/home/ubuntu/scancode-toolkit/scancode -clpeui -q -n 1 --json-pp  /home/ubuntu/scancode-toolkit/sample3.json /home/ubuntu/test_git_project/fastflow/ff/all2all.hpp"
        val output = cmd!!

      
        println("output:" + output)

        val decoded_data = dfJSON_decoded.select(col("project_id"),col("file_basename"),col("decoded_data"))



        val result = decoded_data.filter(col("decoded_data").contains("Lorem"))
        result.show()
       
        /*
        if (cumulativeRDD == null){
          cumulativeRDD = result.rdd
        }else{
          val ret = cumulativeRDD.fullOuterJoin(result.rdd)
          cumulativeRDD = ret.map[(String, Int)]({
            vv: (String, (Option[Int], Option[Int])) =>  {(vv._1, vv._2._1.getOrElse(0) + vv._2._2.getOrElse(0))}
          })
        }
        cumulativeRDD.foreach(println)
        */

        /*
        val words = decoded_data
          .flatMap { row => row.toString().substring(1, row.toString().length-1).split("\n")}
          .flatMap { row => row.toString().split(" ")}

        val pairs = words.map(word => (word, 1))
        
        val wordCounts = pairs.groupBy(col("_1")).count()

        wordCounts.show()
        */

        // Test end of all streams     
        if ( EOSCounter.value == args(0).toInt ) {

        //if ( dfJSON.filter(col("project_id").contains("EOS") ) {i
            println("EOS - App exit")
            //Thread.sleep(Integer.parseInt(args(2))*2)
            //irdd.context.stop()
            ssc.stop(true)
            System.exit(0)


        }


      } else {
        println("RDD empty")
      }
    }


    //println("wordcount END Computation ")
    ssc.start() // Start the computation
    ssc.awaitTermination() // Wait for the computation to terminate
    
  
  }


  def sendResults(rdd: RDD[(String, Int)]): Unit = {
    if (!rdd.isEmpty()) {
      // Connessione al socket
      val sock = new Socket("localhost", 9998)
      // Invia i risultati
      sock.getOutputStream.write(rdd.collect().toString.getBytes(StandardCharsets.UTF_8))
      // Chiudi la connessione
      sock.close()
    }
  }

}
