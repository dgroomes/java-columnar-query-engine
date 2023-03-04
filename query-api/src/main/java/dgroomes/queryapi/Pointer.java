package dgroomes.queryapi;

sealed public interface Pointer {
  record Ordinal(int ordinal) implements Pointer {}

  // This is a nested pointer. This is the unit of recursion.
  record NestedPointer(int ordinal, Pointer pointer) implements Pointer {}
}
