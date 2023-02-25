package dgroomes.geography;

import java.util.Map;
import java.util.Set;

/**
 * An immutable "object graph" of the geography data.
 * <p>
 * I'm modelling the object-to-object relationships using maps instead of having direct references because it's a
 * chicken-and-egg bootstrapping problem: I can't fully construct a ZIP object (as a Java "record") unless I have a
 * fully constructed City object (also as a Java record) but I can't construct the City object without a fully
 * constructed ZIP object. There is a bi-directional relationship from ZIP to city ("contained in" and "contains").
 * Pretty annoying! Also even if I didn't use records you need to be careful with circular references because you can
 * get a stack overflow in a poorly implemented equals(), hashCode(), or toString() method.
 *
 * @param zips
 * @param zipToCity
 * @param cities
 * @param cityToState
 * @param states
 */
public record GeographyGraph(Set<Zip> zips,
                             Map<Zip, City> zipToCity,
                             Set<City> cities,
                             Map<City, State> cityToState,
                             Set<State> states) {}
