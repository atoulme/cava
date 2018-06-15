package net.consensys.cava.devp2p;

import static net.consensys.cava.bytes.Bytes.fromHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeerRepositoryTest {

  private PeerRepository peerRepository;

  private Instant now = Instant.now();

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    peerRepository = new PeerRepository(() -> now);
  }

  @Test
  void shouldReturnInactivePeerWithNoEndpointForUnknownId() {
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    assertFalse(peer.isActive());
    assertNull(peer.endpoint());
  }

  @Test
  void shouldReturnInactivePeerWithEndpointForUnknownIdAndEndpoint() {
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3), endpoint);
    assertFalse(peer.isActive());
    assertNotNull(peer.endpoint());
    assertEquals(endpoint, peer.endpoint());
  }

  @Test
  void shouldReturnInactivePeerBasedOnURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:7654");
    assertFalse(peer.isActive());
    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    Endpoint endpoint = peer.endpoint();
    assertNotNull(endpoint);
    assertEquals("172.20.0.4", endpoint.address());
    assertEquals(7654, endpoint.udpPort());
    assertEquals(7654, endpoint.tcpPort());
  }

  @Test
  void shouldReturnPeerWithDefaultPortsWhenMissingFromURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4");
    assertFalse(peer.isActive());
    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    Endpoint endpoint = peer.endpoint();
    assertNotNull(endpoint);
    assertEquals("172.20.0.4", endpoint.address());
    assertEquals(30303, endpoint.udpPort());
    assertEquals(30303, endpoint.tcpPort());
  }

  @Test
  void shouldReturnPeerWithDifferentPortsWhenQueryParamInURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=23456");
    assertFalse(peer.isActive());
    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    Endpoint endpoint = peer.endpoint();
    assertNotNull(endpoint);
    assertEquals("172.20.0.4", endpoint.address());
    assertEquals(23456, endpoint.udpPort());
    assertEquals(54789, endpoint.tcpPort());
  }

  @Test
  void shouldThrowWhenNotEnodeURI() {
    assertThrows(IllegalArgumentException.class, () -> {
      peerRepository.get(
          "http://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:30303");
    });
  }

  @Test
  void shouldThrowWhenNoNodeIdInURI() {
    assertThrows(IllegalArgumentException.class, () -> {
      peerRepository.get("enode://172.20.0.4:30303");
    });
  }

  @Test
  void shouldThrowWhenInvalidPortInURI() {
    assertThrows(
        IllegalArgumentException.class,
        () -> peerRepository.get(
            "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:98766"));
  }

  @Test
  void shouldThrowWhenOutOfRangeDiscPortInURI() {
    assertThrows(
        IllegalArgumentException.class,
        () -> peerRepository.get(
            "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=98765"));
  }

  @Test
  void shouldThrowWhenInvalidDiscPortInURI() {
    assertThrows(
        IllegalArgumentException.class,
        () -> peerRepository.get(
            "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=abcd"));
  }

  @Test
  void shouldIgnoreAdditionalQueryParametersInURI() {
    Peer peer = peerRepository.get(
        "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?foo=bar&discport=23456&bar=foo");
    assertFalse(peer.isActive());
    assertEquals(
        fromHexString(
            "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"),
        peer.nodeId());
    Endpoint endpoint = peer.endpoint();
    assertNotNull(endpoint);
    assertEquals("172.20.0.4", endpoint.address());
    assertEquals(23456, endpoint.udpPort());
    assertEquals(54789, endpoint.tcpPort());
  }

  @Test
  void shouldUpdatePeerWhenGettingInactiveWithNewEndpoint() {
    Bytes nodeId = Bytes.of(1, 2, 3);
    Peer peer1 = peerRepository.get(nodeId);
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer2 = peerRepository.get(nodeId, endpoint);
    assertSame(peer2, peer1);
    assertNotNull(peer1.endpoint());
    assertEquals(endpoint, peer1.endpoint());
  }

  @Test
  void shouldNotUpdateEndpointWhenGettingActivePeer() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer1 = peerRepository.get(Bytes.of(1, 2, 3));
    peer1.setActive(endpoint1);
    Endpoint endpoint2 = new Endpoint("127.0.0.2", 30304, 30304);
    Peer peer2 = peerRepository.get(Bytes.of(1, 2, 3), endpoint2);
    assertSame(peer2, peer1);
    assertNotNull(peer1.endpoint());
    assertEquals(endpoint1, peer1.endpoint());
  }

  @Test
  void shouldNotUpdateEndpointWhenSettingActivePeerToActive() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint1);
    Endpoint endpoint2 = new Endpoint("127.0.0.2", 30304, 30304);
    peer.setActive(endpoint2);
    assertNotNull(peer.endpoint());
    assertEquals(endpoint1, peer.endpoint());
  }

  @Test
  void shouldNotUpdateCapabilitiesForInactivePeer() {
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setCapabilities("eth");
    assertEquals(Collections.emptySet(), peer.capabilities());
    peer.setActive(new Endpoint("127.0.0.1", 30303, 30303));
    peer.setInactive();
    peer.setCapabilities(Collections.singletonList("eth"));
    assertEquals(Collections.emptySet(), peer.capabilities());
  }

  @Test
  void shouldNotifyObserversWhenPeerIsAdded() {
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    Consumer<Peer> observer = notifiedPeer::set;
    peerRepository.observePeerAddition(observer);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    assertEquals(peer, notifiedPeer.get());
    assertTrue(peerRepository.unObservePeerAddition(observer));
  }

  @Test
  void shouldNotifyObserversWhenPeerBecomesActive() {
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    Consumer<Peer> observer = notifiedPeer::set;
    peerRepository.observePeerActive(observer);

    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);

    assertTrue(peer.isActive());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerActive(observer));
  }

  @Test
  void shouldNotifyObserversWhenPeerBecomesInActive() {
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    Consumer<Peer> observer = notifiedPeer::set;
    peerRepository.observePeerInactive(observer);

    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);

    assertTrue(peer.isActive());
    assertNull(notifiedPeer.get());

    peer.setInactive();
    assertFalse(peer.isActive());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerInactive(observer));
  }

  @Test
  void shouldNotifyObserversWhenPeerCapabilitiesAreUpdated() {
    AtomicReference<Set<String>> notifiedCapabilities = new AtomicReference<>();
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    BiConsumer<Set<String>, Peer> observer = (caps, peer) -> {
      notifiedCapabilities.set(caps);
      notifiedPeer.set(peer);
    };
    peerRepository.observePeerCapabilities(observer);

    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);

    assertTrue(peer.isActive());
    assertNull(notifiedPeer.get());

    peer.setCapabilities(Collections.singletonList("eth"));
    assertTrue(peer.hasCapability("eth"));
    assertEquals(Collections.singleton("eth"), peer.capabilities());

    assertEquals(Collections.emptySet(), notifiedCapabilities.get());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerCapabilities(observer));
  }

  @Test
  void shouldNotifyCapabilityObserversWhenPeerBecomesInactive() {
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);
    peer.setCapabilities("eth");

    AtomicReference<Set<String>> notifiedCapabilities = new AtomicReference<>();
    AtomicReference<Peer> notifiedPeer = new AtomicReference<>();
    BiConsumer<Set<String>, Peer> observer = (caps, observedPeer) -> {
      notifiedCapabilities.set(caps);
      notifiedPeer.set(observedPeer);
    };
    peerRepository.observePeerCapabilities(observer);

    peer.setInactive();

    assertEquals(Collections.emptySet(), peer.capabilities());

    assertEquals(Collections.singleton("eth"), notifiedCapabilities.get());
    assertEquals(peer, notifiedPeer.get());

    assertTrue(peerRepository.unObservePeerCapabilities(observer));
  }

  @Test
  void setLastSeenToCurrentTime() {
    Endpoint endpoint = new Endpoint("127.0.0.1", 30303, 30303);
    Peer peer = peerRepository.get(Bytes.of(1, 2, 3));
    peer.setActive(endpoint);
    peer.updateLastSeen();

    assertEquals(now, peer.lastSeen());
  }
}
