## About

bywei = 程序员百味

个人博客 = "http://www.bywei.cn"

## Use Case
You can use this plugin to calculate the relevance score of the two feature, such as:
- Personalized Search;
- Find similar product;
- Product recommendation;

## Build Plugin from Code
1. git clone 
2. mvn clean package -DskipTests
3. create vector scoring plugin dircectoy, such as ${ES_HOME}/plugins/vector-scoring
4. copy target/releases/elasticsearch-feature-scoring-2.1.0.zip to your plugin directory and unzip it
5. restart elasticsearch

## Script Parameters
- **field**: **required**, field in index to store the vector of document;
- **inputFeatureVector**: **required**,  the condition vector, a string connected with comma;
- **version**: version of vector, if it isn't null, it should match the version of vector of document(if use version, the field value should start with 鈥�$VERSION|鈥�, such as '20170331|0.1,2.3,-1.6,0.7,-1.3');
- **baseConstant** and **factorConstant**: used to calculate the final score, default value are 1. final_score = baseConstant + factorConstant * cos(X, Y)

## Example
### create a test index
```
    PUT /test
    {
      "mappings": {
        "product": {
          "properties": {
            "productName": {
              "type": "text",
              "analyzer": "standard"
            },
            "productFeatureVector": {
              "type": "keyword"
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
```

### index some documents
```
    POST /test/product/1
    {
      "productName": "My favorite brand of shoes",
      "productFeatureVector": "0.1,2.3,-1.6,0.7,-1.3"
    }
    
    POST /test/product/2
    {
      "productName": "Normal brand of shoes",
      "productFeatureVector": "-0.5,1.6,1.1,0.9,0.7"
    }
    
    POST /test/product/3
    {
      "productName": "The shoes I don't like",
      "productFeatureVector": "1.2,0.1,0.4,-0.2,0.3"
    }
```
### normal search
```
    POST /test/_search
    {
      "query": {
        "match": {
          "productName": "shoes"
        }
      }
    }
```

the the result is: 
```
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
            "_index": "test",
            "_type": "product",
            "_id": "2",
            "_score": 0.14181954,
            "_source": {
              "productName": "Normal brand of shoes",
              "productFeatureVector": "-0.5,1.6,1.1,0.9,0.7"
            }
          },
          {
            "_index": "test",
            "_type": "product",
            "_id": "1",
            "_score": 0.1273061,
            "_source": {
              "productName": "My favorite brand of shoes",
              "productFeatureVector": "0.1,2.3,-1.6,0.7,-1.3"
            }
          },
          {
            "_index": "test",
            "_type": "product",
            "_id": "3",
            "_score": 0.1273061,
            "_source": {
              "productName": "The shoes I don't like",
              "productFeatureVector": "1.2,0.1,0.4,-0.2,0.3"
            }
          }
        ]
      }
    }
```

### search with feature score
```
    POST /test/_search
    {
  "from": 0,
  "size": 100,
  "query": {
    "bool": {
      "must": [
        {
          "term": {
            "channelCode": "CHANNELCODE"
          }
        },
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
      "jjCode",
      "isPromotion",
      "ifPromotion",
      "promotionCodes",
      "promotionRateCodes",
      "promotionRateName",
      "isScoreExchange",
      "isSale",
      "isUseCoupon",
      "promotionTypes"
    ],
    "excludes": []
  },
  "sort": [
    {
      "isSale": {
        "order": "desc"
      }
    },
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
    },
    {
      "dayPrices.dayPrice": {
        "order": "asc",
        "missing": 99999999,
        "mode": "min",
        "nested_filter": {
          "bool": {
            "must": [
              {
                "range": {
                  "dayPrices.dayBookingStartDate": {
                    "from": 1493740800000,
                    "to": 1493779620000,
                    "include_lower": true,
                    "include_upper": true
                  }
                }
              },
              {
                "range": {
                  "dayPrices.dayBookingEndDate": {
                    "from": 1493779620000,
                    "to": 1493827200000,
                    "include_lower": true,
                    "include_upper": true
                  }
                }
              }
            ]
          }
        },
        "nested_path": "dayPrices"
      }
    }
  ]
}
```
and the result is: 

