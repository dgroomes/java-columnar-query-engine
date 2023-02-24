package dgroomes.geography;

/**
 * Note: the state code is only necessary to disambiguate cities with the same name. ZIP codes and states don't have this
 * same problem because ZIP codes are unique and state names/code are unique.
 */
public record City(String name, String stateCode) {
  public State state(GeographyGraph geographyGraph) {
    return geographyGraph.cityToState().get(this);
  }
}
