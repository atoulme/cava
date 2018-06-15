package net.consensys.cava.devp2p;

import net.consensys.cava.bytes.Bytes;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An Ethereum P2P network peer.
 */
public interface Peer {

  /**
   * @return The nodeId for this peer.
   */
  Bytes nodeId();

  /**
   * @return The endpoint for this peer, if known.
   */
  @Nullable
  Endpoint endpoint();

  /**
   * @return The set of capabilities this peer current has.
   */
  Set<String> capabilities();

  /**
   * Check if this peer has the given capability.
   *
   * @param capability The capability to check for.
   * @return <tt>true</tt> if this peer has the specified capability.
   */
  boolean hasCapability(String capability);

  /**
   * Check if this peer is active.
   *
   * An active peer is a peer that the Ethereum client is currently communicating with at the discovery layer. It will
   * therefore have endpoint information available.
   *
   * @return <tt>true</tt> if this peer is active.
   */
  boolean isActive();

  /**
   * @return The point-in-time when this peer was last seen.
   */
  @Nullable
  Instant lastSeen();

  /**
   * Set this peer to active state, with the given endpoint.
   *
   * If this peer is already active, then the endpoint is not changed.
   *
   * @param endpoint The endpoint for communicating with this peer.
   */
  void setActive(Endpoint endpoint);

  /**
   * Set the capabilities for this peer.
   *
   * If this peer is not active, then the capabilities will not be set.
   *
   * @param capabilities The capabilities to add.
   */
  void setCapabilities(String... capabilities);

  /**
   * Set the capabilities for this peer.
   *
   * If this peer is not active, then the capabilities will not be set.
   *
   * @param capabilities The capabilities to add.
   */
  void setCapabilities(Collection<String> capabilities);

  /**
   * Set this peer to inactive state.
   */
  void setInactive();

  /**
   * Update the endpoint for this peer, if it is inactive.
   *
   * @param endpoint The endpoint for communicating with the peer.
   */
  void updateEndpointIfInactive(Endpoint endpoint);

  /**
   * Update the point-in-time when this peer was last seen to the current time.
   */
  void updateLastSeen();
}
