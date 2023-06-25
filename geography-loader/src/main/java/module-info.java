module dgroomes.geographyloader {
  requires dgroomes.geography;
  requires com.fasterxml.jackson.databind;
  requires org.slf4j;
  requires dgroomes.util;
  requires dgroomes.data_system_serial_indices_arrays;
  exports dgroomes.loader;
}
