import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, from_json, unbase64}
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}
import org.apache.spark.streaming._


object Main {

  var cumulativeRDD: RDD[(String, Int)] = _

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
    val ssc = new StreamingContext(conf, Seconds(2))
    ssc.checkpoint("checkpoint-folder")
    ssc.sparkContext.setLogLevel("FATAL")

    // Create a DStream that will connect to hostname:port, like localhost:9999
    //val lines = ssc.socketTextStream("131.114.3.199", 9999)
    val lines = ssc.socketTextStream("localhost", 9999)

    val schema = new StructType()
      .add("project_id", IntegerType, nullable = true)
      .add("file_basename", StringType, nullable = true)
      .add("file_name", StringType, nullable = true)
      .add("file_type", StringType, nullable = true)
      .add("data", StringType, nullable = true)

    lines.foreachRDD { rdd =>
      val spark = SparkSession.builder.config(rdd.sparkContext.getConf).getOrCreate()
      import spark.implicits._

      // Convert RDD[String] to DataFrame
      val wordsDataFrame = rdd.toDF("raw_data")

      // Create a temporary view
      wordsDataFrame.createOrReplaceTempView("input_data")

      if (!rdd.isEmpty()) {
        val dfJSON = wordsDataFrame.withColumn("json", from_json(col("raw_data"), schema)).select("json.*")
        val dfJSON_decoded = dfJSON.withColumn("decoded_data", unbase64(col("data")).cast("string"))

        dfJSON_decoded.show()

        val decoded_data = dfJSON_decoded.select(col("decoded_data"))
        decoded_data.show()

        val words = decoded_data
          .flatMap { row => row.toString().substring(1, row.toString().length-1).split("\n")}
          .flatMap { row => row.toString().split(" ")}

        words.show()
        val pairs = words.map(word => (word, 1))
        val wordCounts = pairs.rdd.reduceByKey(_ + _)

        if (cumulativeRDD == null){
          cumulativeRDD = wordCounts
        }else{
          val ret = cumulativeRDD.fullOuterJoin(wordCounts)
          cumulativeRDD = ret.map[(String, Int)]({
            vv: (String, (Option[Int], Option[Int])) =>  {(vv._1, vv._2._1.getOrElse(0) + vv._2._2.getOrElse(0))}
          })
        }
        cumulativeRDD.foreach(println)
      }
    }

    println("Starting the computation...")
    ssc.start() // Start the computation
    ssc.awaitTermination() // Wait for the computation to terminate
  }

}