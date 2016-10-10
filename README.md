# Play csv vs elasticsearch
Just a small REST api to search dynamically into csv files with relationship
mappings.
Fast implementation as well of elasticsearch wrapper for better search
performances. A csv file can be loaded directly into elastic db with appropriate 
JSON Formats.
To use [elasticsearch query api](https://www.elastic.co/guide/en/elasticsearch/reference/2.3/search.html) do that first:
- install elastic server **2.3.X** only, run it `./bin/elasticsearch` make sure curl [localhost:9200](http://localhost:9200) works
- edit `application.conf` with proper cluster name as set in elastic conf
- `sbt run` the app and check elastic server talks with the app [localhost:9000/es/stats](http://localhost:9000/es/stats)
- post to [localhost:9000/es/index](http://localhost:9000/es/index) to create the main db index (lunatech)
- post to [localhost:9000/es/load](http://localhost:9000/es/load) to load the airport collection from csv files in /resources
- [localhost:9200/lunatech/airport/_search?q=iso_country:US](http://localhost:9200/lunatech/airport/_search?q=iso_country:US) should 
give you airports from the USA

    ## Routes
    
    GET     /es/stats        elasticsearch server probe
    POST    /es/index        creates the elasticsearch db index
    POST    /es/load         populates the elastic db with csv data
    GET     /es/airport/:id  gets an airport by id in elastic search
    GET     /es/airports/by-country-code/:country      gets an airport by country code in elasticsearch (under development)
    
    GET     /search/airports/by-country/:country    gets an airport by country code or country name (fuzzy search as well)
    GET     /report/countries/airports-count        gets 10 top and low count of airports per country
    GET     /report/countries/runways               gets runway types per country
    GET     /report/runways/identifier              gets most used runway identifiers
    GET     /report/airports/all                    gets all airports data (very heavy)
