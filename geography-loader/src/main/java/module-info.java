module dgroomes.geographyloader {
  requires dgroomes.geography;
  requires com.fasterxml.jackson.databind;
  requires org.slf4j;
  requires dgroomes.util;
  requires dgroomes.queryengine;
  exports dgroomes.loader;
}
