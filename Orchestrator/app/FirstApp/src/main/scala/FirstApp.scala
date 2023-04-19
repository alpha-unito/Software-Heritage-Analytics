import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.IntParam
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream
//import org.apache.spark.streaming.StreamingContext._ // not necessary since Spark 1.3
import org.apache.spark.sql._
import java.util.Base64
import org.apache.spark.TaskContext

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.apache.spark.internal.Logging

import java.net.Socket
import java.nio.charset.StandardCharsets

object FirstApp {
  // args params sequence: <numStreams> <host> <port> <batchMillis>
  def main(args: Array[String]): Unit = {

    Configurator.setRootLevel(Level.WARN)
    //val Array(IntParam(numStreams), host, IntParam(port), IntParam(batchMillis)) = args
    //StreamingExamples.setStreamingLogLevels()
    // val sparkConf = new SparkConf().setMaster("spark://ubuntu-8gb-fsn1-1:7077").setAppName("SimpleApp")
    // val sparkConf = new SparkConf().setMaster("local[3]").setAppName("SimpleApp")
    val sparkConf = new SparkConf().setAppName("FirstApp")

    // val mongoClient: MongoClient = MongoClient()
    // val database: MongoDatabase = mongoClient.getDatabase("analysis")
    // val analysis: MongoCollection[Document] = database.getCollection("analysis")
    // val doc: Document = Document("_id" -> "hash", "content" -> "decoded")
    // val insert = analysis.insertOne(doc)
    // Create the context
    val ssc = new StreamingContext(sparkConf, Duration(args(3).toInt))
    val spark = SparkSession.builder.getOrCreate()

    val rawStreams = (1 to args(0).toInt).map(_ =>
      //ssc.socketTextStream("127.0.0.1", 4321, StorageLevel.MEMORY_ONLY_SER_2))

      ssc.socketTextStream(args(1), args(2).toInt)) //, StorageLevel.MEMORY_ONLY_SER_2))


    //val union = ssc.union(rawStreams)
    //union.filter(_.contains("project")).count().foreachRDD(r =>
      //println(s"Grep count: ${r.collect().mkString}"))
      //ssc.start()
      //ssc.awaitTermination()

    //TaskContext tc = TaskContext.get();
    //println(tc.taskAttemptId());
    val updateFunc = (values: Seq[Int], state: Option[Int]) => {
      val currentCount = values.foldLeft(0)(_ + _)

      val previousCount = state.getOrElse(0)

      Some(currentCount + previousCount)
    }

    val union = ssc.union(rawStreams)
    union.foreachRDD(rdd => {

      if ( rdd.isEmpty != true ) {
        // println("rdd Type:" + rdd.getClass)
        //print("rdd.collect:" +rdd.collect())
        val df = spark.read.json(rdd)
        df.printSchema()
        df.show()

        //println("df Type:" + df.getClass)
        //df.map(row =>
        //  println("row" + row)
        //);


        // println("Before foreach ")
        val df_row = df.collect()

        df_row.foreach(row => {


          // println("Row " + row)
          if ( (row.getAs("project_id") != null) ) {

          val project_id = row.getAs("project_id").toString()

          //  println("project_id:" + project_id)

          if ( project_id=="EOS" ) {
            println("EOS")
            Thread.sleep(5000)
            //rdd.context.stop()
            ssc.stop()


          }

          val content = row.getAs("data").toString()
          val decoded = new String(Base64.getDecoder().decode(content.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8)
          // println("project_id:" + project_id)
          // println("content:" + content)
          // println("decoded:" + decoded)

          var df_tweets:DataFrame = null
          //val inputStream = ssc.queueStream(new Queue[RDD[String]])


          val lines = decoded.split("\n")

          val words = lines.flatMap(_.split(" "))

          val wordDstream = words.map(x => (x, 1))
          //val test = wordDstream.collect()
          //println(wordDstream)
          //println(wordDstream.getClass)
          //println(wordDstream(1))
          //println(wordDstream(2))
          // Update the cumulative count using updateStateByKey
          // This will give a Dstream made of state (which is the cumulative count of the words)
          //val stateDstream = wordDstream.updateStateByKey[Int](updateFunc)
          //stateDstream.print()
          }
        });
        //println("go to next file")
      }
    })
    ssc.start()
    ssc.awaitTermination()

  }
}

