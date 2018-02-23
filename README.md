# Complex Named Entities Similarity Computation
This project is the implementation of complex NE computation approaches proposed by Fotsoh et al. 
The corresponding paper can be found here : https://dl.acm.org/citation.cfm?id=3155903&dl=ACM&coll=DL
In this implementation, we suppose that data are store in Elasticsearch.
Data that has been use in our implementation are available here : https://github.com/armelCPU/Similarity-Ressources.
 * events.json: contains elastisearch dump of event's NE.
 	each of them is referenced by their id field.
 	File events_score.xlsx give event's pairs and their similarity scores according to expert
 * representaions.json: contains performance's NE also referenced by if field.
 	File representation_scores.xlsx give representation pairs and their similairty scores

## Getting Started
This project is written in JAVA and is based on Maven.
You need to install all required packages before running this code.
You just have to run command
```
	mvn run clean install
```

## Description
 ### Models
 	They are contained in the folder: src/main/java/com/cogniteev/cognisearch/event/model.
 	Each one describes structure of data need in the similarity computation.
 	We have for example event or Cluster description.

 ### Performances Similarity : src/main/java/com/cogniteev/cognisearch/event/repSimilarity
 	This package is dedicated to similarity computation between performances.
 	Each approach is implemented in a single file with the name of the approach.

### Events Similarity : src/main/java/com/cogniteev/cognisearch/event/similarity
	This package is dedicated to similarity computation between events.
 	Each approach is implemented in a single file with the name of the approach.

### Utils: src/main/java/com/cogniteev/cognisearch/event/Utils.java
	This file contains the implementation of properties's similarity computation.