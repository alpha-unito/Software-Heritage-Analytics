# Software-Heritage-Analytics
The Software Heritage Analytics framework (a parallel cache for analysing Software Heritage data with Spark Streaming applications)

#### General Description
Software Heritage Analytics (SHA) is a framework developed specifically in the context of the ADMIRE project. SHA was created from the need of a development environment able to perform analysis of open source software preserved over time by [Software Heritage](https://www.softwareheritage.org/) (SWH). SWH hosts over 100 million different projects with over 400 TB of data in constant growth. The data can be turned into valuable knowledge: copyright and license violations, error propagation, programming patterns, evolution of coding paradigms and languages. Another important aspect is the meaning of algorithms, which is a fundamental prerequisite for trust in analytics and machine learning. 
SHA architecture is designed to allow both fast access to data and to provide a parallel computing environment based on [Apache Spark Streaming](https://spark.apache.org/docs/latest/streaming-programming-guide.html).
SHA is composed of three main components: 
                    
* Data and metadata cache
* Data orchestration layer
* Web console

To speed up data transfer and data access it was chosen to implement a distributed cache component called Cachemire. As for the parallel computing environment, it was decided to adopt the Apache Spark framework, an open-source parallel computing environment for applications that analyze Big Data. Specifically, the Spark Streaming  version was chosen to optimize data transfer time to the analysis computation.
The orchestration layer and console use the official SWH APIs to search, retrieve and get the git status of the given project.
SHA allows the execution of custom analysis applications written in SCALA, compatible with Apache Spark Streaming. 
Applications can analyze a set of projects. The set of projects must be specified in files (recipes) containing the SWH identifier of the project and other metadata useful for analysis purposes (e.g., the type of programming language).
The SHA web console component is a browser app in which an authenticated user can: 
Search one or more projects by name inside the SWH archive which can be added to a recipe file
Upload a custom analytic app in JAR format
Create the correspondences between recipe and app (which application with which recipe)
Run the application
Both recipe files and application JAR files are stored in a local repository. It is possible to use the same application with different recipes and the same recipe with different applications. The “recipe” term is inherited by SWH because the process of preparing to download a project from SWH is called “cooking”. The cooking process is basically a preparation of a tar.gz with all the files of the requested project. 
#### Prerequisite
The three main components listed above are developed as separate modules, in the three directories:
* [Data and metadata cache](Cachemire)
* [Data orchestration layer](Orchestrator)
* [Web console](Webconsole)
