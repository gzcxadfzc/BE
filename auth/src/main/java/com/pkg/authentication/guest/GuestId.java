package com.pkg.authentication.guest;

import com.pkg.core.Credential;
import com.pkg.core.CredentialType;

public class GuestId extends Credential {

    private final String id;

    protected GuestId(CredentialType authenticationType, String id) {
        super(authenticationType);
        this.id = id;
    }

    public static GuestId of(String id) {
        return new GuestId(CredentialType.ID, id);
    }

    public String getId() {
        return id;
    }
}
