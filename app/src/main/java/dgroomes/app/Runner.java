package dgroomes.app;

import dgroomes.geography.City;
import dgroomes.geography.GeographyGraph;
import dgroomes.geography.State;
import dgroomes.geography.Zip;
import dgroomes.loader.GeographiesLoader;
import dgroomes.queryengine.ObjectGraph;
import dgroomes.queryengine.ObjectGraph.Association;
import dgroomes.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    ObjectGraph.MultiColumnEntity zipsTable;
    ObjectGraph.Column.IntegerColumn zipCodeColumn;
    ObjectGraph.Column.IntegerColumn zipPopulationColumn;
    ObjectGraph.Column.AssociationColumn zipCityColumn;

    ObjectGraph.MultiColumnEntity citiesTable;
    ObjectGraph.Column.StringColumn cityNameColumn;
    ObjectGraph.Column.AssociationColumn cityZipColumn;
    ObjectGraph.Column.AssociationColumn cityStateColumn;

    ObjectGraph.MultiColumnEntity statesTable;
    ObjectGraph.Column.StringColumn stateCodeColumn;
    ObjectGraph.Column.StringColumn stateNameColumn;
    ObjectGraph.Column.AssociationColumn stateCityColumn;

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

        stateCodeColumn = new ObjectGraph.Column.StringColumn(stateCodes);
        stateNameColumn = new ObjectGraph.Column.StringColumn(stateNames);
        statesTable = ObjectGraph.ofColumns(stateCodeColumn, stateNameColumn);
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
        Association[] stateCitiesAssociations = new Association[citiesSize];
        int i = 0;
        for (City city : cities) {
          cityToColumnIndex.put(city, i);
          cityNames[i] = city.name();
          State state = city.state(geo);
          int stateColumnIndex = stateToColumnIndex.get(state);

          // Associate the city to the state (easy because a city is contained in exactly one state)
          cityStateAssociations[i] = new Association.One(stateColumnIndex);

          // Associate the state to the city (a bit more work because a state contains many cities)
          {
            Association existingAssociation = stateCitiesAssociations[stateColumnIndex];
            Association incrementedAssociation = switch (existingAssociation) {
              case null -> new Association.One(i);
              case Association a -> a.add(i);
            };
            stateCitiesAssociations[stateColumnIndex] = incrementedAssociation;
          }
          i++;
        }

        cityNameColumn = new ObjectGraph.Column.StringColumn(cityNames);
        cityStateColumn = new ObjectGraph.Column.AssociationColumn(statesTable, cityStateAssociations);
        citiesTable = ObjectGraph.ofColumns(cityNameColumn, cityStateColumn);
      }


      // Load the ZIP data into the in-memory format.
      {
        var zipValuesSize = geo.zips().size();
        int[] codes = new int[zipValuesSize],
                populations = new int[zipValuesSize];
        Association[] zipCityAssociations = new Association[zipValuesSize];
        Association[] cityZipAssociations = new Association[zipValuesSize];

        int i = 0;
        for (Zip zip : geo.zips()) {
          codes[i] = zip.zipCode();
          populations[i] = zip.population();
          City city = zip.city(geo);
          int cityIndex = cityToColumnIndex.get(city);

          // Associate the ZIP to the city (easy because a ZIP is contained in exactly one city)
          zipCityAssociations[i] = new Association.One(cityIndex);

          // Associate the city to the ZIP (a bit more work because a city can contain many ZIPs
          {
            Association existingAssociation = cityZipAssociations[cityIndex];
            Association incrementedAssociation = switch (existingAssociation) {
              case null -> new Association.One(i);
              case Association a -> a.add(i);
            };
            cityZipAssociations[cityIndex] = incrementedAssociation;
          }
          i++;
        }

        zipCodeColumn = new ObjectGraph.Column.IntegerColumn(codes);
        zipPopulationColumn = new ObjectGraph.Column.IntegerColumn(populations);
        zipCityColumn = new ObjectGraph.Column.AssociationColumn(citiesTable, zipCityAssociations);
        zipsTable = ObjectGraph.ofColumns(zipCodeColumn, zipPopulationColumn, zipCityColumn);
        citiesTable.columns().add(new ObjectGraph.Column.AssociationColumn(zipsTable, cityZipAssociations));
      }

      // TODO load the state adjacencies into Apache Arrow vectors
      // How should "many-to-may mapping data" be represented in Arrow data structures? I mean, I want a hash/dictionary
      // but all Arrow has is vectors. It has a dictionary type but it's like a thin wrapper over vectors (I think).
      // I think I want a vector of integer arrays to represent state to state adjacencies... not 100%.

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
        log.info("The ZIP code with the highest population is {} in {}, {} with a population of {}.", code, city, stateCode, Util.formatInteger(maxPopulation));
      }

      // TODO the rest (and hard part) of the program...
    }
  }
}
