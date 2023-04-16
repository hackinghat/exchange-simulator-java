
# Exchange Simulator

Description
-----------

An equity exchange simulator that provides the following features:

* Noise trading agents
* Order entry & clearing
* Statistics monitoring
* Auction management (timed & monitoring)

Why?
----

After spending a few years working on a systematic trading engine I wanted to dig deeper into the behaviour of limit 
order books of lit markets.  The behaviour we were trying to profit from seemed largely random to me.  Trying to read
meaning into random behaviour is a form of madness.  So what if I simulated the behaviour of a real exchange using 
purely random inputs?  

If this produced outputs and behaviours that looked like real markets that might go some way to demonstrating that 
the random behaviour **on its own** produces meaningful price movements that systematic traders can capture the spread
on.  

The intention, once written was to see if there is a way to derive a real trading strategy from this.  I suspect there
is but I leave that as an exercise for the reader :smiley:.



History
-------

The ExchangeSimulator was slowly developed over a period of years, based on the following  references.   

* Asynchronous Simulations of a Limit Order Book (2006), Gilles Daniel, University of Manchester
* Limit Order Books, Quantitative Finance (2013), Vol 13, No 11, p1709-174 - Gould, Porter, Williams, et al.


