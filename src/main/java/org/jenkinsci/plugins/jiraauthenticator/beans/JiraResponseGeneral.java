package org.jenkinsci.plugins.jiraauthenticator.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used to parse the JSON response from Jira.
 * 
 * @author stephan.watermeyer
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JiraResponseGeneral {

    private String key;
    private String name;
    private String emailAddress;
    private Groups groups;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Groups getGroups() {
        return groups;
    }

    public void setGroups(Groups groups) {
        this.groups = groups;
    }

    @Override
    public String toString() {
        return "JiraResponseGeneral [key=" + key + ", name=" + name + ", emailAddress=" + emailAddress + ", groups=" + groups + "]";
    }

}
