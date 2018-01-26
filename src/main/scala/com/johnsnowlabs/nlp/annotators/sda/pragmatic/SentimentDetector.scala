package com.johnsnowlabs.nlp.annotators.sda.pragmatic

import com.johnsnowlabs.nlp.annotators.common.Tokenized
import com.johnsnowlabs.nlp.util.io.ResourceHelper
import com.johnsnowlabs.nlp.{Annotation, AnnotatorModel}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.ml.param.Param
import org.apache.spark.ml.util.{DefaultParamsReadable, Identifiable}

/**
  * Created by saif on 12/06/2017.
  */

/**
  * Gives a good or bad score to a sentence based on the approach used
  * @param uid internal uid needed for saving annotator to disk
  * @@ model: Implementation to be applied for sentiment analysis
  */
class SentimentDetector(override val uid: String) extends AnnotatorModel[SentimentDetector] {

  import com.johnsnowlabs.nlp.AnnotatorType._

  private val config: Config = ConfigFactory.load

  val dictPath = new Param[String](this, "dictPath", "path to dictionary for pragmatic sentiment analysis")

  val dictFormat = new Param[String](this, "dictFormat", "format of dictionary, can be TXT or TXTDS for read as dataset")

  val dictSeparator = new Param[String](this, "dictSeparator", "key value separator for dictionary")

  if (config.getString("nlp.sentimentDict.file").nonEmpty)
    setDefault(dictPath, config.getString("nlp.sentimentDict.file"))

  setDefault(dictFormat, config.getString("nlp.sentimentDict.format"))

  setDefault(dictSeparator, config.getString("nlp.sentimentDict.separator"))

  lazy val model: PragmaticScorer =
    new PragmaticScorer(SentimentDetector.retrieveSentimentDict($(dictPath), $(dictFormat), $(dictSeparator)))

  override val annotatorType: AnnotatorType = SENTIMENT

  override val requiredAnnotatorTypes: Array[AnnotatorType] = Array(TOKEN, DOCUMENT)

  def this() = this(Identifiable.randomUID("SENTIMENT"))

  def setDictPath(path: String): this.type = set(dictPath, path)

  def getDictPath: String = $(dictPath)

  def setDictFormat(format: String): this.type = set(dictFormat, format)

  def getDictFormat: String = $(dictFormat)

  def setDictSeparator(separator: String): this.type = set(dictSeparator, separator)

  def getDictSeparator: String = $(dictSeparator)

  /**
    * Tokens are needed to identify each word in a sentence boundary
    * POS tags are optionally submitted to the model in case they are needed
    * Lemmas are another optional annotator for some models
    * Bounds of sentiment are hardcoded to 0 as they render useless
    * @param annotations Annotations that correspond to inputAnnotationCols generated by previous annotators if any
    * @return any number of annotations processed for every input annotation. Not necessary one to one relationship
    */
  override def annotate(annotations: Seq[Annotation]): Seq[Annotation] = {
    val tokenizedSentences = Tokenized.unpack(annotations)

    val score = model.score(tokenizedSentences.toArray)

    Seq(Annotation(
      annotatorType,
      0,
      0,
      { if (score >= 0) "positive" else "negative"},
      Map.empty[String, String]
    ))
  }

}
object SentimentDetector extends DefaultParamsReadable[SentimentDetector] {

  /**
    * Sentiment dictionaries from compiled sources set in configuration
    * @return Sentiment dictionary
    */
  private[pragmatic] def retrieveSentimentDict(filePath: String, sentFormat: String, sentSeparator: String): Map[String, String] = {
    ResourceHelper.parseKeyValueText(filePath, sentFormat.toUpperCase, sentSeparator)
  }


}