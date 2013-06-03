Daniel's Master's Project 
===

This is an optimized, trimmed down, and easily portable version of Daniel's project meant for demos and distribution. The original project can be found at https://github.com/mwdchang/VehicleVis

There are two parts in this project: a database generator and the vis application. 

Running the generator:
---
'java -DGenDB=true -DPart=<part_file> -DData=<data_file> -DWhiteList=[list] -jar VehicleVis.jar'

Running the vis
---
'java -DUseDB=<db_location> -DUseTuio=[true|false] -DUseSlide=[slide_location] -jar VehicleVis.jar [[startYear] [endYear]]
