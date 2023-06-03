package dgroomes.geography;

public record Zip(int zipCode, int population) {
    public City city(GeographyGraph graph) {
        return graph.zipToCity().get(this);
    }
}
