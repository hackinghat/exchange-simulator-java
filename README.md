# Exchange Simulator

## Description

An equity exchange simulator that provides the following features:

* Noise trading agents
* Order entry & clearing
* Statistics monitoring
* Auction management (timed & monitoring)

## Why?

After spending a few years working on a systematic trading engine I wanted to dig deeper into the behaviour of limit
order books of lit markets. The behaviour we were trying to profit from seemed largely random to me. Trying to read
meaning into random behaviour is a form of madness. So what if I simulated the behaviour of a real exchange using
purely random inputs?

If this produced outputs and behaviours that looked like real markets that might go some way to demonstrating that
the random behaviour **on its own** produces meaningful price movements that systematic traders can capture the spread
on.

The intention, once written was to see if there is a way to derive a real trading strategy from this. I suspect there
is but I leave that as an exercise for the reader :smiley:.

## Getting Started

### Building the application

If you would like to try this for yourself you'll need to hava a Maven installed.  
And then run the following command in the checkout path. That will compile the application into a single jar with all
the dependencies, suitable for running inside a container.

    mvn install

### Running the application

To run the application with the minimum of fuss you'll need to have installed and configured
[docker compose](https://docs.docker.com/compose/). Once you've done that and also built the application you can run
the application and all the dependencies with:

    docker-compose up

It will log a lot of stuff to the console, but all being well the interesting output of the application gets placed in
an output folder called `out`:

    ls ./out
    AGENT.20230416-130055959923.csv  AGENT-ZERO.20230416-130055959923.csv  ...

### Tweaking the configuration

The main runnable class is the OrderBookSimulatorImp, to adjust how the simulator behaves modify this class. You can
amend this class to adjust simulator level things like the speed of simulator, the number of agents, and the default
behaviour for market makers.

Amending the simulator is one thing but changing the agent behaviour is perhaps the most
interesting thing to do which you can do by changing the `ZeroIntelligenceAgent` class.

If you know how to use [VisualVM](https://visualvm.github.io/) there are also some live settable config exposed by
JMXBeans.

## Analysis and future work

The main purpose of this software is to be able to do research on trading venues by having complete control over a toy
one. The first thing to verify with the exchange simulator is that it actually behaves like a real venue. You can see
some visual inspection of the
results [here](https://github.com/hackinghat/exchange-simulator-java/blob/main/docs/Output%20Analysis.ipynb)

If you have [Jupyter](https://jupyter.org/) installed you can re-run the notebook on your own outputs.

### Future

I want to do some more work to verify the correctness of the simulator but I'd also like to try some
other things that seem interesting:

* **Tick sizes** Following MiFID2 most European exchanges have a tick size regime which changes the tick
  size based on the turnover over of the stock. The boundaries are arbitrary numbers so given a pre-canned order flow
  (but with un-rounded ticks!) how does the price evolve with different tick sizes.
* **Telemetry** I'd like this to be as high performance as possible (currently the simulator has some perceived
  performance issues). So some throughput telemetry.
* **Bad agents** Currently all the participants on the exchange are noise traders. What if we inject a quote-stuffing,
  layering bad actor into the noise. What's the minimum of bad behaviour that creates a favourable outcome for the
  actor?
  Could the agent detect and suppress the bad actor without any prior knowledge of who it is?

## History

The ExchangeSimulator is based on the following references.

* Asynchronous Simulations of a Limit Order Book (2006), Gilles Daniel, University of Manchester
* Limit Order Books, Quantitative Finance (2013), Vol 13, No 11, p1709-174 - Gould, Porter, Williams, et al.


