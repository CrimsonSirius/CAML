package org.chs.camlauncher.backend.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OfflineAccountTest {
    @Test
    void createsStableOfflineSession() {
        OfflineAccount account = new OfflineAccount("Steve");

        AuthSession first = account.login();
        AuthSession second = account.login();

        assertEquals("Steve", first.username());
        assertEquals(first.uuid(), second.uuid());
        assertEquals(OfflineAccount.offlineUuid("Steve"), first.uuid());
        assertEquals("legacy", first.userType());
        assertEquals("{}", first.userProperties());
        assertFalse(first.accessToken().isBlank());
    }

    @Test
    void rejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> new OfflineAccount(""));
        assertThrows(IllegalArgumentException.class, () -> new OfflineAccount("   "));
        assertThrows(IllegalArgumentException.class, () -> new OfflineAccount(null));
    }
}
