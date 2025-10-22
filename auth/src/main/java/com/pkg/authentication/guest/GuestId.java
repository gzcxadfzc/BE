package com.pkg.authentication.guest;

import com.pkg.authentication.core.Credential;
import com.pkg.authentication.core.CredentialType;

public class GuestId extends Credential {

    private final String id;

    protected GuestId(CredentialType authenticationType, String id) {
        super(authenticationType);
        this.id = id;
    }

    public static GuestId of(String id) {
        return new GuestId(CredentialType.USERNAME_PASSWORD, id);
    }

    public String getId() {
        return id;
    }
}
