
# Exchange Simulator

Description
-----------

An equity exchange simulator that provides the following features:

* Noise trading agents
* Order entry & clearing
* Statistics monitoring
* Auction management (timed & monitoring)

This project uses bug.py to track issues.

History
-------

The ExchangeSimulator was slowly developed over a period of years, based on the following
references. 

* Asynchronous Simulations of a Limit Order Book (2006), Gilles Daniel, University of Manchester
* Limit Order Books, Quantitative Finance (2013), Vol 13, No 11, p1709-174 - Gould, Porter, Williams, et al.
* 

Dependencies
------------
* Java
    * JDK 1.8 (OpenJDK)
	* JavaFX 2 (OpenJFX)
	* Maven 3
	* JUnit 4.12
	* log4j 2.7
	
	
Troubleshooting
---------------

On Linux OpenJDK does not come with JavaFX installed it needs to be installed
separately.  
 
For Fedora 27 the installation of the OpenJDK was completed by:

dnf install java-1.8.0-openjdk-devel openjfx-devel java-1.8.0-openjdk-openjfx

Links
-----

For testing purposes a sample of NASDAQ ITCH data was downloaded from here: ftp://emi.nasdaq.com/ITCH/
Description of the format is provided here: https://www.nasdaqtrader.com/content/technicalsupport/specifications/dataproducts/PSXTV-ITCH-V4_1.pdf
