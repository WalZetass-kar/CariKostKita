package com.carikostkita.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RoleTest {

    @Test
    public void databaseValuesMapToRoles() {
        assertEquals(Role.USER, Role.fromString("user"));
        assertEquals(Role.PEMILIK_KOST, Role.fromString("owner"));
        assertEquals(Role.ADMIN, Role.fromString("developer"));
        assertEquals(Role.MODERATOR, Role.fromString("moderator"));
        assertEquals(Role.USER, Role.fromString(null));
    }

    @Test
    public void moderatorIsStaffButNotSuperAdmin() {
        assertTrue(Role.MODERATOR.isDeveloper());
        assertFalse(Role.MODERATOR.isSuperAdmin());
        assertTrue(Role.ADMIN.isSuperAdmin());
        assertEquals("moderator", Role.MODERATOR.toSupabaseRole());
    }
}
