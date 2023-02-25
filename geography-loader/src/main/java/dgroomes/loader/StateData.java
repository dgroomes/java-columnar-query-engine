package dgroomes.loader;

import dgroomes.geography.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hardcoded data about US states.
 */
public class StateData {

  public static List<State> STATES;

  record StateAdjacency(String state, String adjacentState) {}

  public static List<StateAdjacency> STATE_ADJACENCIES;

  static {
    STATES = List.of(new State("AL", "Alabama"),
            new State("AL", "Alabama"),
            new State("AK", "Alaska"),
            new State("AZ", "Arizona"),
            new State("AR", "Arkansas"),
            new State("CA", "California"),
            new State("CO", "Colorado"),
            new State("CT", "Connecticut"),
            new State("DE", "Delaware"),
            new State("DC", "Distrt of Columbia"),
            new State("FL", "Florida"),
            new State("GA", "Georgia"),
            new State("HI", "Hawaii"),
            new State("ID", "Idaho"),
            new State("IL", "Illinois"),
            new State("IN", "Indiana"),
            new State("IA", "Iowa"),
            new State("KS", "Kansas"),
            new State("KY", "Kentucky"),
            new State("LA", "Louisiana"),
            new State("ME", "Maine"),
            new State("MD", "Maryland"),
            new State("MA", "Massachusetts"),
            new State("MI", "Michigan"),
            new State("MN", "Minnesota"),
            new State("MS", "Mississippi"),
            new State("MO", "Missouri"),
            new State("MT", "Montana"),
            new State("NE", "Nebraska"),
            new State("NV", "Nevada"),
            new State("NH", "New Hampshire"),
            new State("NJ", "New Jersey"),
            new State("NM", "New Mexico"),
            new State("NY", "New York"),
            new State("NC", "North Carolina"),
            new State("ND", "North Dakota"),
            new State("OH", "Ohio"),
            new State("OK", "Oklahoma"),
            new State("OR", "Oregon"),
            new State("PA", "Pennsylvania"),
            new State("RI", "Rhode Island"),
            new State("SC", "South Carolina"),
            new State("SD", "South Dakota"),
            new State("TN", "Tennessee"),
            new State("TX", "Texas"),
            new State("UT", "Utah"),
            new State("VT", "Vermont"),
            new State("VA", "Virginia"),
            new State("WA", "Washington"),
            new State("WV", "West Virginia"),
            new State("WI", "Wisconsin"),
            new State("WY", "Wyoming"));
  }

