// Databricks notebook source
// MAGIC %md
// MAGIC # Learning ADB Professionally 

// COMMAND ----------

val defaultMoviesUrl = "https://iomeagatraining.blob.core.windows.net/data/movies.csv"
val defaultRatingsUrl = "adl://omegadatalakestore.azuredatalakestore.net/Data/ratings.csv"
 
val moviesUrl = dbutils.widgets.text("moviesUrl","")
val ratingsUrl = dbutils.widgets.text("ratingsUrl", "")
 
var inputMoviesUrl = dbutils.widgets.get("moviesUrl")
 
if(inputMoviesUrl == null) {
  inputMoviesUrl = defaultMoviesUrl
}
 
var inputRatingsUrl = dbutils.widgets.get("ratingsUrl")
 
if(inputRatingsUrl == null) {
  inputRatingsUrl = defaultRatingsUrl
}

// COMMAND ----------

package com.microsoft.analytics.utils

import scala.io.Source
import scala.io.Codec
import java.nio.charset.CodingErrorAction

object MovieUtils{
def loadMovieNames(fileName: String): Map[Int, String] = {
  if(fileName == null || fileName == "") {
    throw new Exception("Invalid File / Reference URL Specified!");
  }

  implicit val codec = Codec("UTF-8")

  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

  val lines = Source.fromURL(fileName).getLines

  lines.drop(1)

  var movieNames: Map[Int, String] = Map()

  for(line <- lines) {
    val records = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")
    val movieId = records(0).toInt
    val movieName = records(1)

    movieNames += (movieId -> movieName)
  }

  movieNames
}}


// COMMAND ----------

import com.microsoft.analytics.utils._
val broadcastedMovies = sc.broadcast(() => { MovieUtils.loadMovieNames(inputMoviesUrl)})

// COMMAND ----------

spark.conf.set("dfs.adls.oauth2.access.token.provider.type", "ClientCredential")
spark.conf.set("dfs.adls.oauth2.client.id", "767985f3-541d-430f-9bbe-7944716ce2e8")
spark.conf.set("dfs.adls.oauth2.credential", "1feOUb/K+sgDdHbjwHX3OY5ZxtdFPq4EgYtIaKL0gUQ=")
spark.conf.set("dfs.adls.oauth2.refresh.url", "https://login.microsoftonline.com/72f988bf-86f1-41af-91ab-2d7cd011db47/oauth2/token")

spark.sparkContext.hadoopConfiguration.set("dfs.adls.oauth2.access.token.provider.type", spark.conf.get("dfs.adls.oauth2.access.token.provider.type"))
spark.sparkContext.hadoopConfiguration.set("dfs.adls.oauth2.client.id", spark.conf.get("dfs.adls.oauth2.client.id"))

spark.sparkContext.hadoopConfiguration.set("dfs.adls.oauth2.credential", spark.conf.get("dfs.adls.oauth2.credential"))

spark.sparkContext.hadoopConfiguration.set("dfs.adls.oauth2.refresh.url", spark.conf.get("dfs.adls.oauth2.refresh.url"))

val ratingsData = sc.textFile("adl://omegadatalakestore.azuredatalakestore.net/Data/ratings.csv")
val originalData = ratingsData.mapPartitionsWithIndex((index, iterator) => {
if(index == 0) iterator.drop(1)

 else iterator
})
val mappedData = originalData.map(line => { val splitted = line.split(",")

(splitted(1).toInt, 1)
})
val reducedData = mappedData.reduceByKey((x, y) => (x + y))
val result = reducedData.sortBy(_._2).collect
val finalOutput = result.reverse.take(10)
val mappedFinalOuptut = finalOutput.map(record => (broadcastedMovies.value()(record._1), record._2))

// COMMAND ----------

mappedFinalOuptut.foreach(println)

// COMMAND ----------

