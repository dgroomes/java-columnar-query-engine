# java-in-memory-columnar-query-engine

NOT YET IMPLEMENTED

A toy Java implementation of a query engine over in-memory, columnar, schema-ful data.


## Overview

I want to model and search over a cyclic structured set of data. Think "car models" made by "car makers" influenced by
the design and success of other car models and car makers.

**NOTE**: This project was developed on macOS. It is for my own personal use.

I want all the following characteristics:

* In-memory
  * This is mostly for convenience. I don't want to deal with file IO.
* Vectorization
  * I want to at least vaguely think about vectorized CPU computation in light of Java's [(incubating) vector API](https://openjdk.org/jeps/426). I probably won't implement to this because it's kind of beyond me plus I don't know
  if it even applies.
* Columnar
  * The data is physically laid out as columns (Java arrays).
* Schema-ful
  * The data adheres to a schema. Look back to my car models and car makers comment earlier. In other words, I don't
  care about implementing a query engine over a traditional "graph database" where the relationship of edges and vertices
  is unconstrained, like in Neo4J. The effect of having schema-ful data is that the data can be described as columns,
  and columnar data lays out physically as arrays. I believe what I want is something called an ["object database"](https://en.wikipedia.org/wiki/Object_database). This type of database is somewhat obscure but actually there
  is one modern one called [Realm](https://en.wikipedia.org/wiki/Realm_(database)) which is popular on mobile. Also I've
  been inspired by [Kuzu](https://github.com/kuzudb/kuzu) which is a property graph database but it has schemas (which I
  like) so doesn't that make it a traditional object database? I tried to build Kuzu from source but had issues (it's
  extremely new; so that's ok) so maybe I'll try Realm (although it's also C++ so I'm scared).

Apache Arrow is the natural choice for modeling in-memory columnar data in 2023 but I've already learned that in my
other repository: <https://github.com/dgroomes/arrow-playground>. It has strong reference implementations for Java,
which is my go-to language. The Java Arrow implementation also offers table-like data modeling which goes a long way to
making the developer experience pretty good for modeling the aforementioned "car makers" and "car models" data. But the
convenience stops there. The Java Arrow implementation does not offer entity-to-entity relationships and it offers only
very basic implementations of algorithms: specifically binary search and sorting. And I find it a bit cumbersome (but I
still appreciate it; thank you open source developers). I want an actual basic query engine. So, this repo is me doing
that (fingers crossed).


## Design

The domain data of this project is not the aforementioned "car makers" and "car models" data but instead it is ZIP code
data. This data is small (3MB) but it can be multiplied into "parallel universes" if needed. It's relatable. It's real.
And the surface area of the schema is about as small as possible: ZIP codes, population, containing city name, containing
state name and state code, and then state-to-state adjacencies. Nice. I don't want to be bogged down with more tables/columns
than that.

When it comes to the workload of the queries, I want to execute a query like:

> Find all ZIP codes that have a population of 10,000 or more and are adjacent to a state with at least one city named
> "Springfield".


Or something cyclic like:

> Find all states 

Simple requirements. That allows the focus to be on the implementation.


## Prior Art

I would love to know other implementations of something like this. I think there are many implementations of this and much
more sophisticated systems that take it into the stratosphere like Apache Spark (Scala/Java) and maybe [Data Fusion](https://arrow.apache.org/datafusion/user-guide/introduction.html) and other implementations of data frames. I'm not very
educated in the space. While it would be fruitful to peruse the code of these open source projects and toy around with
it I still think the barrier to entry is quite high; especially when you compare it to the library-less Java
implementation like I want to do here (well I'm using SLF4J and Jackson to load the data). I like Rust for this workload
but no I don't want to be constrained by ownership/borrowing. I'm in "query engine learning mode" not "Rust learning mode".


## Instructions

Follow these instructions to build and run the example program:

1. Use Java 19
2. Build and run the program:
   * ```shell
     ./gradlew run
     ```
3. Run the tests:
   * ```shell
     ./gradlew test
     ```


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] IN PROGRESS Replace Arrow usage with straight array usage. (Remember, I copied this project over from a subproject in my `arrow-playground`
  repo). I think I'll create loading code (file I/O, JSON parsing) and keep it decoupled from the generic columnar
  code (it's own JPMS module) and then create a bridge module that describes the geograhies data using the APIs of the
  columnar/engine module.
  * DONE (it's just a package not a JPMS module) Create the loader module. (glue code)
  * Create the columnar data store module. (generic/API/high-value code)
  * Create the bridge module (glue code)
* [x] DONE Model the data in Apache Arrow's table abstractions. Use `Table` even knowing it is experimental.
* [ ] Model cyclic graphs in the data using the ["state adjacencies" of my cypher-playground](https://github.com/dgroomes/cypher-playground/blob/dc836b1ac934175394ece264c443bfae47465cd6/postgres-init/2-init-states-data.sql#L1)
  and do a query by something like "find states adjacent to states that have at least a ZIP code with a population of 1,000,000"
  (or a more illustrative query if you can think of one)
  * DONE Define the adjacencies data.
  * DONE Define the state data (code and name).
  * DONE Incorporate the state data into the Arrow data model.
  * How to relate the ZIP code "rows" to the entries in the state vector? I can do it by hand, but is this what Arrow's
    dictionary encoder is for?
  * Load the adjacencies data into vectors
* [ ] Create a generic graph API plus a (overtly simple) query execution engine. The graph API only
  supports schema-ful graphs (does this matter?). The query execution engine should prune the vector lists (i can't find
  words for this right now).
* [ ] Consider renaming the project to something like "object-query-engine" or something more specific/descriptive.
* [ ] Consider compressing integer arrays with [this integer compression library](https://github.com/lemire/JavaFastPFOR) which
      uses the [(incubating) Java vector API](https://openjdk.org/jeps/426). This would be kind of epic.
