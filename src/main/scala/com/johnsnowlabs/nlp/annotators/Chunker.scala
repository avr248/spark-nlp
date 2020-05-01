package com.johnsnowlabs.nlp.annotators

import com.johnsnowlabs.nlp.{Annotation, AnnotatorModel, AnnotatorType}
import org.apache.spark.ml.param.StringArrayParam
import org.apache.spark.ml.util.{DefaultParamsReadable, Identifiable}

import scala.util.matching.Regex


/**
  * This annotator matches a pattern of part-of-speech tags in order to return meaningful phrases from document
  *
  * See [[https://github.com/JohnSnowLabs/spark-nlp/blob/master/src/test/scala/com/johnsnowlabs/nlp/annotators/ChunkerTestSpec.scala]] for reference on how to use this API.
  *
  * @param uid internal uid required to generate writable annotators
  **/
class Chunker(override val uid: String) extends AnnotatorModel[Chunker] {

  import com.johnsnowlabs.nlp.AnnotatorType._

  /** an array of grammar based chunk parsers */
  val regexParsers = new StringArrayParam(this, "regexParsers", "an array of grammar based chunk parsers")

  /** Output annotator type : CHUNK */
  override val outputAnnotatorType: AnnotatorType = CHUNK
  /** Input annotator type : DOCUMENT, POS */
  override val inputAnnotatorTypes: Array[AnnotatorType] = Array(DOCUMENT, POS)

  /** A list of regex patterns to match chunks, for example: Array(“‹DT›?‹JJ›*‹NN›”) */
  def setRegexParsers(value: Array[String]): Chunker = set(regexParsers, value)

  /** adds a pattern to the current list of chunk patterns, for example: “‹DT›?‹JJ›*‹NN›” */
  def addRegexParser(value: String): Chunker = {
    set(regexParsers, get(regexParsers).getOrElse(Array.empty[String]) :+ value)
  }

  /** A list of regex patterns to match chunks, for example: Array(“‹DT›?‹JJ›*‹NN›”) */
  def getRegexParsers: Array[String] = $(regexParsers)

  def this() = this(Identifiable.randomUID("CHUNKER"))

  private lazy val replacements = Map("<" -> "(?:<", ">" -> ">)", "|" -> ">|<")

  private lazy val POSTagPatterns: Array[Regex] = {
    getRegexParsers.map(regexParser => replaceRegexParser(regexParser))
  }

  private def replaceRegexParser(regexParser: String): Regex = {
    replacements.foldLeft(regexParser)((accumulatedParser, keyValueReplace) =>
      accumulatedParser.replaceAllLiterally(keyValueReplace._1, keyValueReplace._2)).r
  }

  private def patternMatchIndexes(pattern: Regex, text: String): List[(Int, Int)] = {
    pattern.findAllMatchIn(text).map(index => (index.start, index.end )).toList
  }

  private def patternMatchFirstIndex(pattern: Regex, text: String): List[Int] =
    pattern.findAllMatchIn(text).map(_.start).toList

  private def getIndexAnnotation(limits: (Int, Int), indexTags: List[(Int, Int)]): List[Int] = {
    val indexAnnotation = indexTags.zipWithIndex.collect {
      case (range, index) if limits._1 - 1 <= range._1 && limits._2 > range._2 => index
    }
    indexAnnotation
  }

  private def getPhrase(indexAnnotation: List[Int], annotations: Seq[Annotation]): Seq[Annotation] = {
    val annotation = indexAnnotation.map(index => annotations.apply(index))
    annotation
  }

  private def getChunkPhrases(POSTagPattern: Regex, POSFormatSentence: String, annotations: Seq[Annotation]):
  Option[Array[Seq[Annotation]]] = {
    val rangeMatches = patternMatchIndexes(POSTagPattern, POSFormatSentence)
    if (rangeMatches.isEmpty){
      None
    }
    val indexLeftTags = patternMatchFirstIndex("<".r, POSFormatSentence)
    val indexRightTags = patternMatchFirstIndex(">".r, POSFormatSentence)
    val indexTags = indexLeftTags zip indexRightTags //merge two sequential collections
    val indexAnnotations = rangeMatches.map(range => getIndexAnnotation(range, indexTags))
    Some(indexAnnotations.map(indexAnnotation => getPhrase(indexAnnotation, annotations)).toArray)
  }

  override def annotate(annotations: Seq[Annotation]): Seq[Annotation] = {

    val sentences = annotations.filter(_.annotatorType == AnnotatorType.DOCUMENT)

    sentences.zipWithIndex.flatMap { case(sentence, sentenceIndex) =>

      val sentencePos = annotations.filter(pos =>
        pos.annotatorType == AnnotatorType.POS &&
          pos.begin >= sentence.begin &&
          pos.end <= sentence.end)

      val POSFormatSentence = sentencePos.map(annotation => "<"+annotation.result+">")
        .mkString(" ").replaceAll("\\s","")

      val chunkPhrases = POSTagPatterns.flatMap(POSTagPattern =>
        getChunkPhrases(POSTagPattern, POSFormatSentence, sentencePos)).flatten

      val chunkAnnotations = chunkPhrases.zipWithIndex.map{ case (phrase, idx) =>
        val result = sentence.result.substring(
          phrase.head.begin - sentence.begin,
          phrase.last.end - sentence.begin + 1
        )
        val start = phrase.head.begin
        val end = phrase.last.end
        Annotation(
          outputAnnotatorType,
          start,
          end,
          result,
          Map("sentence" -> sentenceIndex.toString, "chunk" -> idx.toString)
        )
      }

      chunkAnnotations
    }

  }

}

object Chunker extends DefaultParamsReadable[Chunker]