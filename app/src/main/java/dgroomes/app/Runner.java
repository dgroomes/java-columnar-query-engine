package dgroomes.app;

import dgroomes.datasystem.Association;
import dgroomes.datasystem.Criteria;
import dgroomes.datasystem.Query;
import dgroomes.datasystem.QueryResult;
import dgroomes.geography.City;
import dgroomes.geography.GeographyGraph;
import dgroomes.geography.State;
import dgroomes.geography.Zip;
import dgroomes.inmemory.InMemoryColumn;
import dgroomes.inmemory.InMemoryTable;
import dgroomes.loader.GeographiesLoader;
import dgroomes.loader.StateData;
import dgroomes.queryengine.DataSystemSerialIndices;
import dgroomes.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Please see the README for more information.
 */
public class Runner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) {
        new Runner().execute();
    }

    public void execute() {
        var dataSystem = new DataSystemSerialIndices();

        // Read the ZIP code data from the local JSON file.
        GeographyGraph geo;
        {
            File zipsFile = new File("../zips.jsonl");
            if (!zipsFile.exists()) {
                String msg = "The 'zips.jsonl' file could not be found (%s). You need to run this program from the 'app/' directory.".formatted(zipsFile.getAbsolutePath());
                throw new RuntimeException(msg);
            }
            geo = GeographiesLoader.loadFromFile(zipsFile);
        }

        // Load the ZIP data into the in-memory table/column format.

        // The zips table is made of these columns:
        //   0: ZIP code (integer)
        //   1: population (integer)
        //   2: city (association)
        InMemoryTable zipsTable;
        InMemoryColumn.IntegerColumn zipCodeColumn;
        InMemoryColumn.IntegerColumn zipPopulationColumn;
        InMemoryColumn.AssociationColumn zipCityColumn;

        // The cities table is made of these columns:
        //   0: city name (string)
        //   1: state (association)
        //   2: ZIP codes (association)
        InMemoryTable citiesTable;
        InMemoryColumn.StringColumn cityNameColumn;
        InMemoryColumn.AssociationColumn cityStateColumn;

        // The states table is made of these columns:
        //   0: state code (string)
        //   1: state name (string)
        //   2: cities (association)
        //   3: state adjacencies (association)
        //   4: state adjacencies (reverse association) (this is a weird one)
        InMemoryTable statesTable;
        InMemoryColumn.StringColumn stateCodeColumn;
        InMemoryColumn.StringColumn stateNameColumn;

        {
            // Load the state data into the in-memory format
            //
            // This is phase one of two phases. In the first phase, we can only load columns that correspond to "direct data"
            // like the state name and state code. We can't load the association column ("contains cities") yet because the
            // cities table has not be created. This is a classic bootstrapping problem. The order that we bootstrap is
            // arbitrary. We just have to pick one. The map object is used to do the association work later.
            Map<State, Integer> stateToColumnIndex = new HashMap<>();
            {
                Set<State> states = geo.states();
                int statesSize = states.size();
                String[] stateCodes = new String[statesSize],
                        stateNames = new String[statesSize];

                int i = 0;
                for (State state : states) {
                    stateToColumnIndex.put(state, i);
                    stateCodes[i] = state.code();
                    stateNames[i] = state.name();
                    i++;
                }

                stateCodeColumn = new InMemoryColumn.StringColumn(stateCodes);
                stateNameColumn = new InMemoryColumn.StringColumn(stateNames);
                statesTable = InMemoryTable.ofColumns(stateCodeColumn, stateNameColumn);
                dataSystem.register("states", statesTable);
            }

            // Load the city data into the in-memory format.
            //
            // Similarly, we need a map that tracks cities to their column indices so that the cities table can be associated
            // with the ZIP table.
            //
            // Also, because the states data was already initialized, we can associate the cities rows to their state rows
            // and vice versa.
            Map<City, Integer> cityToColumnIndex = new HashMap<>();
            {
                Set<City> cities = geo.cities();
                int citiesSize = cities.size();
                String[] cityNames = new String[citiesSize];
                Association[] cityStateAssociations = new Association[citiesSize];
                int i = 0;
                for (City city : cities) {
                    cityToColumnIndex.put(city, i);
                    cityNames[i] = city.name();
                    State state = city.state(geo);
                    int stateColumnIndex = stateToColumnIndex.get(state);

                    // Associate the city to the state (easy because a city is contained in exactly one state)
                    cityStateAssociations[i] = new Association.One(stateColumnIndex);
                    i++;
                }

                cityNameColumn = new InMemoryColumn.StringColumn(cityNames);
                citiesTable = InMemoryTable.ofColumns(cityNameColumn);
                dataSystem.register("cities", citiesTable);
                cityStateColumn = citiesTable.associateTo(statesTable, cityStateAssociations);
            }


            // Load the ZIP data into the in-memory format.
            {
                var zipValuesSize = geo.zips().size();
                int[] codes = new int[zipValuesSize],
                        populations = new int[zipValuesSize];
                Association[] zipCityAssociations = new Association[zipValuesSize];

                int i = 0;
                for (Zip zip : geo.zips()) {
                    codes[i] = zip.zipCode();
                    populations[i] = zip.population();
                    City city = zip.city(geo);
                    int cityIndex = cityToColumnIndex.get(city);

                    // Associate the ZIP to the city (easy because a ZIP is contained in exactly one city)
                    zipCityAssociations[i] = new Association.One(cityIndex);
                    i++;
                }

                zipCodeColumn = new InMemoryColumn.IntegerColumn(codes);
                zipPopulationColumn = new InMemoryColumn.IntegerColumn(populations);
                zipsTable = InMemoryTable.ofColumns(zipCodeColumn, zipPopulationColumn);
                dataSystem.register("zips", zipsTable);
                zipCityColumn = zipsTable.associateTo(citiesTable, zipCityAssociations);
            }

            // Load the state adjacencies into the in-memory format.
            //
            // I need to add an AssociationColumn to the states table that references the states table. A bit circuitous, but
            // that's the point!
            {
                // The adjacencies data is represented as "state code (string) to state code (string)" pairs. I need to do a
                // look up of the State object from the state codes.
                Map<String, State> stateCodeToState = geo.states().stream().collect(Collectors.toMap(State::code, Function.identity()));
                Association[] associations = new Association[statesTable.size()];
                // Initialize the associations array with empty associations.
                Arrays.fill(associations, Association.NONE);

                for (StateData.StateAdjacency stateAdjacency : StateData.STATE_ADJACENCIES) {
                    State state = stateCodeToState.get(stateAdjacency.state());
                    int stateIndex = stateToColumnIndex.get(state);
                    State adjacentState = stateCodeToState.get(stateAdjacency.adjacentState());
                    int adjacentStateIndex = stateToColumnIndex.get(adjacentState);

                    Association existingAssociation = associations[stateIndex];
                    Association incrementedAssociation = switch (existingAssociation) {
                        case null -> new Association.One(adjacentStateIndex);
                        case Association a -> a.add(adjacentStateIndex);
                    };

                    associations[stateIndex] = incrementedAssociation;
                }

                statesTable.associateTo(statesTable, associations);
            }
        }

        {
            // Do a simple scan and determine the ZIP with the highest population.
            int maxPopulation = -1;
            int maxPopulationIndex = -1;

            for (int i = 0; i < zipsTable.size(); i++) {
                var pop = zipPopulationColumn.ints()[i];
                if (pop > maxPopulation) {
                    maxPopulation = pop;
                    maxPopulationIndex = i;
                }
            }

            if (maxPopulationIndex == -1) {
                throw new IllegalStateException("The max population index was never set.");
            }

            int code = zipCodeColumn.ints()[maxPopulationIndex];
            int cityIndex = ((Association.One) zipCityColumn.associations[maxPopulationIndex]).idx();
            String city = cityNameColumn.strings()[cityIndex];
            int stateIndex = ((Association.One) cityStateColumn.associations[cityIndex]).idx();
            String stateCode = stateCodeColumn.strings()[stateIndex];
            log.info("The ZIP code with the highest population is '{}' in {}, {} with a population of {}.", code, city, stateCode, Util.formatInteger(maxPopulation));
        }

        {
            // Query the data using the full 'data system' abstraction.
            //
            // Specifically, find all ZIP codes that have a population around 10,000 and are adjacent to a state with at
            // least one city named "Plymouth".

            var query = new Query("zips");
            query.rootNode.addCriteria(new Criteria.IntCriteria(1, i -> i >= 10_000 && i < 10_100));
            query.rootNode.createChild(2) // Column 2 is the association column to cities.
                    .createChild(1) // Column 1 is the association column to states.
                    .createChild(3) // Column 3 is the association column to other states.
                    .createChild(2) // Column 2 is the association column to cities.
                    .addCriteria(new Criteria.StringCriteria(0, "PLYMOUTH"::equals)); // Column 0 is the string column of city names.

            QueryResult queryResult = dataSystem.execute(query);

            switch (queryResult) {
                case QueryResult.Success(var resultSet) -> {
                    int matches = resultSet.size();
                    var matchingZipCodes = (InMemoryColumn.IntegerColumn) resultSet.columns().get(0);
                    var count = Util.formatInteger(matches);
                    var zipsStr = Arrays.toString(matchingZipCodes.ints());
                    log.info("{} ZIP codes have a population around 10,000 and are adjacent to a state that has a city named 'Plymouth': {}", count, zipsStr);
                }
                case QueryResult.Failure(var msg) -> log.error(msg);
            }
        }

        {
            // Find all states named with "North" that are adjacent to a state with "South" that are adjacent to a state with "North".
            var query = new Query("states");
            query.rootNode.addCriteria(new Criteria.StringCriteria(1, s -> s.contains("North"))) // Column 1 is the string column of state names.
                    .createChild(3) // Column 3 is the association column to other states.
                    .addCriteria(new Criteria.StringCriteria(1, s -> s.contains("South")))
                    .createChild(3)
                    .addCriteria(new Criteria.StringCriteria(1, s -> s.contains("North")));

            QueryResult queryResult = dataSystem.execute(query);

            switch (queryResult) {
                case QueryResult.Success(var resultSet) -> {
                    int matches = resultSet.size();
                    var matchingStateNamesColumn = (InMemoryColumn.StringColumn) resultSet.columns().get(1);
                    var count = Util.formatInteger(matches);
                    var names = Arrays.toString(matchingStateNamesColumn.strings());
                    log.info("{} states have 'North' in their name and are adjacent to states with 'South' in their name which are adjacent to states with 'North' in their name (yes this is totally redundant!): {}", count, names);
                }
                case QueryResult.Failure(var msg) -> log.error(msg);
            }
        }
    }
}
