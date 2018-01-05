package org.jenkinsci.plugins.jiraauthenticator.beans;

/**
 * Bean class for response parsing.
 * 
 * @author stephan.watermeyer
 *
 */
public class Item {

    String name;
    String self;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    @Override
    public String toString() {
        return "Item [name=" + name + ", self=" + self + "]";
    }

}
