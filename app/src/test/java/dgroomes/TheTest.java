package dgroomes;

import dgroomes.geography.GeographyGraph;
import dgroomes.geography_loader.GeographiesLoader;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class TheTest {

    @Test
    void placeholder() {
        assertThat(true).isTrue();
    }

    @Test
    void loadObjectGraph() {
        GeographyGraph graph = GeographiesLoader.loadFromFile(new File("../zips.jsonl"));

        assertThat(graph.zips()).hasSize(29_353);
        assertThat(graph.zipToCity()).hasSize(29_353);
        assertThat(graph.cities()).hasSize(25_701);
        assertThat(graph.cityToState()).hasSize(25_701);
        assertThat(graph.states()).hasSize(51); // 50 states + DC
    }
}
