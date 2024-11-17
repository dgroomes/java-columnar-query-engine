# java-columnar-query-engine

A toy Java implementation of a query engine over columnar, schema-ful, in-memory data.


## Overview

I want to search over a cyclic structured set of data. Think "cities" belonging to "states" which are adjacent to other
states which contain cities.

**NOTE**: This project was developed on macOS. It is for my own personal use.

I want all the following characteristics:

* Columnar
  * The data is physically laid out as columns (e.g. Java arrays).
* Schema-ful
  * The data adheres to a schema. Look back to my cities and states comment earlier. In other words, I don't
  care about implementing a query engine over a traditional "graph database" where the relationship of edges and vertices
  is unconstrained, like in Neo4J. The effect of having schema-ful data is that the data can be described as columns,
  and columnar data lays out physically as arrays. I believe what I want is something called an ["object database"](https://en.wikipedia.org/wiki/Object_database). This type of database is somewhat obscure but actually there
  is one modern one called [Realm](https://en.wikipedia.org/wiki/Realm_(database)) which is popular on mobile. Also, I've
  been inspired by [Kuzu](https://github.com/kuzudb/kuzu) which is a property graph database, but it has schemas (which I
  like) so doesn't that make it a traditional object database? I tried to build Kuzu from source but had issues (it's
  extremely new; so that's ok) so maybe I'll try Realm (although it's also C++ so I'm scared).
* In-memory
  * This is mostly for convenience. I don't want to deal with file IO.
* Foreign Memory API (stretch goal)
  * I want to lay the memory out not in arrays or objects but in contiguous chunks of off-heap memory. See [JEP 442: Foreign Function & Memory API (Third Preview)](https://openjdk.org/jeps/442).
* Vectorization (stretch goal)
  * I want to at least vaguely think about vectorized CPU computation in light of Java's [(incubating) vector API](https://openjdk.org/jeps/426). I probably won't implement to this because it's kind of beyond me plus I don't know
  if it even applies.

Apache Arrow is the natural choice for modeling in-memory columnar data in 2023, but I've already learned that API
in my other repository: <https://github.com/dgroomes/arrow-playground>. It has a strong reference implementation for Java,
which is my go-to language. The Java Arrow implementation also offers table-like data modeling which goes a long way to
making the developer experience pretty good for modeling the aforementioned "cities and states" data. But the convenience
stops there. The Java Arrow implementation does not offer entity-to-entity relationships and it offers only very basic
implementations of algorithms: specifically binary search and sorting. And I find it a bit cumbersome (but I
still appreciate it; thank you open source developers). I want an actual basic query engine. So, this repo is me doing
that (fingers crossed).


## Design

The domain data of this project is ZIP code data, contained in cities, contained in states and with state adjacencies.
This data is small (3MB) but it can be multiplied into "parallel universes" if needed. It's relatable. It's real.
And the surface area of the schema is about as small as possible: ZIP codes, population, containing city name, containing
state name and state code, and then state-to-state adjacency relationships. Nice. I don't want to be bogged down with
more tables/columns than that.

When it comes to the workload of the queries, I want to execute a query like:

> Find all ZIP codes that have a population around 10,000 and are in a state adjacent to a state with at least one
> city named "Plymouth".


Or something cyclic (although contrived) like:

> Find all states named with "North" that are adjacent to a state with "South" that are adjacent to a state with "North".

Simple requirements. That allows the focus to be on the implementation.

The code is implemented across a few modules:

* `data-system`
  * This module describes the API of the "read-only object datastore and query engine". It also defines the core
    interfaces of tables, columns and associations. A data system is characterized by its physical data strategy (e.g.
    a columnar SQL database) and its query execution strategy.
* `data-system-serial-indices-arrays`
  * This module is an implementation of a data system. It is characterized by a serial-style (non-parallel) and indices-
    tracking execution strategy and the physical data is laid out in arrays. This is the most interesting module.
* `data-model-in-memory`
  * This module is a concrete implementation of the data model API using in-memory data structures (i.e. no file IO).
* `geography`
  * This module is a pure domain model. It has zero dependencies by design. It models the ZIP, city and
  state data which we can collectively refer to as "geography" for short.
* `geography-loader`
  * This module is responsible for loading the data from disk (JSON) into memory (Java domain objects). It's a
  "glue" module that is not very interesting.
* `geography-query`
  * NOT YET IMPLEMENTED
  * This module is an application of the query engine over the geography domain. This is an interesting module. 
* `app`
  * This module is the entrypoint of the project. It's a "glue" module.


## Prior Art

I would love to know other implementations of something like this. I think there are many implementations of this and much
more sophisticated systems that take it into the stratosphere like Apache Spark (Scala/Java) and maybe [Data Fusion](https://arrow.apache.org/datafusion/user-guide/introduction.html) and other implementations of data frames. I'm not very
educated in the space. While it would be fruitful to peruse the code of these open source projects and toy around with
it I still think the barrier to entry is quite high; especially when you compare it to the library-less Java
implementation like I want to do here (well I'm using SLF4J and Jackson to load the data). I like Rust for this workload
but no I don't want to be constrained by ownership/borrowing. I'm in "query engine learning mode" not "Rust learning mode".

[DuckDB](https://github.com/duckdb/duckdb) is another OLAP database (columnar) and it is well-loved by developers and offers lots of features. I bet I
could learn by building it from source and poking around. Although it's C++ so that's another learning curve for me.

[tablesaw](https://github.com/jtablesaw/tablesaw) is a Java-based data frame implementation.


## Instructions

Follow these instructions to build and run the example program:

1. Pre-requisite: Java 21
2. Build and run the program:
   * ```shell
     ./gradlew :app:run
     ```
3. Run the tests:
   * ```shell
     ./gradlew test
     ```


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [ ] Create a parallel query engine. Parallelization would be cool, and it's just a natural thing to do with data
  workloads like this.
* [x] DONE Re-package `query-engine`. I want a `data-system` module which is an API implemented by `query-engine`. `query-engine` is a "serial, indices-tracking
  query execution strategy" that implements the main database API. The overarching query API is this method signature:
  `QueryResult match(Query query, Table table)`. There can be multiple different query engine implementations, like a
  parallel one, one that tracks statistics and has heuristics, one for readability and trying new features. I think 
  creating more layers of abstraction within `query-engine` to accommodate different behavior will not scale well. It's
  ok to just re-implement some things for the sake of decoupling, interpretability, execution speed, and development
  speed. Thinking more widely, I even want to just use SQLite, DuckDB, Kuzu etc as a form of a "query engine" and be able to
  benchmark the same workload between my query engine (and in-memory column model).
   * DONE Create `data-system` (it is purposely not called "database" because it is far from a database. A database supports
     writes and is durable and has more features. This is more like a "query engine for ephemeral data").
   * DONE Move `QueryResult match(Query query, Table table)` out of `Executor` and into `data-system`.
   * DONE Rename `query-engine` to something like `data-system-serial-indices` ... and follow-on renames like classes.
   * DONE The data system should encapsulate the table. The API should become `QueryResult match(Query query)`. While it is
     convenient to allow the caller to inject their own physical data model (e.g. `InMemoryTable` in this case), that
     should be encapsulated by the data system (see the earlier language I used to describe a data system). And actually
     `data-system-serial-indices` should really be `data-system-serial-indices-arrays` which makes way for things like
     `data-system-serial-indices-arrow` and `data-system-serial-indices-foreign-memory`. I actually want to extract the
     common "indices-" query engine again... but anyway this is progress. It's important that the data system encapsulates
     the physical data because that makes it possible for a substantially different query engine to code to its
     substantially and necessarily different physical data model. For example, Neo4J (Cypher engine and vertices/edges
     data model).
   * DONE Rename `data-system-serial-indices` to `data-system-serial-indices-arrays`.
   * DONE Clean up package and module names.
   * DONE Update docs as appropriate.
   * DONE Plan future work.
* [ ] "Test Compatibility Kit" (TCK). Create a test fixture (harness? TCK?) that defines functional query tests but does
  not code to a specific implementation. In other words, make `QueryEngineTest` into `QueryTest` and use some indirection
  (explore the JUnit5 and Gradle options for this. Or, just do something plain old like a template method pattern) to
  allow the test to be implemented by different data systems (e.g. `data-system-serial-indices-arrays` or a future
  `data-system-serial-indices-arrow` or `data-system-serial-indices-foreign-memory`).
* [ ] (cosmetic) Consider renaming the project to something like "object-query-engine" or something more specific/descriptive.
* [ ] (stretch) Consider compressing integer arrays with [this integer compression library](https://github.com/lemire/JavaFastPFOR) which
      uses the [(incubating) Java vector API](https://openjdk.org/jeps/426). This would be kind of epic.
* [ ] (stretch) Consider creating performance benchmarks. Consider using [Java MicroBenchmark Harness](https://github.com/openjdk/jmh)..
  Be careful with the benchmarks. Don't draw overly broad conclusions.
* [ ] (stretch) Consider splitting apart a query verifier (UPDATE: the verifier is implemented) from a query planner from a query executor (and maybe even a query
  optimizer but I don't think I care to do that). I'm already finding that there is too much verification logic in the
  engine code which I'd rather be used just for execution.
* [ ] (cosmetic) Implement some human readable descriptive toStrings for the domain types like Table, Column, etc.
* [ ] (cosmetic) Criteria/criterion language. Consider it. singular/plural. I don't care much.
* [ ] (stretch) Create a test fixtures module or maybe just a module built for testing. This will encapsulate the `TestUtil` class.
* [x] DONE (I narrowed the issue and just worked around it) Upgrade to Java 21. WARNING: I got some serious JVM failures when trying this (I guess this is liable to happen
  with preview features but I thought preview was more a statement of "this could change" not "this could fail").
  Specifically I got the following.
  ```text
  Caused by: java.lang.VerifyError: Inconsistent stackmap frames at branch target 494
  Exception Details:
    Location:
      dgroomes/queryengine/QueryEngineTest.queryOnAssociationProperty()V @494: aload
    Reason:
      Type top (current frame, locals[8]) is not assignable to 'dgroomes/queryengine/Executor$QueryResult$Failure' (stack map, locals[8])
  ```
  I want to use the latest version of Java (currently 20), but the latest version of
  Gradle (8.1) only supports up to Java 19 when running Gradle itself but can support Java 20 for "forked work" like
  compilation, testing and running the program. See the [Gradle Compatibility Matrix](https://docs.gradle.org/current/userguide/compatibility.html#java).
* [ ] POJO (static) vs `Table` (dynamic) tension. By necessity, the query engine and its core API needs to be implemented
  with dynamism to accommodate tables of different widths and types but as application developers, we want to work with
  static types like a `State` POJO. This is what I envision the `geography-query` to demonstrate: it will bridge the
  dynamic query API to a static/specific domain. Now that I type all this out, I realize that modeling the static stuff
  and a "view/wrapper" class is a secondary concern for this project. I really want to focus on the query engine
  algorithm. Still, it's a valid nice-to-have. Or, seriously consider deleting the code and marking this wish list item
  as "won't do".
* [ ] Fully implement boolean support. (remember we want to support 1-bit (boolean), 32-bit (int) and variable length
  (string)).
* [ ] Consider using ZCTA (ZIP Code Tabulation Areas) instead of raw ZIP code data. ZCTA is a trademark of the US Census,
  and it represents an area whereas a ZIP code does not because it describes mail delivery routes. With area data, we can
  get into other area entities like counties. This is interesting for queries. Also, the data is well-described and
  official. See [the related page on the Census website](https://www.census.gov/programs-surveys/geography/guidance/geo-areas/zctas.html).
* [ ] Maybe create a data system using [Apache Calcite](https://calcite.apache.org/). Calcite has a model
  for describing relational queries, and it has many implementations (i.e. adapters) to implement those queries,
  including a simple "frontend" in-memory one. That's what I'll use.
   * First I'll start with a proof-of-technology in its own module and not integrate it with the functional tests.
   * Next, I'll actually load the geography data into (unfortuately yet another) POJO model and make a join query across
     the entities.
   * Next, I'll write a converter to convert the `Query` to a Calcite relational algrebra expression/object. I found this
     is tricky because the user docs are not super amazing, however there are good JavaDocs (always hard to discover).

## Finished Wish List Items

* [x] DONE Replace Arrow usage with straight array usage. (Remember, I copied this project over from a subproject in my `arrow-playground`
  repo). I think I'll create loading code (file I/O, JSON parsing) and keep it decoupled from the generic columnar
  code (it's own JPMS module) and then create a bridge module that describes the geograhies data using the APIs of the
  columnar/engine module.
  * DONE (it's just a package not a JPMS module) Create the loader module. (glue code)
  * DONE Create Gradle subprojects (and JPMS modularized). I'm kind of dragging my feet by doing this but I like this style.
  * DONE (extremely rough but working) Create the query engine module. (generic/API/high-value code)
  *   DONE Start associations
  *   DONE two-way associations
  *   DONE Query over associations (I have a test case but the code is going to take a lot of work)
  * DONE Load the geography data into the appropriate in-memory format.
* [x] DONE Model the data in Apache Arrow's table abstractions. Use `Table` even knowing it is experimental.
* [x] DONE Drop the 'single field' object graph type. I just want to model a "table" or maybe "collection" to use Mongo's term
* [x] DONE Use the word 'Table' instead of 'ObjectGraph' and lift out the many types from `ObjectGraph` into top-level
  classes because it is distracting being so tightly coupled/related (especially with the static creator methods).
  which is good because it disambiguates it from SQL.
* [x] DONE (answer: yes) Can we make the query execution signature return a table?
* [x] DONE Separate the query API from the query engine. Use different Gradle modules.
* [x] DONE Use less AssertJ (although I love it) and rely on pattern matching and plain Java a bit more in the tests.
* [x] DONE (This worked very nicely) Query verifier. Read the definitional "criteria + pointer" object and verify that it's legal for the table, and
  return an "attached" or "live" object that can be used for the query execution.
* [x] DONE "Criteria on intermediate nodes". I can't believe I missed this. We need to be able to match not just on the root
  and the leaves but on the intermediate entities in between. For example, the "North/South/North" query example can't
  be expressed correctly because the "South/North" part can't be expressed.
  * DONE Write a test case.
  * DONE Express "criteria chains" in the API
    * Update: I found that I think I want a `ChainMultiCriteria`. An effect of this is that it obsoletes the need for a
      list of criteria. Update: I think it's natural to express this as `And`-named thing (I guess?) And also (stream-of-consciousness)
      I don't need to support "disparate ANDs" and I might need to decouple `Pointer` a bit from `Criteria` (although
      that worked nicely until now). The type of query I want to support is all ANDed together (literally no ORs). And
      querying the root is not special case compared to querying from sub-entities (I need to make one big iterative for
      loop and get rid of the "root index matches" (it's going to be a moving root depending on context)).
    * DONE Update: this isn't working well. I think it's time to create a simple query verifier which reads the definitional
      "criteria + pointer" object (the query) and then turns that into a stateful/attached "query state" object that
      represents the graph and the query results (like intermediate pruning). This change can be done without changing
      the API. So it's best to do this work, then come back to the "Criteria on intermediate nodes" work otherwise it's
      too much at once.
  *  DONE Implement the North/South/North query in the main program
* [x] DONE (UPDATE 2023-05-10 work on this next) Model cyclic graphs in the data using the ["state adjacencies" of my cypher-playground](https://github.com/dgroomes/cypher-playground/blob/dc836b1ac934175394ece264c443bfae47465cd6/postgres-init/2-init-states-data.sql#L1)
  and do a query across associations
  * DONE Define the adjacencies data.
  * DONE Define the state data (code and name).
  * DONE Incorporate the state data into the Arrow data model.
  * DONE Load the adjacencies data into the in-memory format.
  * DONE Wire up the association columns correctly in `app/`
  * DONE Implement a query across state adjacencies data.
* [x] DONE Create a generic graph query API plus a (overtly simple) query execution engine. The graph API only
  supports schema-ful graphs (does this matter?). The query execution engine should prune the vector lists (i can't find
  words for this right now).
  * Ok I did the foundation of this work in other tasks, and the task-tracking is quite messy but I'm not going re-write
    history here. Let's move on. Now I need flesh out the query API.
  * DONE Support multiple criteria for strings.
  * DONE Support multiple criteria for ints. Note: if I take on this work now, I will implement it as another copy/paste
    change and the code will continue to suffer. If I consolidate the design and implementation first, while benefiting
    from a solid set of regression tests against an API that I'm also happy enough with (no need to change the API for now!)
    then the refactoring process will be safe/fun and then I come back and implement this task. I need to pay off this
    tech debt (it was a good debt).
* [x] DONE Genericize the Query API a bit. `PointedStringCriteriaQuery` is too restrictive. There should be a query type that
  allows multiple criteria of multiple types (e.g. string and int).
  * What happens to `OrdinalSingleFieldIntegerQuery`. Does this become a type that can be used as a component object in
    a composite query type (e.g. `MultiPointedCriteriaQuery`)?.
* [x] DONE (pretty good; the other wish list items capture similar improvement ideas nicely) Consolidate the duplicative code in `Executor`.
  * DONE Remove `PointerSingleFieldStringQuery` because it is obsolete with the more powerful `PointedStringCriteriaQuery`.
  * DONE Extract some common methods
  * DONE Be consistent about a 'result set' return type. Combine it with the final "prune" operation.
  * What else?
* [x] DONE Use a BitSet instead of an integer array. This is way more efficient. This is the type of thing that I'm glad I
  learned by doing this project.
* [x] DONE The 'query-engine' should maybe just be an execution strategy? I really need a separate API for the data model and
  the physical implementation I think. I'm treating `Table` and `Column` as physical but those should be interfaces.
  Maybe a module `data-model-api` and then `data-model-in-memory`? I don't care much about the feature set of the
  physical impl and API but I do care about thinning out query-engine to help me focus on the query execution strategy.
  Eventually I want to do parallelization and that's going to take a lot of complexity budget.
  * DONE Scaffold out the modules: `data-model-api` and `data-model-in-memory`.
  * DONE Create concrete implementations of `Table` and `Column` in `data-model-in-memory`. This needs to be called from `:app`.
  * DONE Somehow abstract the Verifier away from the `data-model-in-memory`.
    * DONE I think (vaguely, not really sure) I need a `TYPE` enum on `Column` to help the `Verifier` do its job.
    * DONE (ColumnFilterable interface) I'm going to try something else.
  * DONE Somehow abstract all implementation details out of `query-engine`. It should just code to the API.
* [ ] SKIP (I agree with my comment in this item: I'm not sure I'm going to sub-type Table.) Generic type parameters should work on the 'match' method. It takes a table and returns table of the exact same
  type. Not sure this is worth doing because I'm not sure I'm going to sub-type Table? I mean maybe.
