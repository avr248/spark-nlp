---
layout: model
title: Multilingual bert_base_multilingual_cased_finetuned_naija BertEmbeddings from Davlan
author: John Snow Labs
name: bert_base_multilingual_cased_finetuned_naija
date: 2023-09-09
tags: [bert, xx, open_source, fill_mask, onnx]
task: Embeddings
language: xx
edition: Spark NLP 5.1.1
spark_version: 3.0
supported: true
engine: onnx
annotator: BertEmbeddings
article_header:
  type: cover
use_language_switcher: "Python-Scala-Java"
---

## Description

Pretrained BertEmbeddings  model, adapted from Hugging Face and curated to provide scalability and production-readiness using Spark NLP.`bert_base_multilingual_cased_finetuned_naija` is a Multilingual model originally trained by Davlan.

{:.btn-box}
<button class="button button-orange" disabled>Live Demo</button>
<button class="button button-orange" disabled>Open in Colab</button>
[Download](https://s3.amazonaws.com/auxdata.johnsnowlabs.com/public/models/bert_base_multilingual_cased_finetuned_naija_xx_5.1.1_3.0_1694279152866.zip){:.button.button-orange.button-orange-trans.arr.button-icon}
[Copy S3 URI](s3://auxdata.johnsnowlabs.com/public/models/bert_base_multilingual_cased_finetuned_naija_xx_5.1.1_3.0_1694279152866.zip){:.button.button-orange.button-orange-trans.button-icon.button-copy-s3}

## How to use



<div class="tabs-box" markdown="1">
{% include programmingLanguageSelectScalaPythonNLU.html %}
```python


document_assembler = DocumentAssembler() \
    .setInputCol("text") \
    .setOutputCol("documents")
    
    
embeddings =BertEmbeddings.pretrained("bert_base_multilingual_cased_finetuned_naija","xx") \
            .setInputCols(["documents","token"]) \
            .setOutputCol("embeddings")

pipeline = Pipeline().setStages([document_assembler, embeddings])

pipelineModel = pipeline.fit(data)

pipelineDF = pipelineModel.transform(data)

```
```scala


val document_assembler = new DocumentAssembler()
    .setInputCol("text") 
    .setOutputCol("embeddings")
    
val embeddings = BertEmbeddings 
    .pretrained("bert_base_multilingual_cased_finetuned_naija", "xx")
    .setInputCols(Array("documents","token")) 
    .setOutputCol("embeddings") 

val pipeline = new Pipeline().setStages(Array(document_assembler, embeddings))

val pipelineModel = pipeline.fit(data)

val pipelineDF = pipelineModel.transform(data)


```
</div>

{:.model-param}
## Model Information

{:.table-model}
|---|---|
|Model Name:|bert_base_multilingual_cased_finetuned_naija|
|Compatibility:|Spark NLP 5.1.1+|
|License:|Open Source|
|Edition:|Official|
|Input Labels:|[documents, token]|
|Output Labels:|[embeddings]|
|Language:|xx|
|Size:|665.0 MB|

## References

https://huggingface.co/Davlan/bert-base-multilingual-cased-finetuned-naija