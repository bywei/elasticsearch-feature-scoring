## About

bywei = 程序员百味

个人博客 = "http://www.bywei.cn"

## Use Case
You can use this plugin to calculate the relevance score of the two feature, such as:
- Personalized Search;
- Find similar product;
- Product recommendation;

## Build Plugin from Code
1. git clone https://github.com/bywei/elasticsearch-feature-scoring.git
2. mvn clean package -DskipTests
3. create feature scoring plugin dircectoy, such as ${ES_HOME}/plugins/feature-scoring
4. copy target/releases/elasticsearch-feature-scoring-2.1.0.zip to your plugin directory and unzip it
5. restart elasticsearch

## Script Parameters
- **customSortField**: **Optional**, field in index to store the vector of document;
- **customSorts**: **customSortField is not null required **,  the condition, a Map<String, Integer>;
- **sortFields**: **Optional**,  the condition, a Map<String, Integer>;
- **version**: version of vector, if it isn't null, it should match the version of vector of document(if use version, the field value should start with ‘$VERSION|’, such as '20170331|0.1');

## About
- QQ: 1940775885
- Wx: jiaivr
- WebSite: http://www.bywei.cn

## Example
### create a test index

    PUT /hotelplatform
    {
      "mappings": {
        "hotelDayPrice": {
          "properties": {
            "hotelId": {
              "type": "long"
            },
            "day": {
              "type": "long"
            }
          }
        }
      },
      "settings": {
        "index": {
          "number_of_shards": 1,
          "number_of_replicas": 0
        }
      }
    }

### index some documents

    POST /hotelplatform/hotelDayPrice/1
    {
      "hotelId": "32195",
      "day": "1493740800000"
    }
    
    POST /hotelplatform/hotelDayPrice/2
    {
      "hotelId": "32250",
      "day": "1493740800000"
    }
    
    POST /hotelplatform/hotelDayPrice/3
    {
      "hotelId": "32217",
      "day": "1493740800000"
    }

### normal search

    POST /hotelplatform/_search
    {
      "query": {
        "match": {
          "hotelId": "32217"
        }
      }
    }

the the result is: 

    {
      "took": 8,
      "timed_out": false,
      "_shards": {
        "total": 1,
        "successful": 1,
        "failed": 0
      },
      "hits": {
        "total": 3,
        "max_score": 0.14181954,
        "hits": [
          {
            "_index": "hotelplatform",
            "_type": "hotelDayPrice",
            "_id": "2",
            "_score": 0.14181954,
            "_source": {
              "hotelId": "32250",
              "day": "1493740800000"
            }
          },
          {
            "_index": "hotelplatform",
            "_type": "hotelDayPrice",
            "_id": "1",
            "_score": 0.1273061,
            "_source": {
              "hotelId": "32195",
              "day": "1493740800000"
            }
          },
          {
            "_index": "hotelplatform",
            "_type": "hotelDayPrice",
            "_id": "3",
            "_score": 0.1273061,
            "_source": {
              "hotelId": "32217",
              "day": "1493740800000"
            }
          }
        ]
      }
    }

### search whith feature score sortFields

	{
	  "from" : 0,
	  "size" : 10,
	  "query" : {
	    "bool" : {
	      "must" : [ {
	        "term" : {
	          "channelCode" : "CHANNELCODE"
	        }
	      }, {
	        "term" : {
	          "day" : 1493827200000
	        }
	      } ]
	    }
	  },
	  "_source" : {
	    "includes" : [ "hotelId", "orderScore", "hotelScore", "serviceScore"],
	    "excludes" : [ ]
	  },
	  "sort" : [  {
	    "_script" : {
	      "script" : {
	        "inline" : "feature-scoring",
	        "lang" : "native",
	        "params" : {
	          "sortFields" : {
	            "orderScore" : 0.3,
	            "hotelScore" : 0.2,
	            "serviceScore" : 0.5
	          }
	        }
	      },
	      "type" : "number",
	      "reverse" : true
	    }
	  }]
	}
	
and the result is: sort by ( orderScore * 0.3 + hotelScore * 0.2 + serviceScore * 0.5 )


### search with feature score customSorts and customSortField

    POST /hotelplatform/_search
    {
	  "from": 0,
	  "size": 100,
	  "query": {
	    "bool": {
	      "must": [
	        {
	          "term": {
	            "day": 1493740800000
	          }
	        }
	      ]
	    }
	  },
	  "_source": {
	    "includes": [
	      "hotelId",
	      "day"
	    ],
	    "excludes": []
	  },
	  "sort": [
	    {
	      "_script": {
	        "script": {
	          "inline": "feature-scoring",
	          "lang": "native",
	          "params": {
	            "customSortField": "hotelId",
	            "customSorts": {
	              "32195": 200,
	              "32217": 300
	            }
	          }
	        },
	        "type": "number",
	        "reverse": true
	      }
	    }
	  ]
	}

and the result is: 

	   {
	    "took": 36,
	    "timed_out": false,
	    "_shards": {
	        "total": 5,
	        "successful": 5,
	        "failed": 0
	    },
	    "hits": {
	        "total": 12,
	        "max_score": null,
	        "hits": [
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "32217CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "32217",
	                "_parent": "32217",
	                "_source": {
	                    "hotelId": 32217,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    1,
	                    300,
	                    440
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "32195CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "32195",
	                "_parent": "32195",
	                "_source": {
	                    "hotelId": 32195,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    1,
	                    200,
	                    554
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "32250CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "32250",
	                "_parent": "32250",
	                "_source": {
	                    "hotelId": 32250,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    1,
	                    0,
	                    596
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "32216CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "32216",
	                "_parent": "32216",
	                "_source": {
	                    "hotelId": 32216,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    1,
	                    0,
	                    646
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "32247CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "32247",
	                "_parent": "32247",
	                "_source": {
	                    "hotelId": 32247,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    1,
	                    0,
	                    4079
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "32219CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "32219",
	                "_parent": "32219",
	                "_source": {
	                    "hotelId": 32219,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    1,
	                    0,
	                    4909
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "21400CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "21400",
	                "_parent": "21400",
	                "_source": {
	                    "hotelId": 21400,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    0,
	                    0,
	                    1
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "22292CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "22292",
	                "_parent": "22292",
	                "_source": {
	                    "hotelId": 22292,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    0,
	                    0,
	                    12
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "22290CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "22290",
	                "_parent": "22290",
	                "_source": {
	                    "hotelId": 22290,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    0,
	                    0,
	                    18
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "21382CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "21382",
	                "_parent": "21382",
	                "_source": {
	                    "hotelId": 21382,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    0,
	                    0,
	                    150
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "21440CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "21440",
	                "_parent": "21440",
	                "_source": {
	                    "hotelId": 21440,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    0,
	                    0,
	                    488
	                ]
	            },
	            {
	                "_index": "hotelplatform",
	                "_type": "hotelDayPrice",
	                "_id": "20166CHANNELCODE1493740800000",
	                "_score": null,
	                "_routing": "20166",
	                "_parent": "20166",
	                "_source": {
	                    "hotelId": 20166,
	                    "day": 1493740800000
	                },
	                "sort": [
	                    0,
	                    0,
	                    755
	                ]
	            }
	        ]
	    }
	}
    
### A personalized search case details
 see BYWEI app DEMO