```
{
	"took": 26,
	"timed_out": false,
	"_shards": {
		"total": 5,
		"successful": 5,
		"failed": 0
	},
	"hits": {
		"total": 12,
		"max_score": null,
		"hits": [{
				"_index": "hotelplatform",
				"_type": "hotelDayPrice",
				"_id": "32217CHANNELCODE493740800000",
				"_score": null,
				"_routing": "32217",
				"_parent": "32217",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT5", "RBT1"],
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": true,
					"hotelId": 32217,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "32195CHANNELCODE493740800000",
				"_score": null,
				"_routing": "32195",
				"_parent": "32195",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT5", "RBT1"],
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": true,
					"hotelId": 32195,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "32250CHANNELCODE493740800000",
				"_score": null,
				"_routing": "32250",
				"_parent": "32250",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT5"],
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": true,
					"hotelId": 32250,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "32216CHANNELCODE493740800000",
				"_score": null,
				"_routing": "32216",
				"_parent": "32216",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT5", "RBT1"],
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": true,
					"hotelId": 32216,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "32247CHANNELCODE493740800000",
				"_score": null,
				"_routing": "32247",
				"_parent": "32247",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT5", "RBT1"],
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": true,
					"hotelId": 32247,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "32219CHANNELCODE493740800000",
				"_score": null,
				"_routing": "32219",
				"_parent": "32219",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT5", "RBT1"],
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": true,
					"hotelId": 32219,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "21400CHANNELCODE493740800000",
				"_score": null,
				"_routing": "21400",
				"_parent": "21400",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": true,
					"promotionTypes": ["RBT1"],
					"promotionRateName": {},
					"ifPromotion": true,
					"promotionCodes": ["coupon98", "JJIHMC1A", "JJSPDB13A1", "mastercard"],
					"isSale": false,
					"hotelId": 21400,
					"isUseCoupon": true,
					"promotionRateCodes": ["MEMS2", "DISG1"]
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
				"_id": "22292CHANNELCODE493740800000",
				"_score": null,
				"_routing": "22292",
				"_parent": "22292",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT1"],
					"promotionRateName": {},
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": false,
					"hotelId": 22292,
					"isUseCoupon": true,
					"promotionRateCodes": ["DISG1"]
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
				"_id": "22290CHANNELCODE493740800000",
				"_score": null,
				"_routing": "22290",
				"_parent": "22290",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT1"],
					"promotionRateName": {},
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": false,
					"hotelId": 22290,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "21382CHANNELCODE493740800000",
				"_score": null,
				"_routing": "21382",
				"_parent": "21382",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["JJINN_TJ", "RBT1"],
					"promotionRateName": {},
					"ifPromotion": true,
					"promotionCodes": ["JJIHMC1A", "JJSPDB13A1", "mastercard"],
					"isSale": false,
					"hotelId": 21382,
					"isUseCoupon": true,
					"promotionRateCodes": ["MEMS2", "DISG1"]
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
				"_id": "21440CHANNELCODE493740800000",
				"_score": null,
				"_routing": "21440",
				"_parent": "21440",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": true,
					"promotionTypes": ["RBT1"],
					"promotionRateName": {},
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": false,
					"hotelId": 21440,
					"isUseCoupon": true,
					"promotionRateCodes": []
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
				"_id": "20166CHANNELCODE493740800000",
				"_score": null,
				"_routing": "20166",
				"_parent": "20166",
				"_source": {
					"isPromotion": true,
					"isScoreExchange": false,
					"promotionTypes": ["RBT1"],
					"promotionRateName": {},
					"ifPromotion": false,
					"promotionCodes": [],
					"isSale": false,
					"hotelId": 20166,
					"isUseCoupon": true,
					"promotionRateCodes": ["JCP"]
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
```
    
### A personalized search case details