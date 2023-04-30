# Exchange Simulator

## Description

An equity exchange simulator that provides the following features:

* Noise trading agents
* Order entry & clearing
* Statistics monitoring
* Auction management (intra-day timed & price monitoring)

## Why?

After spending a few years working on a systematic trading engines in various shops I wanted to dig deeper into the 
behaviour of limit order books of lit markets.  Does a trading strategy actually need to take account of the market 
mechanics, as well as whatever other alpha seeking strategy it might have? Perhaps we could explore this by building a
model of a _real_ exchange using purely random inputs?  

If this produced outputs and behaviours that looked like real markets that might go some way to demonstrating that
the random behaviour **on its own** produces meaningful price movements that systematic traders can capture the spread
of.

The intention, once written was to see if there is a way to derive a real trading strategy from this. I suspect there
might be, but it would need to take into account lots of other factors to avoid losing a lot of money.  I leave that as 
an exercise for the reader :smiley:.

## How does it work?

The simulator has two different types of actors, agents and market makers.  They simply wait for a configurable random 
period before making an assessment of their holdings, open orders, and the market and then take a random action.

The market makers are more passive and designed to stay out of the way in this simulation, their role is to support the 
market should there be a sudden drop in liquidity by putting up firm two-way prices of sufficient size.   However, when 
all the agents are acting in the same way we might not expect any interaction with these market makers and the rest of 
the market.  This is probably not how market makers operate in real life :smiley:.

### Zero intelligence

Noise traders don't really have zero intelligence.  It's just that they all have competing views and situations and the 
reasons they are trading don't follow any particular pattern.  These are the most interesting because they represent the 
behaviour of all of us who are not trading to 'provide liquidity' or make short term bets.  They don't possess any 
particular knowledge or advantage over any other noise trader.

In this simulation a zero intelligence agent will choose one of these random actions after a random period:

* Place a new, or amend an existing, order for a random quantity that is:
  * Market
  * With a limit inside the spread
  * With a limit outside the spread
* Cancel an existing random order

## Getting Started

### Building the application

If you would like to try the simulator for yourself you'll need to hava a Maven installed.  
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

Amending the simulator is one way to affect the simulation, another (perhaps more interesting way) is by changing the 
agent behaviour.  You can do by changing the `ZeroIntelligenceAgent` class.

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

* **Agent sbehaviour analysis** Come up with a set of different agent configurations and see how that affects prices.
  Does it do so consistently (i.e. if we re-run with the same config but a different random seed do we see the same sort
  of results).
* **Tick sizes** Following MiFID2 European exchanges have a tick size regime which changes the tick
  size based on the price and the turnover over of the stock. The boundaries are arbitrary numbers so given a pre-canned order flow
  (but with un-rounded ticks!) how does the price evolve with different tick sizes.
* **High Performance** There is an ulterior motive for building this.  I'd also like to explore strategies for doing high 
  performance low-latency Java.  But the first task is to build something that works, so the current version is probably
  not super performant.  
* **Bad agents** Currently all the participants on the exchange are noise traders. What if we inject a quote-stuffing,
  layering bad actor into the noise. What's the minimum of bad behaviour that creates a favourable outcome for the
  actor?  Could the exchange detect and suppress the bad actor without any prior knowledge of who it is?

## History

The ExchangeSimulator is based on the following references.

* Asynchronous Simulations of a Limit Order Book (2006), Gilles Daniel, University of Manchester
* Limit Order Books, Quantitative Finance (2013), Vol 13, No 11, p1709-174 - Gould, Porter, Williams, et al.


