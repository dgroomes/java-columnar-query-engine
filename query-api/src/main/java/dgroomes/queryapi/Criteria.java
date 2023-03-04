package dgroomes.queryapi;

sealed public interface Criteria permits Criteria.IntCriteria, Criteria.StringCriteria {
  non-sealed interface IntCriteria extends Criteria {
    boolean match(int integerUnderTest);
  }

  non-sealed interface StringCriteria extends Criteria {
    boolean match(String stringUnderTest);
  }
}
