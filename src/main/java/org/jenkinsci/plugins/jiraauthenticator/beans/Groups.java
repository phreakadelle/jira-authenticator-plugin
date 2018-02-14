package org.jenkinsci.plugins.jiraauthenticator.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean class for response parsing.
 * 
 * @author stephan.watermeyer
 *
 */
public class Groups {

    String size;

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    List<Item> items;

    public List<Item> getItems() {
        if (items == null) {
            items = new ArrayList<Item>();
        }
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Groups [size=" + size + ", Items [");
        for (Item current : getItems()) {
            sb.append(current.toString()).append(", ");
        }
        sb.append(" ]");

        return sb.toString();
    }

}
