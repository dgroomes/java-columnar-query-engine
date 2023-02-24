package dgroomes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.complex.reader.FieldReader;
import org.apache.arrow.vector.table.Row;
import org.apache.arrow.vector.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Please see the README for more information.
 */
public class Runner {

  private static final Logger log = LoggerFactory.getLogger(Runner.class);

  record Zip(String zipCode, String cityName, String stateCode, int population) {}

  List<Zip> zips;

  public static void main(String[] args) {
    new Runner().execute();
  }

  public void execute() {
    log.info("Reading ZIP code data from the local file ...");

    // Read the ZIP code data from the local JSON file.
    {
      JsonMapper jsonMapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

      File zipsFile = new File("zips.jsonl");
      if (!zipsFile.exists()) {
        String msg = "The 'zips.jsonl' file could not be found (%s). You need to run this program from the root of the 'sort-and-search' project.".formatted(zipsFile.getAbsolutePath());
        throw new RuntimeException(msg);
      }

      try (Stream<String> zipsJsonLines = Files.lines(zipsFile.toPath())) {
        zips = zipsJsonLines.map(zipJson -> {
          JsonNode zipNode;
          try {
            zipNode = jsonMapper.readTree(zipJson);
          } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize the JSON representing a ZIP code", e);
          }

          return new Zip(zipNode.get("_id").asText(), zipNode.get("city").asText(), zipNode.get("state").asText(), zipNode.get("pop").asInt());
        }).toList();
      } catch (IOException e) {
        throw new RuntimeException("There was an error while reading the ZIP data from the file.", e);
      }

      log.info("Read {} ZIP codes from the local file and into Java objects.", formatInteger(zips.size()));
    }

    // Load the in-memory ZIP and city data from Java objects into Apache Arrow's in-memory data structures.
    try (BufferAllocator allocator = new RootAllocator();
         IntVector zipCodeVector = new IntVector("zip-codes", allocator);
         VarCharVector cityNameVector = new VarCharVector("city-names", allocator);
         VarCharVector stateCodeVector = new VarCharVector("state-codes", allocator);
         IntVector populationVector = new IntVector("populations", allocator);
         VarCharVector stateStateCodeVector = new VarCharVector("state-state-codes", allocator);
         VarCharVector stateStateNameVector = new VarCharVector("state-state-names", allocator)) {

      // Load the state data into vectors
      int statesSize = StateData.STATES.size();
      for (int i = 0; i < statesSize; i++) {
        StateData.State state = StateData.STATES.get(i);
        stateStateCodeVector.setSafe(i, state.code().getBytes());
        stateStateNameVector.setSafe(i, state.name().getBytes());
      }

      stateStateCodeVector.setValueCount(statesSize);
      stateStateNameVector.setValueCount(statesSize);

      int zipValuesSize = zips.size();

      zipCodeVector.allocateNew(zipValuesSize);
      // Notice that we don't set the size because we don't know how many bytes the city names and state codes are going
      // to take. This is the nature of using a variable-sized data type, like "var char".
      cityNameVector.allocateNew();
      stateCodeVector.allocateNew();
      populationVector.allocateNew(zipValuesSize);

      for (int i = 0; i < zipValuesSize; i++) {
        Zip zip = zips.get(i);
        zipCodeVector.set(i, Integer.parseInt(zip.zipCode));
        // The "safe" version of the set method automatically grows the vector if it's not big enough to hold the new value.
        cityNameVector.setSafe(i, zip.cityName.getBytes());
        stateCodeVector.setSafe(i, zip.stateCode.getBytes());
        populationVector.set(i, zip.population);
      }

      // TODO figure out how to use the state data as a "dictionary encoding" or whatever in the ZIP code data.

      // Necessary boilerplate to tell Apache Arrow that we're done adding values to the vectors, and to restate the
      // number of values in each vector.
      zipCodeVector.setValueCount(zipValuesSize);
      cityNameVector.setValueCount(zipValuesSize);
      stateCodeVector.setValueCount(zipValuesSize);
      populationVector.setValueCount(zipValuesSize);

      log.info("Loaded {} ZIP codes into Apache Arrow vectors (arrays)", formatInteger(zipValuesSize));

      // TODO load the state adjacencies into Apache Arrow vectors
      // How should "many-to-may mapping data" be represented in Arrow data structures? I mean, I want a hash/dictionary
      // but all Arrow has is vectors. It has a dictionary type but it's like a thin wrapper over vectors (I think).
      // I think I want a vector of integer arrays to represent state to state adjacencies... not 100%.

      // Turn it into an Arrow table
      try (Table zipTable = new Table(List.of(zipCodeVector, cityNameVector, stateCodeVector, populationVector), zipValuesSize)) {

        // I want to scan the population vector and find the max value and then find the ZIP code, city, and state of
        // that ZIP code. I think this is a basic thing to do in Arrow. The FieldReader interface has a huge API
        // surface area.
        //
        // Let's iterate over a few rows.
        int maxPopulation = -1;
        int maxPopulationIndex = -1;

        FieldReader populationReader = zipTable.getReader("populations");

        for (int i = 0; i < zipValuesSize; i++) {
          populationReader.setPosition(i);
          int pop = populationReader.readInteger();
          if (pop > maxPopulation) {
            maxPopulation = pop;
            maxPopulationIndex = populationReader.getPosition();
          }
        }

        if (maxPopulationIndex == -1) {
          throw new IllegalStateException("The max population index was never set.");
        }

        Row maxPopulationZip = zipTable.immutableRow().setPosition(maxPopulationIndex);

        log.info("The ZIP code with the highest population is {} in {}, {} with a population of {}.", maxPopulationZip.getInt("zip-codes"), maxPopulationZip.getVarCharObj("city-names"), maxPopulationZip.getVarCharObj("state-codes"), formatInteger(maxPopulation));
      }

      // TODO the rest (and hard part) of the program...
    }
  }

  /**
   * Formats an integer value with commas.
   * <p>
   * For example, 1234567 becomes "1,234,567".
   */
  private static String formatInteger(int value) {
    return NumberFormat.getNumberInstance(Locale.US).format(value);
  }
}
