Daniel's Master's Project 
===

This is an optimized, trimmed down, and easily portable version of Daniel's project meant for demos and distribution. The original project can be found at https://github.com/mwdchang/VehicleVis

There are two parts in this project: a database generator and the vis application. 

Running the generator:
---
The generator generate text files that can be imported into a sqlite3 database.

`java -DGenDB=true -DPart=<part_file> -DData=<data_file> -DWhiteList=[list] -jar VehicleVis.jar`

 - DPart : The vehicle hierarchy file
 - Data  : The raw complaint file
 - WhiteList: Optional, tells the parser to only parse the manufacturers in the list

To database can be created using a loading script
`sqlite3 <db_location> < <loading_script>`




Running the vis
---
`java -DUseDB=<db_location> -DUseTuio=[true|false] -DUseSlide=[slide_location] -jar VehicleVis.jar [[startYear] [endYear]]`


 - UseDb : Locatio of the generated sqlite3 database
 - UseTUIO : Whether to enable multitouch with TUIO, or use keyboard/mouse combination
 - UseSlide : Location of a presentation slide

 - startYear : specifies a starting time frame
 - endYear : specifies an ending time frame
