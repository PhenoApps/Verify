package org.phenoapps.verify;

public class EntryItem {
    public String prefix;
    private String value;

    public EntryItem(String prefix, String value) {
        this.prefix = prefix;
        this.value = value;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getValue() {
        return value;
    }
}


