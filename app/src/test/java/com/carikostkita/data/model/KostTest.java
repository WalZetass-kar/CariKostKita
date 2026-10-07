package com.carikostkita.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class KostTest {

    @Test
    public void unknownRoomCountIsNotShownAsANumber() {
        Kost k = new Kost();
        k.setStatus(StatusKost.TERSEDIA);
        assertFalse(k.hasRoomInfo());
        assertTrue(k.isAvailable());
        assertEquals("Tersedia", k.getAvailabilityLabel());
    }

    @Test
    public void zeroRoomsMeansFull() {
        Kost k = new Kost();
        k.setStatus(StatusKost.TERSEDIA);
        k.setKamarTersedia(0);
        assertFalse(k.isAvailable());
        assertEquals("Penuh", k.getAvailabilityLabel());
    }

    @Test
    public void inactiveKostIsLabelledInactive() {
        Kost k = new Kost();
        k.setStatus(StatusKost.TIDAK_AKTIF);
        k.setKamarTersedia(3);
        assertEquals("Nonaktif", k.getAvailabilityLabel());
    }

    @Test
    public void newKostDefaultsArePendingAndHonest() {
        Kost k = new Kost();
        assertEquals(KostVerificationStatus.PENDING, k.getVerificationStatus());
        assertNull(k.getUkuranKamar());
        assertEquals(0, k.getTotalKamar());
    }

    @Test
    public void facilityIdsComeFromMasterListNotPosition() {
        Kost k = new Kost();
        k.setFasilitas(Arrays.asList("Air Bersih 24 Jam", "AC Dingin"));
        assertEquals(13, k.getListFasilitas().get(0).getIdFasilitas());
        assertEquals(4, k.getListFasilitas().get(1).getIdFasilitas());
    }

    @Test
    public void ratingLabelHiddenWithoutReviews() {
        Kost k = new Kost();
        assertNull(k.getRatingLabel());
        k.setRatingAvg(4.5);
        k.setRatingCount(12);
        assertEquals("★ 4,5 (12)", k.getRatingLabel());
    }
}