  static {
    // This could not be much more verbose could it?
    List<StateAdjacency> data = new ArrayList<>();
    data.add(new StateAdjacency("AL", "FL"));
    data.add(new StateAdjacency("AL", "GA"));
    data.add(new StateAdjacency("AL", "MS"));
    data.add(new StateAdjacency("AL", "TN"));
    data.add(new StateAdjacency("AZ", "CA"));
    data.add(new StateAdjacency("AZ", "NV"));
    data.add(new StateAdjacency("AZ", "NM"));
    data.add(new StateAdjacency("AZ", "UT"));
    data.add(new StateAdjacency("AZ", "CO"));
    data.add(new StateAdjacency("AR", "LA"));
    data.add(new StateAdjacency("AR", "MO"));
    data.add(new StateAdjacency("AR", "MS"));
    data.add(new StateAdjacency("AR", "OK"));
    data.add(new StateAdjacency("AR", "TN"));
    data.add(new StateAdjacency("AR", "TX"));
    data.add(new StateAdjacency("CA", "OR"));
    data.add(new StateAdjacency("CA", "NV"));
    data.add(new StateAdjacency("CA", "AZ"));
    data.add(new StateAdjacency("CO", "KS"));
    data.add(new StateAdjacency("CO", "OK"));
    data.add(new StateAdjacency("CO", "NM"));
    data.add(new StateAdjacency("CO", "AZ"));
    data.add(new StateAdjacency("CO", "UT"));
    data.add(new StateAdjacency("CO", "WY"));
    data.add(new StateAdjacency("CO", "NE"));
    data.add(new StateAdjacency("CT", "MA"));
    data.add(new StateAdjacency("CT", "NY"));
    data.add(new StateAdjacency("CT", "RI"));
    data.add(new StateAdjacency("DE", "MD"));
    data.add(new StateAdjacency("DE", "NJ"));
    data.add(new StateAdjacency("DE", "PA"));
    data.add(new StateAdjacency("DC", "MD"));
    data.add(new StateAdjacency("DC", "VA"));
    data.add(new StateAdjacency("FL", "AL"));
    data.add(new StateAdjacency("FL", "GA"));
    data.add(new StateAdjacency("GA", "AL"));
    data.add(new StateAdjacency("GA", "FL"));
    data.add(new StateAdjacency("GA", "NC"));
    data.add(new StateAdjacency("GA", "SC"));
    data.add(new StateAdjacency("GA", "TN"));
    data.add(new StateAdjacency("ID", "MT"));
    data.add(new StateAdjacency("ID", "WY"));
    data.add(new StateAdjacency("ID", "UT"));
    data.add(new StateAdjacency("ID", "NV"));
    data.add(new StateAdjacency("ID", "OR"));
    data.add(new StateAdjacency("ID", "WA"));
    data.add(new StateAdjacency("IL", "WI"));
    data.add(new StateAdjacency("IL", "IA"));
    data.add(new StateAdjacency("IL", "MO"));
    data.add(new StateAdjacency("IL", "KY"));
    data.add(new StateAdjacency("IL", "IN"));
    data.add(new StateAdjacency("IN", "MI"));
    data.add(new StateAdjacency("IN", "OH"));
    data.add(new StateAdjacency("IN", "KY"));
    data.add(new StateAdjacency("IN", "IL"));
    data.add(new StateAdjacency("IA", "MN"));
    data.add(new StateAdjacency("IA", "WI"));
    data.add(new StateAdjacency("IA", "IL"));
    data.add(new StateAdjacency("IA", "MO"));
    data.add(new StateAdjacency("IA", "NE"));
    data.add(new StateAdjacency("IA", "SD"));
    data.add(new StateAdjacency("KS", "NE"));
    data.add(new StateAdjacency("KS", "CO"));
    data.add(new StateAdjacency("KS", "OK"));
    data.add(new StateAdjacency("KS", "MO"));
    data.add(new StateAdjacency("KS", "AR"));
    data.add(new StateAdjacency("KY", "IN"));
    data.add(new StateAdjacency("KY", "OH"));
    data.add(new StateAdjacency("KY", "WV"));
    data.add(new StateAdjacency("KY", "VA"));
    data.add(new StateAdjacency("KY", "TN"));
    data.add(new StateAdjacency("KY", "MO"));
    data.add(new StateAdjacency("KY", "IL"));
    data.add(new StateAdjacency("LA", "AR"));
    data.add(new StateAdjacency("LA", "TX"));
    data.add(new StateAdjacency("LA", "MS"));
    data.add(new StateAdjacency("ME", "NH"));
    data.add(new StateAdjacency("MD", "DE"));
    data.add(new StateAdjacency("MD", "PA"));
    data.add(new StateAdjacency("MD", "WV"));
    data.add(new StateAdjacency("MD", "VA"));
    data.add(new StateAdjacency("MD", "DC"));
    data.add(new StateAdjacency("MA", "NH"));
    data.add(new StateAdjacency("MA", "VT"));
    data.add(new StateAdjacency("MA", "CT"));
    data.add(new StateAdjacency("MA", "RI"));
    data.add(new StateAdjacency("MA", "NY"));
    data.add(new StateAdjacency("MI", "WI"));
    data.add(new StateAdjacency("MI", "IN"));
    data.add(new StateAdjacency("MI", "OH"));
    data.add(new StateAdjacency("MN", "WI"));
    data.add(new StateAdjacency("MN", "IA"));
    data.add(new StateAdjacency("MN", "SD"));
    data.add(new StateAdjacency("MN", "ND"));
    data.add(new StateAdjacency("MS", "TN"));
    data.add(new StateAdjacency("MS", "AR"));
    data.add(new StateAdjacency("MS", "LA"));
    data.add(new StateAdjacency("MS", "AL"));
    data.add(new StateAdjacency("MO", "IA"));
    data.add(new StateAdjacency("MO", "IL"));
    data.add(new StateAdjacency("MO", "KY"));
    data.add(new StateAdjacency("MO", "TN"));
    data.add(new StateAdjacency("MO", "AR"));
    data.add(new StateAdjacency("MO", "OK"));
    data.add(new StateAdjacency("MO", "KS"));
    data.add(new StateAdjacency("MO", "NE"));
    data.add(new StateAdjacency("MT", "ID"));
    data.add(new StateAdjacency("MT", "WY"));
    data.add(new StateAdjacency("MT", "SD"));
    data.add(new StateAdjacency("MT", "ND"));
    data.add(new StateAdjacency("NE", "SD"));
    data.add(new StateAdjacency("NE", "IA"));
    data.add(new StateAdjacency("NE", "MO"));
    data.add(new StateAdjacency("NE", "KS"));
    data.add(new StateAdjacency("NE", "CO"));
    data.add(new StateAdjacency("NE", "WY"));
    data.add(new StateAdjacency("NV", "OR"));
    data.add(new StateAdjacency("NV", "ID"));
    data.add(new StateAdjacency("NV", "UT"));
    data.add(new StateAdjacency("NV", "AZ"));
    data.add(new StateAdjacency("NV", "CA"));
    data.add(new StateAdjacency("NH", "ME"));
    data.add(new StateAdjacency("NH", "VT"));
    data.add(new StateAdjacency("NH", "MA"));
    data.add(new StateAdjacency("NJ", "NY"));
    data.add(new StateAdjacency("NJ", "DE"));
    data.add(new StateAdjacency("NJ", "PA"));
    data.add(new StateAdjacency("NM", "AZ"));
    data.add(new StateAdjacency("NM", "UT"));
    data.add(new StateAdjacency("NM", "CO"));
    data.add(new StateAdjacency("NM", "OK"));
    data.add(new StateAdjacency("NM", "TX"));
    data.add(new StateAdjacency("NY", "VT"));
    data.add(new StateAdjacency("NY", "MA"));
    data.add(new StateAdjacency("NY", "CT"));
    data.add(new StateAdjacency("NY", "NJ"));
    data.add(new StateAdjacency("NY", "PA"));
    data.add(new StateAdjacency("NC", "SC"));
    data.add(new StateAdjacency("NC", "GA"));
    data.add(new StateAdjacency("NC", "TN"));
    data.add(new StateAdjacency("NC", "VA"));
    data.add(new StateAdjacency("ND", "MN"));
    data.add(new StateAdjacency("ND", "SD"));
    data.add(new StateAdjacency("ND", "MT"));
    data.add(new StateAdjacency("OH", "PA"));
    data.add(new StateAdjacency("OH", "WV"));
    data.add(new StateAdjacency("OH", "KY"));
    data.add(new StateAdjacency("OH", "IN"));
    data.add(new StateAdjacency("OH", "MI"));
    data.add(new StateAdjacency("OK", "KS"));
    data.add(new StateAdjacency("OK", "MO"));
    data.add(new StateAdjacency("OK", "AR"));
    data.add(new StateAdjacency("OK", "TX"));
    data.add(new StateAdjacency("OK", "NM"));
    data.add(new StateAdjacency("OK", "CO"));
    data.add(new StateAdjacency("OR", "WA"));
    data.add(new StateAdjacency("OR", "ID"));
    data.add(new StateAdjacency("OR", "NV"));
    data.add(new StateAdjacency("OR", "CA"));
    data.add(new StateAdjacency("PA", "NY"));
    data.add(new StateAdjacency("PA", "NJ"));
    data.add(new StateAdjacency("PA", "DE"));
    data.add(new StateAdjacency("PA", "MD"));
    data.add(new StateAdjacency("PA", "WV"));
    data.add(new StateAdjacency("PA", "OH"));
    data.add(new StateAdjacency("RI", "CT"));
    data.add(new StateAdjacency("RI", "MA"));
    data.add(new StateAdjacency("SC", "GA"));
    data.add(new StateAdjacency("SC", "NC"));
    data.add(new StateAdjacency("SD", "ND"));
    data.add(new StateAdjacency("SD", "MN"));
    data.add(new StateAdjacency("SD", "IA"));
    data.add(new StateAdjacency("SD", "NE"));
    data.add(new StateAdjacency("SD", "WY"));
    data.add(new StateAdjacency("SD", "MT"));
    data.add(new StateAdjacency("TN", "KY"));
    data.add(new StateAdjacency("TN", "VA"));
    data.add(new StateAdjacency("TN", "NC"));
    data.add(new StateAdjacency("TN", "GA"));
    data.add(new StateAdjacency("TN", "AL"));
    data.add(new StateAdjacency("TN", "MS"));
    data.add(new StateAdjacency("TN", "AR"));
    data.add(new StateAdjacency("TN", "MO"));
    data.add(new StateAdjacency("TX", "OK"));
    data.add(new StateAdjacency("TX", "AR"));
    data.add(new StateAdjacency("TX", "LA"));
    data.add(new StateAdjacency("TX", "NM"));
    data.add(new StateAdjacency("UT", "ID"));
    data.add(new StateAdjacency("UT", "WY"));
    data.add(new StateAdjacency("UT", "CO"));
    data.add(new StateAdjacency("UT", "NM"));
    data.add(new StateAdjacency("UT", "AZ"));
    data.add(new StateAdjacency("UT", "NV"));
    data.add(new StateAdjacency("VT", "NY"));
    data.add(new StateAdjacency("VT", "NH"));
    data.add(new StateAdjacency("VT", "MA"));
    data.add(new StateAdjacency("VA", "MD"));
    data.add(new StateAdjacency("VA", "WV"));
    data.add(new StateAdjacency("VA", "KY"));
    data.add(new StateAdjacency("VA", "TN"));
    data.add(new StateAdjacency("VA", "NC"));
    data.add(new StateAdjacency("VA", "DC"));
    data.add(new StateAdjacency("WA", "OR"));
    data.add(new StateAdjacency("WA", "ID"));
    data.add(new StateAdjacency("WV", "OH"));
    data.add(new StateAdjacency("WV", "PA"));
    data.add(new StateAdjacency("WV", "MD"));
    data.add(new StateAdjacency("WV", "VA"));
    data.add(new StateAdjacency("WV", "KY"));
    data.add(new StateAdjacency("WI", "MI"));
    data.add(new StateAdjacency("WI", "IL"));
    data.add(new StateAdjacency("WI", "IA"));
    data.add(new StateAdjacency("WI", "MN"));
    data.add(new StateAdjacency("WY", "MT"));
    data.add(new StateAdjacency("WY", "ID"));
    data.add(new StateAdjacency("WY", "UT"));
    data.add(new StateAdjacency("WY", "CO"));
    data.add(new StateAdjacency("WY", "NE"));
    data.add(new StateAdjacency("WY", "SD"));
    STATE_ADJACENCIES = Collections.unmodifiableList(data);
  }
}
