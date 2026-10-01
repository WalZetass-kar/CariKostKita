package com.carikostkita;

import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.model.User;
import org.junit.Test;
import static org.junit.Assert.*;

public class DomainModelTest {

    @Test
    public void testRoleParsing() {
        assertEquals(Role.ADMIN, Role.fromString("ADMIN"));
        assertEquals(Role.PEMILIK_KOST, Role.fromString("PEMILIK_KOST"));
        assertEquals(Role.USER, Role.fromString("USER"));
        assertEquals(Role.USER, Role.fromString("unknown"));
        assertEquals(Role.USER, Role.fromString(null));
    }

    @Test
    public void testTipeKostParsing() {
        assertEquals(TipeKost.PUTRI, TipeKost.fromString("PUTRI"));
        assertEquals(TipeKost.PUTRA, TipeKost.fromString("PUTRA"));
        assertEquals(TipeKost.CAMPUR, TipeKost.fromString("CAMPUR"));
        assertEquals(TipeKost.CAMPUR, TipeKost.fromString(null));
    }

    @Test
    public void testStatusKostParsing() {
        assertEquals(StatusKost.TERSEDIA, StatusKost.fromString("TERSEDIA"));
        assertEquals(StatusKost.PENUH, StatusKost.fromString("PENUH"));
        assertEquals(StatusKost.TIDAK_AKTIF, StatusKost.fromString("TIDAK_AKTIF"));
        assertEquals(StatusKost.TERSEDIA, StatusKost.fromString(null));
    }

    @Test
    public void testKostModel() {
        Kost k = new Kost();
        k.setNamaKost("Kost Mawar");
        k.setHarga(850000);
        k.setKelurahan("Simpang Tiga");
        k.setKecamatan("Bukit Raya");
        k.setUkuranKamar("3x4 m");
        k.setTotalKamar(10);
        k.setKamarTersedia(3);

        assertEquals("Kost Mawar", k.getNamaKost());
        assertTrue(k.getFullLocation().contains("Simpang Tiga, Bukit Raya"));
        assertEquals("3x4 m", k.getUkuranKamar());
        assertEquals(10, k.getTotalKamar());
        assertEquals(3, k.getKamarTersedia());
        assertEquals(7, k.getKamarTerisi());
        assertTrue(k.getFormattedHarga().contains("850.000") || k.getFormattedHarga().contains("850,000"));
    }

    @Test
    public void testUserRoles() {
        User u = new User();
        u.setRole(Role.ADMIN);
        assertTrue(u.isAdmin());
        assertTrue(u.isDeveloper());
        assertFalse(u.isPemilikKost());

        u.setRole(Role.PEMILIK_KOST);
        assertFalse(u.isAdmin());
        assertTrue(u.isPemilikKost());

        u.setRole(Role.USER);
        assertFalse(u.isAdmin());
        assertFalse(u.isPemilikKost());
    }
}
