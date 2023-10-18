import org.apache.spark._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.rdd.RDD

import org.apache.spark.sql.{Row,SparkSession,DataFrame}
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
//import org.apache.commons.codec.binary.Base64

import java.lang.management.ManagementFactory


object licensectrl {
  // Creazione del grafo e aggiunta degli archi
  val licenseGraph = new DirectedGraph()
  licenseGraph.loadFromFile("/beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/app/licensectrl/src/main/scala/grafo.txt")

  println(licenseGraph)
  
  var logger = LogManager.getLogger(getClass().getName());

  var cumulativeRDD: RDD[(String, Int)] = _

  def main(args: Array[String]): Unit = {

    // // Ottieni l'oggetto RuntimeMXBean
    // val runtimeMxBean = ManagementFactory.getRuntimeMXBean()

    // // Ottieni il nome del processo
    // val processName = runtimeMxBean.getName()

    // println(s"Il nome del processo è: $processName")  

    // // Termina il processo specificato dal nome
    // val exitCode = s"pkill $processName".!

    // // Stampa il codice di uscita del comando (0 se terminato correttamente)
    // println(s"Codice di uscita del comando: $exitCode")

    // // Termina il tuo programma con lo stesso codice di uscita del comando
    // sys.exit(exitCode)

    println(s"arg0 = ${args(0)}, arg1 = ${args(1)}, arg2 = ${args(2)}, arg3 = ${args(3)}, arg4 = ${args(4)} ")

    Configurator.setRootLevel(Level.FATAL)    

    val conf = new SparkConf().setAppName("licensectrl")

    // Print all the configuration settings
    val configSettings = conf.getAll
    println("SparkConf Settings:")
    configSettings.foreach { case (key, value) =>
      println(s"$key: $value")
    }

    val ssc = new StreamingContext(conf, Seconds( Integer.parseInt(args(3) )))
    val outputPath = s"/beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/spark_output/"+args(4)
    
    // Get the SparkContext's statusTracker
    val statusTracker = ssc.sparkContext.statusTracker
    // Get application id 
    val id = ssc.sparkContext.applicationId

    println(s"spark application id: application_$id")

    // Create a DStream that will connect to hostname:port, like localhost:9999
    println("Connect to " + args(1) + ":"+args(2)+ " " + Integer.parseInt(args(2)))

    val schema = new StructType()
      .add("project_id", StringType, nullable = true)
      .add("file_basename", StringType, nullable = true)
      .add("data", StringType, nullable = true)   
      .add("path", StringType, nullable = true)
 
    val schemaMap = StructType(
      Array (
        StructField("project_id", StringType,  nullable = true),
        StructField("file_basename", StringType, nullable = true),
        StructField("license", StringType, nullable = true),
        StructField("category", StringType, nullable = true),
        StructField("path", StringType, nullable = true)
      )
    )
    val rawStreams = (1 to args(0).toInt).map(_ =>
                      ssc.socketTextStream(args(1), Integer.parseInt(args(2)), StorageLevel.MEMORY_ONLY))

    val spark = SparkSession.builder().getOrCreate()

    val filePath = "/beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/app/licensectrl/src/main/scala/scancode_index.json"
    val licenseMap:Map[String, String] = readJsonFileToMap(filePath, spark)
    
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
    val file_path = "/beegfs/home/gspinate/Software-Heritage-Analytics/Orchestrator/ramdisk" 

    // val workerCount = ssc.statusTracker.getExecutorInfos.length

    // var combinedRDD: RDD[Row] = spark.sparkContext.emptyRDD[Row] // Initialize an empty RDD
    var broadcastVar = spark.sparkContext.broadcast(Seq.empty[Row])
    // var broadcastEnd = spark.sparkContext.broadcast()

    // val reparted = union.repartition(args(0).toInt)
    union.foreachRDD { rdd =>
      logger.fatal("NUOVO RDDDDD")
      // val currentIndex = currentStreamIndex.get()
      // logger.fatal(s"Current index $currentIndex")
      if (!rdd.isEmpty()) {
        was_empty = false
        EMPTYCounter.reset()
        logger.fatal("NON E' EMPTY")
        logger.fatal(s"Abbiamo N: ${broadcastVar.value.length} valori")

        
        val rowRDD = rdd.map { item =>
        var project_id =  ""
        var file_withpath = ""
        var file_basename = ""
        var data = ""
        try {
            var json = parse(item).asInstanceOf[JObject]
            project_id =    compact(render((json \ "project_id"))).replace("\"", "")
            file_withpath = compact(render((json \ "file_name"))).replace("\"", "")
            file_basename = compact(render((json \ "file_basename"))).replace("\"", "")
            data =          compact(render((json \ "data"))).replace("\"", "")
            

            if (project_id == "EOS"){
              EOSCounter.add(1) 
              // streamCounters(currentIndex) += 1
            }
          } catch {
            case e: Exception =>
              logger.fatal(s"ITEM: $item")
              logger.fatal(s"ECCEZIONE: $e")
          }
          Row(project_id, file_withpath, file_basename, data)
        }

        
        // Creazione dataframe
        val rddDataFrameToClean = spark.createDataFrame(rowRDD, schema)
        // rddDataFrameToClean.show()
        val rddDataFrame = rddDataFrameToClean.where(!col("project_id").contains("EOS")).where(!col("project_id").contains("WAIT"))
        // rddDataFrame.show()
        if (rddDataFrame.count().toInt > 0){

          // rddDataFrame.show()
          // logDataFrame(rddDataFrame, logger)
          
          
          val rddData = rddDataFrame.rdd.map{ row =>
            import org.apache.spark.sql.functions._
            import spark.implicits._
            // Estraggo i dati dalla riga
            val project_id = row.getString(0)
            val file_withpath = row.getString(1)
            val file_basename = row.getString(2)
            val data_64 = row.getString(3)

            val file_name = generateSHA1(project_id+"_"+file_withpath)

            // Decode del base 64
            val data_bytes = Base64.getDecoder.decode(data_64)
            val data = new String(data_bytes, "UTF-8") 

            // Nome file salvato
            val new_file = s"$file_path/$file_name"

            // Scrivo file su disco per scancode
            val outputFile = new File(new_file)
            val writer = new BufferedWriter(new FileWriter(outputFile, true))
            // Write item to the file6
            writer.write(data)
            writer.close()

            // ESTRAZIONE DELLA LICENZA 
            
            // TODO: Cache provare a fare un file con log e file da cui pescare invece di process
            // TODO:
            // - lista dei progetti dal link sopra
            // - aggiungere ricerca licenze dichiarate (file LICENSE)
            // - se le licenze sono > 2 controllare se le licenze appartengono a categorie diverse aggiungere la colonna che controlla inconsistenza delle classi e dichiarare le coppie inconsistenti
            // - conflitti : cercare in tabella e verificare presenza licenza
            
            // Lancio scancode su file  
            val json_file = s"$file_path/$file_name.json"
            var file_exists = Files.exists(Paths.get(json_file))

            if (!file_exists) {
            val command = Seq(
              "/beegfs/home/gspinate/slurm_tools/scancode-toolkit/scancode",
              "-clpeui",
              "-q",
              "-n",
              "1",
              "--json-pp",
              json_file,
              new_file
            )
            val exitCode = command.!              
            //   val process = Runtime.getRuntime.exec(s"/home/ubuntu/scancode-toolkit/scancode -clpeui -q -n 1 --json-pp \'${json_file}\' \'${new_file}\'")
            //   process.waitFor()  // Aspetta che il processo esterno finisca                
            }


// val processBuilder = new ProcessBuilder(command: _*)

// val process = processBuilder.start()

// // Attendere che il processo esterno finisca
// val exitCode = process.waitFor()
            
            var license = ""
            var category = "Unstated License"            

            // file_exists = Files.exists(Paths.get(json_file))
            // if (file_exists) {
              // license = ""
              // category = "Unstated License"
              val file_content = Source.fromFile(json_file).getLines().mkString("\n")
            
              
              val file_content_json = parse(file_content).asInstanceOf[JObject]

              // Estrai il campo "license_detections" come lista di oggetti JValue
              var license_detections: JArray = JArray(Nil)
              
              try {
                license_detections = (file_content_json \ "license_detections").asInstanceOf[JArray]
              } catch {
                case e: Exception =>
                  logger.fatal(s"ECCEZIONE: $e")
              }

              // Estrai il campo "license_expression" da ciascun oggetto "license_detections"
              val licenses = license_detections.arr.map { obj =>
                (obj \ "license_expression").asInstanceOf[JString].s
              }
              
              if (licenses.nonEmpty){
                license = licenses(0)
                // val splitted = license.split(" ")
                // if (splitted.length > 1){
                //   license = splitted(0)
                // }
                val found: Option[String] = licenseMap.get(license)

                found match {
                  case Some(value) => category = value
                  case None        => category = "Unknown"
                }
              }         
            // }
            
            Row(project_id, file_basename, license, category, file_withpath)
          }
          // combinedRDD = combinedRDD.union(rddData)
          
          // Ottieni i dati dalla variabile broadcast
          val existingData = broadcastVar.value

          // Unisci i dati esistenti con i nuovi dati
          val combinedData = existingData ++ rddData.collect()

          // Aggiorna la variabile broadcast con i nuovi dati
          broadcastVar.unpersist() // Rimuovi i dati esistenti dalla cache
          broadcastVar = spark.sparkContext.broadcast(combinedData)
          
        }
      }
      else {
        logger.fatal("RDD empty")
        logger.fatal(s"COUNTER ${EOSCounter.value} di ${args(0).toInt }")
        logger.fatal(s"Abbiamo N: ${broadcastVar.value.length} valori")
        // EOSCounter.add(1)
        if (!was_empty){
          was_empty  = true
        } else {
          EMPTYCounter.add(1)
        }
        if ( EOSCounter.value >= args(0).toInt || EMPTYCounter.value < 0) {
          
        // streamCounters.foreach(println)

        // if (streamCounters.forall(_ > 0)) {
          logger.fatal("EOS - App exit")
          val broadcastValue = broadcastVar.value
          val completeDF = spark.createDataFrame(spark.sparkContext.parallelize(broadcastValue), schemaMap).toDF("project_id", "file_basename", "license", "category", "path")
          // var num_row = completeDF.count().toInt
          // completeDF.show(num_row, false)       

          import org.apache.spark.sql.expressions.Window
          import org.apache.spark.sql.functions._


          val declared_license_files = List("LICENSE", "LICENSE.txt", "COPYING", "COPYING.TXT", "NOTICE", "README", "README.md")

          // Filtra il DataFrame originale per trovare i file con licenza dichiarata
          val filteredDeclaredData: DataFrame = completeDF.filter(col("file_basename").isin(declared_license_files: _*))
          
          val declaredLicenseDF = filteredDeclaredData
            .groupBy("project_id")
            .agg(
              collect_set(struct(col("license"), col("category"))).as("project_declared_licenses"),
              count("*").as("n_project_declared_licenses"),
              collect_set("category").as("distinct_category_declared"),
              collect_set("license").as("distinct_license_declared")
            )
          
          // declaredLicenseDF.show()

          val filteredLicenseDF: DataFrame = completeDF.filter(!col("file_basename").isin(declared_license_files: _*))

          val incodeLicenseDF = filteredLicenseDF.select("project_id", "license", "category").distinct()
            .groupBy("project_id")
            .agg(
              collect_list(struct(col("license"), col("category"))).as("project_incode_licenses"),
              collect_set("category").as("distinct_category_incode"),
              collect_set("license").as("distinct_license_incode")
            )

          val incodeTotLicenseDF = filteredLicenseDF
            .filter(col("license") =!= "") 
            .groupBy("project_id")
            .agg(
              collect_list("license").as("tot_license")
            )        
            
          // Effettua il conteggio di tipi di espressioni e categorie diverse per ogni project_id
          val risultatoFinale = completeDF.groupBy("project_id")
            .agg(
              count("*").as("n_project_files")
          )

          // Effettua il join tra completeDFWithLicenseType e risultato utilizzando "project_id" come chiave di join
          val risultatoUnione = risultatoFinale.join(declaredLicenseDF, Seq("project_id"), "left")
                                .join(incodeLicenseDF, Seq("project_id"), "left")
                                .join(incodeTotLicenseDF, Seq("project_id"), "left")
                                .withColumn("n_project_incode_licenses", size(col("tot_license")))


          // Mostra il risultato
          // num_row = risultatoUnione.count().toInt
          // risultatoUnione.show(num_row, false)


          val updatedDF = risultatoUnione
            .withColumn("n_project_declared_licenses", when(col("n_project_declared_licenses").isNull || col("n_project_declared_licenses") === -1, 0).otherwise(col("n_project_declared_licenses")))
            .withColumn("project_declared_licenses", when(col("project_declared_licenses").isNull, array()).otherwise(col("project_declared_licenses")))
            .withColumn("n_project_incode_licenses", when(col("n_project_incode_licenses").isNull || col("n_project_incode_licenses") === -1, 0).otherwise(col("n_project_incode_licenses")))
            .withColumn("project_incode_licenses", when(col("project_incode_licenses").isNull, array()).otherwise(col("project_incode_licenses")))   
            .withColumn("distinct_category_declared", when(col("distinct_category_declared").isNull, array()).otherwise(col("distinct_category_declared")))
            .withColumn("distinct_category_incode", when(col("distinct_category_incode").isNull, array()).otherwise(col("distinct_category_incode")))
            .withColumn("distinct_license_declared", when(col("distinct_license_declared").isNull, array()).otherwise(col("distinct_license_declared")))
            .withColumn("distinct_license_incode", when(col("distinct_license_incode").isNull, array()).otherwise(col("distinct_license_incode")))
                 
          

          // updatedDF.show()

          // Definisci una UDF che utilizza la tua funzione findInconsistency
          val findInconsistencyUDF = udf((lista1: List[String], lista2: List[String]) => {
            val out: List[(String, String, Boolean, Boolean, Boolean)] = findInconsistency(lista1, lista2)
            out
          }) 
          val findInconsistencySelfUDF = udf((lista1: List[String]) => {
            val out: List[(String, String, Boolean, Boolean, Boolean)] = findInconsistency(lista1)
            out
          })           

          // Definisci una UDF che utilizza la tua funzione findConflict
          val findConflictUDF = udf((lista1: List[String], lista2: List[String]) => {
            val out: List[(String, String)] = findConflict(lista1, lista2, licenseGraph)
            out
          })  

          // Definisci una UDF che utilizza la tua funzione findConflict
          val findConflictSelfUDF = udf((lista1: List[String]) => {
            val out: List[(String, String)] = findConflict(lista1, licenseGraph)
            out
          })            

          val aggragetedAnalisys = updatedDF
          .withColumn("between_declared_inconsistencies", findInconsistencySelfUDF(col("distinct_category_declared")))
          .withColumn("between_incode_inconsistencies", findInconsistencySelfUDF(col("distinct_category_incode")))
          .withColumn("between_declared_incode_inconsistencies", findInconsistencyUDF(col("distinct_category_declared"), col("distinct_category_incode")))
          .withColumn("between_declared_conflicts", findConflictSelfUDF(col("distinct_license_declared")))
          .withColumn("between_incode_conflicts", findConflictSelfUDF(col("distinct_license_incode")))
          .withColumn("between_declared_incode_conflicts", findConflictUDF(col("distinct_license_declared"),col("distinct_license_incode")))
          // aggragetedAnalisys.show(truncate=false)
          
          saveOnFile(aggragetedAnalisys, outputPath)

          println("USCITO")
          ssc.stop(false)
          println("USCITO")
          System.exit(0)
          println("USCITO")
        }
      }  
    }

    ssc.start() // Start the computation
    ssc.awaitTermination() // Wait for the computation to terminate 
    System.exit(0)
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

  def readJsonFileToMap(filePath: String, spark: SparkSession): Map[String, String] = {
    import scala.collection.mutable.Map
    val jsonStringsRDD = Source.fromFile(filePath).getLines().mkString("\n")
    val json = parse(jsonStringsRDD).asInstanceOf[JArray].arr.asInstanceOf[Seq[JObject]]

    val resultMap = json.map  { item =>
      val license_key = (item \ "license_key").asInstanceOf[JString].s
      val category = (item \ "category").asInstanceOf[JString].s
      (license_key, category)
    }.toMap

    resultMap
  }   

  def extractUniqueCategories(df: DataFrame, spark: SparkSession): Set[String] = {
    val categorySet = df.select("category").distinct().collect().map( row => row.getString(0)).toSet
    categorySet
  }

  def extractUniqueLicense(df: DataFrame,project_id: String ): DataFrame = {
    // Filtra il DataFrame per il project_id desiderato
    val filtroProjectID = df.filter(col("project_id") === project_id)

    // Estrai tutte le diverse espressioni per il project_id desiderato
    val espressioniDistinct = filtroProjectID.select("license").distinct()

    espressioniDistinct
  }

  // Dopo aver ridotto le categorie vanno confrontate e visto se ci sono inconsistenze FREE e NON-FREE non possono stare insieme!!
  def findInconsistency(lista1: List[String], lista2: List[String]): List[(String, String, Boolean, Boolean, Boolean)] = {
    var listaOutput: Set[(String, String, Boolean, Boolean, Boolean)] = Set()
    var hybridInconsistency = false
    var unstatedLicense = false 
    var inconsistency = false
    if (lista1.length > 0 && lista2.length > 0){
      for (i <- 0 until lista1.length) {
        hybridInconsistency = false
        unstatedLicense = false 
        inconsistency = false      
        for (j <- 0 until lista2.length) {
          val licenseType1 = lista1(i)
          val licenseType2 = lista2(j)
          val FREE_LICENSES = Set("Public Domain", "Permissive", "Copyleft Limited", "Copyleft", "Proprietary Free")
          val NON_FREE_LICENSES = Set("Commercial", "Source-available", "Patent License", "Free Restricted")

          // ritorniamo tupla License_inconsistency(inconsistency=False, hybrid_inconsistency=False, unstated_license=False)
            
          // flag unstated license
          if (licenseType1 == "Unstated License" || licenseType2 == "Unstated License") {
            unstatedLicense = true
          }

          // no inconsistency if one of the licences is public domain
          if (licenseType1 == "Public Domain" || licenseType2 == "Public Domain") {
            inconsistency = false      
          } else {
            // there is an inconsistency if both licenses are free or non-free, but not the same
            if (FREE_LICENSES.contains(licenseType1) && FREE_LICENSES.contains(licenseType2) ||
                NON_FREE_LICENSES.contains(licenseType1) && NON_FREE_LICENSES.contains(licenseType2)) {
              inconsistency = true      
            } else {
              // there is an inconsistency if one of the licenses is free and the other is non-free
              hybridInconsistency = true
            }
          }
          val tupla = if (licenseType1 < licenseType2) (licenseType1, licenseType2, inconsistency, hybridInconsistency, unstatedLicense) else (licenseType2, licenseType1, inconsistency, hybridInconsistency, unstatedLicense)
          listaOutput += tupla 
        }
      }  
    }
    listaOutput.toList
  }


  def findConflict(lista1: List[String], lista2: List[String], licenseGraph: DirectedGraph): List[(String, String)] = {
    var listaOutput: Set[(String, String)] = Set()
    if (lista1.length > 0 && lista2.length > 0){
      for (i <- 0 until lista1.length) {    
        for (j <- 0 until lista2.length) {
          val license1 = lista1(i)
          val license2 = lista2(j)
          val areOnSamePath = licenseGraph.areNodesOnSamePath(license1, license2)
          
          if (!areOnSamePath) {
            val coppia = if (license1 < license2) (license1, license2) else (license2, license1)
            listaOutput += coppia
          }
        }
      }
    }
    listaOutput.toList 
  }

  def findInconsistency(lista: List[String]): List[(String, String, Boolean, Boolean, Boolean)] = {
    var listaOutput: List[(String, String, Boolean, Boolean, Boolean)] = List()
    var hybridInconsistency = false
    var unstatedLicense = false 
    var inconsistency = false
    if (lista.length > 0){
      for (i <- 0 until lista.length) {
        hybridInconsistency = false
        unstatedLicense = false 
        inconsistency = false      
        for (j <- i + 1 until lista.length) {
          val licenseType1 = lista(i)
          val licenseType2 = lista(j)
          val FREE_LICENSES = Set("Public Domain", "Permissive", "Copyleft Limited", "Copyleft", "Proprietary Free")
          val NON_FREE_LICENSES = Set("Commercial", "Source-available", "Patent License", "Free Restricted")

          // ritorniamo tupla License_inconsistency(inconsistency=False, hybrid_inconsistency=False, unstated_license=False)
            
          // flag unstated license
          if (licenseType1 == "Unstated License" || licenseType2 == "Unstated License") {
            unstatedLicense = true
          }

          // no inconsistency if one of the licences is public domain
          if (licenseType1 == "Public Domain" || licenseType2 == "Public Domain") {
            inconsistency = false      
          } else {
            // there is an inconsistency if both licenses are free or non-free, but not the same
            if (FREE_LICENSES.contains(licenseType1) && FREE_LICENSES.contains(licenseType2) ||
                NON_FREE_LICENSES.contains(licenseType1) && NON_FREE_LICENSES.contains(licenseType2)) {
              inconsistency = true      
            } else {
              // there is an inconsistency if one of the licenses is free and the other is non-free
              hybridInconsistency = true
            }
          }
          val tupla = (licenseType1, licenseType2, inconsistency, hybridInconsistency, unstatedLicense)
          listaOutput = tupla :: listaOutput
        }
      }  
    }
    listaOutput  
  }


  def findConflict(lista: List[String], licenseGraph: DirectedGraph): List[(String, String)] = {
    var listaOutput: List[(String, String)] = List()
    if (lista.length > 0){
      for (i <- 0 until lista.length) {    
        for (j <- i + 1 until lista.length) {
          val license1 = lista(i)
          val license2 = lista(j)
          val areOnSamePath = licenseGraph.areNodesOnSamePath(license1, license2)
          if (!areOnSamePath){
            var tupla = (license1, license2)
            listaOutput = tupla :: listaOutput
          }
        }
      }
    }
    listaOutput  
  }

  def fakeConfilct(lista: List[String]): List[(String, String)] = {
    List(("stringa1", "stringa2"), ("stringa3", "stringa4"), ("stringa5", "stringa6"))
  }

  def logDataFrame(df: DataFrame, logger: Logger): Unit = {
    import org.apache.logging.log4j.Logger;
    // Utilizza il metodo foreach per iterare sul DataFrame e stampare ciascuna riga
    df.foreach { row =>
      val rowAsString = row.mkString(", ")
      logger.info(rowAsString) // O puoi utilizzare logger.info(rowAsString) per scriverlo nei log
    }
  }

  def saveOnFile(df: DataFrame, outputPath: String) = {
    // val outputPath = "/var/shared_lab/dataframe"
    // Specifica il percorso di output del file JSON
      
    // Concatena gli elementi dell'array in una singola stringa utilizzando uno spazio come separatore
    // val dfWithConcatenatedColumns = df
    //   .withColumn("unique_licenses", concat_ws(";", col("unique_licenses")))
    //   .withColumn("unique_categories", concat_ws(";", col("unique_categories")))


    // Ora puoi selezionare solo le colonne di interesse, ad esempio:
    val dfToSave = df.select("project_id", "n_project_files", "n_project_declared_licenses", "n_project_incode_licenses", "project_declared_licenses", "project_incode_licenses", "between_declared_inconsistencies", "between_incode_inconsistencies", "between_declared_incode_inconsistencies", "between_declared_conflicts", "between_incode_conflicts", "between_declared_incode_conflicts")

    println(s"Abbiamo ${dfToSave.count()} da salvare!!")
    dfToSave.show()
    // Salva il DataFrame come file JSON
    dfToSave.write.format("json").mode("overwrite").save(outputPath)     
  }

  def generateSHA1(input: String): String = {
    val md = MessageDigest.getInstance("SHA-1")
    val byteArray = md.digest(input.getBytes("UTF-8"))

    // Converte l'array di byte in una rappresentazione esadecimale
    val sb = new StringBuilder
    byteArray.foreach { byte =>
      sb.append(String.format("%02x", byte & 0xFF))
    }

    sb.toString
  }

}




