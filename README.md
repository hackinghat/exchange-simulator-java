
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

Getting Started
---------------

### Building the application

If you would like to try this for yourself you'll need to hava a Maven installed.  
And then run the following command in the checkout path.  That will compile the application into a single jar with all
the dependencies, suitable for running inside a container. 

    mvn install

### Running the application

To run the application with the minimum of fuss you'll need to have installed and configured 
[docker compose](https://docs.docker.com/compose/).  Once you've done that and also built the application you can run
the application and all the dependencies with:

    docker-compose up

It will log a lot of stuff to the console, but all being well  the interesting output of the application gets placed in 
an output folder called `out`:

    ls ./out
    AGENT.20230416-130055959923.csv  AGENT-ZERO.20230416-130055959923.csv  ...

### Tweaking the configuration

The main runnable class is the OrderBookSimulatorImp, to adjust how the simulator behaves modify this class.  You can 
amend this class to adjust simulator level things like the speed of simulator, the number of agents, and the default 
behaviour for market makers.

Amending the simulator is one thing but changing the agent behaviour is perhaps the most
interesting thing to do which you can do by changing the `ZeroIntelligenceAgent` class.

If you know how to use [VisualVM](https://visualvm.github.io/) there are also some live settable config exposed by 
JMXBeans.

History
-------

The ExchangeSimulator is based on the following  references.   

* Asynchronous Simulations of a Limit Order Book (2006), Gilles Daniel, University of Manchester
* Limit Order Books, Quantitative Finance (2013), Vol 13, No 11, p1709-174 - Gould, Porter, Williams, et al.


