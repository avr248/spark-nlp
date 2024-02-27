#  Copyright 2017-2022 John Snow Labs
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
"""Contains classes for MPNetForSequenceClassification."""

from sparknlp.common import *


class MPNetForSequenceClassification(AnnotatorModel,
                                     HasCaseSensitiveProperties,
                                     HasBatchedAnnotate,
                                     HasClassifierActivationProperties,
                                     HasEngine,
                                     HasMaxSentenceLengthLimit):
    """MPNetForSequenceClassification can load MPNet Models with sequence classification/regression head on
    top (a linear layer on top of the pooled output) e.g. for multi-class document classification tasks.

    Pretrained models can be loaded with :meth:`.pretrained` of the companion
    object:

    >>> sequenceClassifier = MPNetForSequenceClassification.pretrained() \\
    ...     .setInputCols(["token", "document"]) \\
    ...     .setOutputCol("label")

    The default model is ``"mpnet_sequence_classifier_ukr_message"``, if no name is
    provided.

    For available pretrained models please see the `Models Hub
    <https://sparknlp.org/models?task=Text+Classification>`__.

    To see which models are compatible and how to import them see
    `Import Transformers into Spark NLP 🚀
    <https://github.com/JohnSnowLabs/spark-nlp/discussions/5669>`_.

    ====================== ======================
    Input Annotation types Output Annotation type
    ====================== ======================
    ``DOCUMENT, TOKEN``    ``CATEGORY``
    ====================== ======================

    Parameters
    ----------
    batchSize
        Batch size. Large values allows faster processing but requires more
        memory, by default 8
    caseSensitive
        Whether to ignore case in tokens for embeddings matching, by default
        True
    maxSentenceLength
        Max sentence length to process, by default 128
    coalesceSentences
        Instead of 1 class per sentence (if inputCols is `sentence`) output
        1 class per document by averaging probabilities in all sentences, by
        default False.
    activation
        Whether to calculate logits via Softmax or Sigmoid, by default
        `"softmax"`.

    Examples
    --------
    >>> import sparknlp
    >>> from sparknlp.base import *
    >>> from sparknlp.annotator import *
    >>> from pyspark.ml import Pipeline
    >>> document = DocumentAssembler() \\
    ...     .setInputCol("text") \\
    ...     .setOutputCol("document")
    >>> tokenizer = Tokenizer() \\
    ...     .setInputCols(["document"]) \\
    ...     .setOutputCol("token")
    >>> sequenceClassifier = MPNetForSequenceClassification \\
    ...     .pretrained() \\
    ...     .setInputCols(["document", "token"]) \\
    ...     .setOutputCol("label")
    >>> data = spark.createDataFrame([
    ...     ["I love driving my car."],
    ...     ["The next bus will arrive in 20 minutes."],
    ...     ["pineapple on pizza is the worst 🤮"],
    ... ]).toDF("text")
    >>> pipeline = Pipeline().setStages([document, tokenizer, sequenceClassifier])
    >>> pipelineModel = pipeline.fit(data)
    >>> results = pipelineModel.transform(data)
    >>> results.select("label.result").show()
    +--------------------+
    |              result|
    +--------------------+
    |     [TRANSPORT/CAR]|
    |[TRANSPORT/MOVEMENT]|
    |              [FOOD]|
    +--------------------+
    """
    name = "MPNetForSequenceClassification"

    inputAnnotatorTypes = [AnnotatorType.DOCUMENT, AnnotatorType.TOKEN]

    outputAnnotatorType = AnnotatorType.CATEGORY


    coalesceSentences = Param(Params._dummy(), "coalesceSentences",
                              "Instead of 1 class per sentence (if inputCols is '''sentence''') output 1 class per document by averaging probabilities in all sentences.",
                              TypeConverters.toBoolean)

    def getClasses(self):
        """
        Returns labels used to train this model
        """
        return self._call_java("getClasses")


    def setCoalesceSentences(self, value):
        """Instead of 1 class per sentence (if inputCols is '''sentence''') output 1 class per document by averaging probabilities in all sentences.
        Due to max sequence length limit in almost all transformer models such as BERT (512 tokens), this parameter helps feeding all the sentences
        into the model and averaging all the probabilities for the entire document instead of probabilities per sentence. (Default: true)

        Parameters
        ----------
        value : bool
            If the output of all sentences will be averaged to one output
        """
        return self._set(coalesceSentences=value)

    @keyword_only
    def __init__(self, classname="com.johnsnowlabs.nlp.annotators.classifier.dl.MPNetForSequenceClassification",
                 java_model=None):
        super(MPNetForSequenceClassification, self).__init__(
            classname=classname,
            java_model=java_model
        )
        self._setDefault(
            batchSize=8,
            maxSentenceLength=128,
            caseSensitive=True,
            coalesceSentences=False,
            activation="softmax"
        )

    @staticmethod
    def loadSavedModel(folder, spark_session):
        """Loads a locally saved model.

        Parameters
        ----------
        folder : str
            Folder of the saved model
        spark_session : pyspark.sql.SparkSession
            The current SparkSession

        Returns
        -------
        MPNetForSequenceClassification
            The restored model
        """
        from sparknlp.internal import _MPNetForSequenceClassificationLoader
        jModel = _MPNetForSequenceClassificationLoader(folder, spark_session._jsparkSession)._java_obj
        return MPNetForSequenceClassification(java_model=jModel)

    @staticmethod
    def pretrained(name="mpnet_sequence_classifier_ukr_message", lang="en", remote_loc=None):
        """Downloads and loads a pretrained model.

        Parameters
        ----------
        name : str, optional
            Name of the pretrained model, by default
            "MPNet_base_sequence_classifier_imdb"
        lang : str, optional
            Language of the pretrained model, by default "en"
        remote_loc : str, optional
            Optional remote address of the resource, by default None. Will use
            Spark NLPs repositories otherwise.

        Returns
        -------
        MPNetForSequenceClassification
            The restored model
        """
        from sparknlp.pretrained import ResourceDownloader
        return ResourceDownloader.downloadModel(MPNetForSequenceClassification, name, lang, remote_loc)
