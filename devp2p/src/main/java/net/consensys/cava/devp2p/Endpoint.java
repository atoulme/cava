package net.consensys.cava.devp2p;

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPReader;
import net.consensys.cava.rlp.RLPWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import com.google.common.net.InetAddresses;

/**
 * An Ethereum node endpoint.
 */
public final class Endpoint {

  /**
   * The default port used by Ethereum DevP2P.
   */
  public static final int DEFAULT_PORT = 30303;

  private final String address;
  private final int udpPort;
  private final int tcpPort;

  /**
   * Create a new endpoint.
   *
   * @param address The IP string literal.
   * @param udpPort The UDP port for the endpoint.
   * @param tcpPort The TCP port for the endpoint.
   * @throws IllegalArgumentException If the address isn't an IP address, or either port is out of range.
   */
  public Endpoint(String address, int udpPort, int tcpPort) {
    checkArgument(
        address != null && InetAddresses.isInetAddress(address),
        "host requires a valid IP address, got " + address);
    checkArgument(udpPort > 0 && udpPort < 65536, "UDP port requires a value between 1 and 65535, got " + udpPort);
    checkArgument(tcpPort > 0 && tcpPort < 65536, "TCP port requires a value between 1 and 65535, got " + tcpPort);

    this.address = address;
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
  }

  /**
   * The IP address for this endpoint.
   *
   * @return An IP address.
   */
  public String address() {
    return address;
  }

  /**
   * The UDP port for this endpoint.
   *
   * @return The UDP port number.
   */
  public int udpPort() {
    return udpPort;
  }

  /**
   * The TCP port for this endpoint.
   *
   * @return The TCP port number.
   */
  public int tcpPort() {
    return tcpPort;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Endpoint)) {
      return false;
    }
    Endpoint other = (Endpoint) obj;
    return address.equals(other.address) && (this.udpPort == other.udpPort) && (this.tcpPort == other.tcpPort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, udpPort, tcpPort);
  }

  @Override
  public String toString() {
    return "Endpoint{" + "address='" + address + '\'' + ", udpPort=" + udpPort + ", tcpPort=" + tcpPort + '}';
  }

  /**
   * Write this endpoint as an RLP list item.
   *
   * @return the result of the RLP encoding as {@link Bytes}
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);

  }

  /**
   * Write this endpoint as separate fields.
   *
   * @param writer The RLP writer.
   */
  void writeTo(RLPWriter writer) {
    writer.writeValue(Bytes.of(InetAddresses.forString(address).getAddress()));
    writer.writeInt(udpPort);
    writer.writeInt(tcpPort);
  }

  /**
   * Decodes the RLP stream as a standalone Endpoint instance, which is not part of a Peer.
   *
   * @param reader The RLP input stream from which to read.
   * @return The decoded endpoint.
   */
  public static Endpoint readFrom(RLPReader reader) {
    return reader.readList(Endpoint::read);
  }

  /**
   * Create an Endpoint by reading fields from the RLP input stream.
   *
   * @param reader The RLP input stream from which to read.
   * @return The decoded endpoint.
   */
  public static Endpoint read(RLPReader reader) {
    InetAddress addr;
    try {
      addr = InetAddress.getByAddress(reader.readValue().toArrayUnsafe());
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
    int udpPort = reader.readInt();

    // Some mainnet packets have been shown to either not have the TCP port field at all,
    // or to have an RLP NULL value for it. Assume the same as the UDP port if it's missing.
    int tcpPort = udpPort;
    if (!reader.isComplete()) {
      tcpPort = reader.readInt();
      if (tcpPort == 0) {
        tcpPort = udpPort;
      }
    }

    return new Endpoint(addr.getHostAddress(), udpPort, tcpPort);
  }
}
