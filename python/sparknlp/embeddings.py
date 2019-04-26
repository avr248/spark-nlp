import sparknlp.internal as _internal

from sparknlp.common import AnnotatorModel, HasWordEmbeddings, HasEmbeddings
from sparknlp.internal import _BertLoader

from pyspark.ml.param.shared import Param, TypeConverters
from pyspark.ml.param import Params
from pyspark import keyword_only


class Embeddings:
    def __init__(self, embeddings):
        self.jembeddings = embeddings


class EmbeddingsHelper:
    @classmethod
    def load(cls, path, spark_session, embeddings_format, embeddings_ref, embeddings_dim, embeddings_casesens=False):
        jembeddings = _internal._EmbeddingsHelperLoad(path, spark_session, embeddings_format, embeddings_ref, embeddings_dim, embeddings_casesens).apply()
        return Embeddings(jembeddings)

    @classmethod
    def save(cls, path, embeddings, spark_session):
        return _internal._EmbeddingsHelperSave(path, embeddings, spark_session).apply()

    @classmethod
    def getFromAnnotator(cls, annotator):
        return _internal._EmbeddingsHelperFromAnnotator(annotator).apply()


class WordEmbeddings(AnnotatorModel, HasWordEmbeddings):

    name = "WordEmbeddings"

    sourceEmbeddingsPath = Param(Params._dummy(),
                                 "sourceEmbeddingsPath",
                                 "Word embeddings file",
                                 typeConverter=TypeConverters.toString)

    embeddingsFormat = Param(Params._dummy(),
                             "embeddingsFormat",
                             "Word vectors file format",
                             typeConverter=TypeConverters.toInt)

    @keyword_only
    def __init__(self):
        super(WordEmbeddings, self).__init__(
            classname="com.johnsnowlabs.nlp.embeddings.WordEmbeddings"
        )
        self._setDefault(
            caseSensitive=False
        )

    def parse_format(self, frmt):
        if frmt == "SPARKNLP":
            return 1
        elif frmt == "TEXT":
            return 2
        elif frmt == "BINARY":
            return 3
        else:
            return frmt

    def setEmbeddingsSource(self, path, nDims, format):
        self._set(sourceEmbeddingsPath=path)
        reformat = self.parse_format(format)
        self._set(embeddingsFormat=reformat)
        return self._set(dimension=nDims)

    def setSourcePath(self, path):
        return self._set(sourceEmbeddingsPath=path)

    def getSourcePath(self):
        return self.getParamValue("sourceEmbeddingsPath")

    def setEmbeddingsFormat(self, format):
        return self._set(embeddingsFormat=self.parse_format(format))

    def getEmbeddingsFormat(self):
        value = self._getParamValue("embeddingsFormat")
        if value == 1:
            return "SPARKNLP"
        elif value == 2:
            return "TEXT"
        else:
            return "BINARY"

    def preload(self, spark):
        self._java_obj.preload(spark._jsparkSession)
        return self

    @staticmethod
    def pretrained(name="glove_100d", language="en", remote_loc=None):
        from sparknlp.pretrained import ResourceDownloader
        return ResourceDownloader.downloadModel(WordEmbeddings, name, language, remote_loc)


class BertEmbeddings(AnnotatorModel, HasEmbeddings):

    name = "BertEmbeddings"

    maxSentenceLength = Param(Params._dummy(),
                              "maxSentenceLength",
                              "Max sentence length to process",
                              typeConverter=TypeConverters.toInt)

    batchSize = Param(Params._dummy(),
                      "batchSize",
                      "Batch size. Large values allows faster processing but requires more memory.",
                      typeConverter=TypeConverters.toInt)

    def setMaxSentenceLength(self, value):
        return self._set(maxSentenceLength=value)

    def setBatchSize(self, value):
        return self._set(batchSize=value)


    @keyword_only
    def __init__(self, classname="com.johnsnowlabs.nlp.embeddings.BertEmbeddings", java_model=None):
        super(BertEmbeddings, self).__init__(
            classname=classname,
            java_model=java_model
        )
        self._setDefault(
            dimension=768,
            batchSize=5,
            maxSentenceLength=100,
            caseSensitive=False
        )

    @staticmethod
    def loadFromPython(folder):
        jModel = _BertLoader(folder)._java_obj
        return BertEmbeddings(java_model=jModel)


    @staticmethod
    def pretrained(name="bert_uncased_base", language="en", remote_loc=None):
        from sparknlp.pretrained import ResourceDownloader
        return ResourceDownloader.downloadModel(BertEmbeddings, name, language, remote_loc)
