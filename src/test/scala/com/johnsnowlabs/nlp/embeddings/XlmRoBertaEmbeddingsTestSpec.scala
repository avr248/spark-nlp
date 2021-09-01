/*
 * Copyright 2017-2021 John Snow Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.johnsnowlabs.nlp.embeddings

import com.johnsnowlabs.nlp.annotators.{StopWordsCleaner, Tokenizer}
import com.johnsnowlabs.nlp.base.DocumentAssembler
import com.johnsnowlabs.nlp.training.CoNLL
import com.johnsnowlabs.nlp.util.io.ResourceHelper
import com.johnsnowlabs.tags.SlowTest
import com.johnsnowlabs.util.Benchmark

import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.functions.{col, explode, size}
import org.scalatest.flatspec.AnyFlatSpec

class XlmRoBertaEmbeddingsTestSpec extends AnyFlatSpec {


  "XlmRoBertaEmbeddings" should "correctly work with empty tokens" taggedAs SlowTest in {

    val smallCorpus = ResourceHelper.spark.read.option("header", "true").csv("src/test/resources/embeddings/sentence_embeddings.csv")

    val documentAssembler = new DocumentAssembler()
      .setInputCol("text")
      .setOutputCol("document")

    val tokenizer = new Tokenizer()
      .setInputCols(Array("document"))
      .setOutputCol("token")

    val stopWordsCleaner = new StopWordsCleaner()
      .setInputCols("token")
      .setOutputCol("cleanTokens")
      .setStopWords(Array("this", "is", "my", "document", "sentence", "second", "first", ",", "."))
      .setCaseSensitive(false)

    val embeddings = XlmRoBertaEmbeddings.pretrained()
      .setInputCols("document", "cleanTokens")
      .setOutputCol("embeddings")
      .setCaseSensitive(true)

    val pipeline = new Pipeline()
      .setStages(Array(
        documentAssembler,
        tokenizer,
        stopWordsCleaner,
        embeddings
      ))

    val pipelineDF = pipeline.fit(smallCorpus).transform(smallCorpus)
    Benchmark.time("Time to save BertEmbeddings results") {
      pipelineDF.write.mode("overwrite").parquet("./tmp_bert_embeddings")
    }
  }

  "XlmRoBertaEmbeddings" should "benchmark test" taggedAs SlowTest in {
    import ResourceHelper.spark.implicits._

    val conll = CoNLL()
    val training_data = conll.readDataset(ResourceHelper.spark, "src/test/resources/conll2003/eng.train")

    val embeddings = XlmRoBertaEmbeddings.pretrained()
      .setInputCols("sentence", "token")
      .setOutputCol("embeddings")
      .setCaseSensitive(true)
      .setMaxSentenceLength(512)
      .setBatchSize(12)

    val pipeline = new Pipeline()
      .setStages(Array(
        embeddings
      ))

    val pipelineDF = pipeline.fit(training_data).transform(training_data)
    Benchmark.time("Time to save RoBertaEmbeddings results") {
      pipelineDF.write.mode("overwrite").parquet("./tmp_bert_embeddings")
    }

    println("missing tokens/embeddings: ")
    pipelineDF.withColumn("sentence_size", size(col("sentence")))
      .withColumn("token_size", size(col("token")))
      .withColumn("embed_size", size(col("embeddings")))
      .where(col("token_size") =!= col("embed_size"))
      .select("sentence_size", "token_size", "embed_size", "token.result", "embeddings.result")
      .show(false)

    println("total sentences: ", pipelineDF.select(explode($"sentence.result")).count)
    val totalTokens = pipelineDF.select(explode($"token.result")).count.toInt
    val totalEmbeddings = pipelineDF.select(explode($"embeddings.embeddings")).count.toInt

    println(s"total tokens: $totalTokens")
    println(s"total embeddings: $totalEmbeddings")

    assert(totalTokens == totalEmbeddings)
  }

  "XlmRoBertaEmbeddings" should "download, save, and load a model" taggedAs SlowTest in {

    import ResourceHelper.spark.implicits._

    val ddd = Seq(
      "This is just a simple sentence for the testing purposes!"
    ).toDF("text")

    val document = new DocumentAssembler()
      .setInputCol("text")
      .setOutputCol("document")

    val tokenizer = new Tokenizer()
      .setInputCols(Array("document"))
      .setOutputCol("token")

    val embeddings = XlmRoBertaEmbeddings.pretrained()
      .setInputCols("document", "token")
      .setOutputCol("embeddings")
      .setCaseSensitive(true)
      .setMaxSentenceLength(512)
      .setBatchSize(12)

    val pipeline = new Pipeline().setStages(Array(document, tokenizer, embeddings))

    val pipelineModel = pipeline.fit(ddd)
    pipelineModel.transform(ddd).show()

    Benchmark.time("Time to save XlmRoBertaEmbeddings pipeline model") {
      pipelineModel.write.overwrite().save("./tmp_xlmroberta_pipeline")
    }

    Benchmark.time("Time to save XlmRoBertaEmbeddings model") {
      pipelineModel.stages.last.asInstanceOf[XlmRoBertaEmbeddings].write.overwrite().save("./tmp_xlmroberta_model")
    }

    val loadedPipelineModel = PipelineModel.load("./tmp_roberta_pipeline")
    loadedPipelineModel.transform(ddd).show()

    val loadedDistilBertModel = XlmRoBertaEmbeddings.load("./tmp_xlmroberta_model")
    loadedDistilBertModel.getDimension

  }

  "XlmRoBertaEmbeddings" should "be aligned with custome tokens from Tokenizer" taggedAs SlowTest in {

    import ResourceHelper.spark.implicits._

    val ddd = Seq(
      "Rare Hendrix song draft sells for almost $17,000.",
      "Rare Hendrix song draft sells for almost $17,000 .",
      "EU rejects German call to boycott British lamb .",
      "EU rejects German call to boycott British lamb.",
      "TORONTO 1996-08-21"
    ).toDF("text")

    val document = new DocumentAssembler()
      .setInputCol("text")
      .setOutputCol("document")

    val tokenizer = new Tokenizer()
      .setInputCols(Array("document"))
      .setOutputCol("token")

    val embeddings = XlmRoBertaEmbeddings.pretrained()
      .setInputCols("document", "token")
      .setOutputCol("embeddings")
      .setCaseSensitive(true)
      .setMaxSentenceLength(512)
      .setBatchSize(12)

    val pipeline = new Pipeline().setStages(Array(document, tokenizer, embeddings))

    val pipelineModel = pipeline.fit(ddd)
    val pipelineDF = pipelineModel.transform(ddd)

    pipelineDF.select("token").show(false)
    pipelineDF.select("embeddings.result").show(false)
    pipelineDF
      .withColumn("token_size", size(col("token")))
      .withColumn("embed_size", size(col("embeddings")))
      .where(col("token_size") =!= col("embed_size"))
      .select("token_size", "embed_size", "token.result", "embeddings.result")
      .show(false)

    val totalTokens = pipelineDF.select(explode($"token.result")).count.toInt
    val totalEmbeddings = pipelineDF.select(explode($"embeddings.embeddings")).count.toInt

    println(s"total tokens: $totalTokens")
    println(s"total embeddings: $totalEmbeddings")

    assert(totalTokens == totalEmbeddings)

  }
}
