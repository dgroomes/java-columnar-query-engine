package dgroomes.geography_loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dgroomes.geography.City;
import dgroomes.geography.GeographyGraph;
import dgroomes.geography.State;
import dgroomes.geography.Zip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static dgroomes.util.Util.formatInteger;
import static java.util.stream.Collectors.toMap;

public class GeographiesLoader {

  private static final Logger log = LoggerFactory.getLogger(GeographiesLoader.class);

  /**
   * Build an instance of {@link GeographyGraph} by loading the raw data from the ZIP code JSON file and inferring
   * object data and object-to-object relationships.
   */
  public static GeographyGraph loadFromFile(File zipsFile) {
    log.info("Reading ZIP code data from the local file ...");

    JsonMapper jsonMapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

    if (!zipsFile.exists()) {
      String msg = "The ZIP code JSON data file could not be found at '%s'".formatted(zipsFile.getAbsolutePath());
      throw new RuntimeException(msg);
    }

    Set<Zip> zips = new HashSet<>();
    Map<Zip, City> zipToCity = new HashMap<>();
    Set<City> cities = new HashSet<>();
    Map<City, State> cityToState = new HashMap<>();
    Set<State> states = new HashSet<>(StateData.STATES);
    Map<String, State> statesByCode = states.stream().collect(toMap(State::code, state -> state));

    try (Stream<String> zipsJsonLines = Files.lines(zipsFile.toPath())) {
      zipsJsonLines.forEach(zipJson -> {
        JsonNode zipNode;
        try {
          zipNode = jsonMapper.readTree(zipJson);
        } catch (JsonProcessingException e) {
          throw new IllegalStateException("Failed to deserialize the JSON representing a ZIP code", e);
        }

        Zip zip;
        {
          int zipCode = Integer.parseInt(zipNode.get("_id").asText());
          int pop = zipNode.get("pop").asInt();
          zip = new Zip(zipCode, pop);
        }

        City city;
        {
          String cityName = zipNode.get("city").asText();
          String stateCode = zipNode.get("state").asText();
          city = new City(cityName, stateCode);
        }

        State state;
        {
          String stateCode = zipNode.get("state").asText();
          state = statesByCode.get(stateCode);
        }

        zips.add(zip);
        zipToCity.put(zip, city);
        if (cities.add(city)) {
          cityToState.put(city, state);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("There was an error while reading the ZIP data from the file.", e);
    }

    log.info("Read {} ZIP codes from the local file and deserialized into Java objects.", formatInteger(zips.size()));

    return new GeographyGraph(zips,
            zipToCity,
            cities,
            cityToState,
            states);
  }
}
