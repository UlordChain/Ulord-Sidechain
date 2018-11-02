package co.usc.peg.whitelist;

import co.usc.ulordj.core.Address;
import co.usc.ulordj.core.Coin;

public class OneOffWhiteListEntry implements LockWhitelistEntry {
    private final Address address;
    private final Coin maxTransferValueField;

    private boolean consumed = false;

    public OneOffWhiteListEntry(Address address, Coin maxTransferValue) {
        this.address =address;
        this.maxTransferValueField = maxTransferValue;
    }

    public Address address() {
        return this.address;
    }

    public boolean canLock(Coin value) {
        return !this.consumed && (this.maxTransferValueField.compareTo(value) >= 0);
    }

    public boolean isConsumed() {
        return this.consumed;
    }

    public void consume() {
        this.consumed = true;
    }

    public Coin maxTransferValue() { return this.maxTransferValueField; }
}
