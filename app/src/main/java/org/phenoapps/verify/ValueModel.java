package org.phenoapps.verify;

public class ValueModel {
    private String prefix;
    private String value;

    public ValueModel(){
        this.value = "";
        this.prefix = "";
    }

    public String getPrefix() {
        return prefix;
    }

    public String getValue() {
        return value;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
