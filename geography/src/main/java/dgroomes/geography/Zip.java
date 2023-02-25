package dgroomes.geography;

public record Zip(String zipCode, int population) {
  public City city(GeographyGraph graph) {
    return graph.zipToCity().get(this);
  }
}
