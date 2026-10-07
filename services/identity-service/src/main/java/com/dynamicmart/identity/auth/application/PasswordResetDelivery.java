package com.dynamicmart.identity.auth.application;

import com.dynamicmart.identity.auth.domain.UserAccount;

public interface PasswordResetDelivery {
    void deliver(UserAccount user, String rawToken);
}
