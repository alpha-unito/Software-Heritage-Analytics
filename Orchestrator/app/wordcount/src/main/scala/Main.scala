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

object WordCount {

  var cumulativeRDD: RDD[(String, Int)] = _

  def main(args: Array[String]): Unit = {

    //Logger.getLogger("org").setLevel(Level.OFF)
    //Logger.getLogger("akka").setLevel(Level.OFF)
    //setStreamingLogLevels()
    Configurator.setRootLevel(Level.WARN)

    val conf = new SparkConf().setAppName("WordCount")
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
    //val spark = SparkSession.builder.getOrCreate()

    val union = ssc.union(rawStreams)

    //val EOSCounter = spark.sparkContext.longAccumulator("EOSCounter")

    union.foreachRDD { rdd =>
      val spark = SparkSession.builder.config(rdd.sparkContext.getConf).getOrCreate()

      val EOSCounter = spark.sparkContext.longAccumulator("EOSCounter")

      import spark.implicits._
      // Create a temporary view
      //wordsDataFrame.createOrReplaceTempView("input_data")
      if (!rdd.isEmpty()) {
         // Convert RDD[String] to DataFrame
        val wordsDataFrame = rdd.toDF("raw_data")
        println("wordsDataFrame: " + wordsDataFrame.getClass.getSimpleName)
        //val wordsDataFrame = rdd.toDF()
        wordsDataFrame.show()

        //val df = spark.read.json(rdd)
        //df.printSchema()
        //df.show()


        val dfJSON = wordsDataFrame.withColumn("json", from_json(col("raw_data"), schema)).select("json.*")
        dfJSON.show()
        println(dfJSON.select(col("project_id")))
        dfJSON.filter(col("project_id").contains("EOS")).show()



        EOSCounter.add(dfJSON.filter(col("project_id").contains("EOS")).count())
        val dfJSON_cleaned = dfJSON.na.drop()


        //dfJSON.foreach(df_row => {

        val dfJSON_decoded = dfJSON_cleaned.withColumn("decoded_data", unbase64(col("data")).cast("string"))
         println("dfJSON_decoded: " + dfJSON_decoded.getClass.getSimpleName)

        dfJSON_decoded.show()

        val decoded_data = dfJSON_decoded.select(col("decoded_data"))
        decoded_data.show()
        println(decoded_data.count())
        println("decoded_data: " + decoded_data.getClass.getSimpleName)

        val words = decoded_data
          .flatMap { row => row.toString().substring(1, row.toString().length-1).split("\n")}
          .flatMap { row => row.toString().split(" ")}

        //words.columns.foreach(println)
        //words.show(30)
        println("words: " + words.getClass.getSimpleName) 


        val pairs = words.map(word => (word, 1))
        //val wordCounts = pairs.groupBy(col("_1")).count()
        val wordCounts = pairs.rdd.reduceByKey(_ + _)    
        
        //pairs.show(30)
        //println(pairs.count())
        //println("pairs: " + pairs.getClass.getSimpleName)
    

        //println("wordCounts: " + wordCounts.getClass.getSimpleName)
        //wordCounts.orderBy($"count".desc).show(100)
        
        //wordCounts.sortBy(-_._2).toDF().show(100)
        println("Number of words:" + wordCounts.count())
        //wordCounts.show(100)
        
        if (cumulativeRDD == null){
          cumulativeRDD = wordCounts
        }else{
          val ret = cumulativeRDD.fullOuterJoin(wordCounts)
          cumulativeRDD = ret.map[(String, Int)]({
            vv: (String, (Option[Int], Option[Int])) =>  {(vv._1, vv._2._1.getOrElse(0) + vv._2._2.getOrElse(0))}
          })
        }
        
        //cumulativeRDD.foreach(println)
        cumulativeRDD.sortBy(-_._2).toDF().show(100)
      
      

       
        println("EOSCounter " + EOSCounter.value)
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
