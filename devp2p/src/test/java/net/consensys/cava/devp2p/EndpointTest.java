package net.consensys.cava.devp2p;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EndpointTest {

  @Test
  void endpointsWithSameHostAndPortsAreEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.1", 7654, 8765);
    assertTrue(endpoint1.equals(endpoint2));
  }

  @Test
  void endpointsWithDifferentHostsAreNotEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.2", 7654, 8765);
    assertFalse(endpoint1.equals(endpoint2));
  }

  @Test
  void endpointsWithDifferentUDPPortsAreNotEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.1", 7655, 8765);
    assertFalse(endpoint1.equals(endpoint2));
  }

  @Test
  void endpointsWithDifferentTCPPortsAreNotEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.1", 7654, 8766);
    assertFalse(endpoint1.equals(endpoint2));
  }

  @Test
  void invalidUDPPortThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Endpoint("127.0.0.1", 76543321, 8765);
    });
  }

  @Test
  void invalidTCPPortThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Endpoint("127.0.0.1", 7654, 87654321);
    });
  }
}
