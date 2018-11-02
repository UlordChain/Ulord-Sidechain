package co.usc.peg.whitelist;

import co.usc.ulordj.core.Address;
import co.usc.ulordj.core.Coin;

/**
 * Represents a lock whitelist
 * entry for a LockWhiteList.
 *
 * @author Jose Dahlquist
 */
public interface LockWhitelistEntry {
    Address address();
    boolean canLock(Coin value);
    boolean isConsumed();
    void consume();
}
