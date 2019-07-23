package com.codesignal.csbot.models;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;


@Table(value = "codesignal_user")
public class CodesignalUser implements Serializable {
    @PrimaryKey
    private final String email;

    @Column
    private final String pass;

    public CodesignalUser(String email, String pass) {
        this.email = email;
        this.pass = pass;
    }

    public String getEmail() {
        return email;
    }

    public String getPass() {
        return pass;
    }
}
