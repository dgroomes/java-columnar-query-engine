package dgroomes.app;

import dgroomes.geography.City;
import dgroomes.geography.GeographyGraph;
import dgroomes.geography.State;
import dgroomes.geography.Zip;
import dgroomes.loader.GeographiesLoader;
import dgroomes.loader.StateData;
import dgroomes.queryapi.Criteria;
import dgroomes.queryapi.Pointer;
import dgroomes.queryengine.Association;
import dgroomes.queryengine.Column;
import dgroomes.queryengine.Executor;
import dgroomes.queryengine.Table;
import dgroomes.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
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
        Table zipsTable;
        Column.IntegerColumn zipCodeColumn;
        Column.IntegerColumn zipPopulationColumn;
        Column.AssociationColumn zipCityColumn;

        // The cities table is made of these columns:
        //   0: city name (string)
        //   1: state (association)
        //   2: ZIP codes (association)
        Table citiesTable;
        Column.StringColumn cityNameColumn;
        Column.AssociationColumn cityStateColumn;

        // The states table is made of these columns:
        //   0: state code (string)
        //   1: state name (string)
        //   2: cities (association)
        //   3: state adjacencies (association)
        //   4: state adjacencies (reverse association) (this is a weird one)
        Table statesTable;
        Column.StringColumn stateCodeColumn;
        Column.StringColumn stateNameColumn;

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

                stateCodeColumn = new Column.StringColumn(stateCodes);
                stateNameColumn = new Column.StringColumn(stateNames);
                statesTable = Table.ofColumns(stateCodeColumn, stateNameColumn);
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

                cityNameColumn = new Column.StringColumn(cityNames);
                citiesTable = Table.ofColumns(cityNameColumn);
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

                zipCodeColumn = new Column.IntegerColumn(codes);
                zipPopulationColumn = new Column.IntegerColumn(populations);
                zipsTable = Table.ofColumns(zipCodeColumn, zipPopulationColumn);
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
            // Query the data using the 'query engine'.
            //
            // Specifically, find all ZIP codes that have a population around 10,000 are adjacent to a state with at
            // least one city named "Plymouth".

            var populationCriteria = new Criteria.PointedIntCriteria(new Pointer.Ordinal(1), i -> i >= 10_000 && i < 10_100);

            // This pointer is hard to read, so let's break it down in words.
            //   1. The first pointer is on the ZIP table. It represents column '2' which is the association column to cities.
            //   2. The second pointer is on the cities table. It represents column '1' which is the association column to states.
            //   3. The third pointer is on the states table. It represents column '3' which is the association column to other states.
            //   4. The fourth pointer is on the states table. It represents column '2' which is the association column to cities.
            //   5. The fifth pointer is on the cities table. It represents column '0' which is the string column of city names.
            var zipToCityToStateToAdjacentStateToCityToNamePointer = new Pointer.NestedPointer(2,
                    new Pointer.NestedPointer(1,
                            new Pointer.NestedPointer(3,
                                    new Pointer.NestedPointer(2,
                                            new Pointer.Ordinal(0)))));
            var plymouthCriteria = new Criteria.PointedStringCriteria(zipToCityToStateToAdjacentStateToCityToNamePointer, "PLYMOUTH"::equals);

            Executor.QueryResult queryResult = Executor.match(List.of(populationCriteria, plymouthCriteria), zipsTable);

            switch (queryResult) {
                case Executor.QueryResult.Success(var resultSet) -> {
                    int matches = resultSet.size();
                    var matchingZipCodes = (Column.IntegerColumn) resultSet.columns().get(0);
                    var count = Util.formatInteger(matches);
                    var zipsStr = Arrays.toString(matchingZipCodes.ints());
                    log.info("{} ZIP codes have a population around 10,000 and are adjacent to a state that has a city named 'Plymouth': {}", count, zipsStr);
                }
                case Executor.QueryResult.Failure(var msg) -> log.error(msg);
            }
        }
    }
}
