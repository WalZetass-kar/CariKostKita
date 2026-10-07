package com.carikostkita.data.repository;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KostRepositoryTest {

    @Test
    public void sanitizeKeyword_removesPostgrestOperators() {
        // Koma, kurung, titik, dan bintang punya arti khusus di filter PostgREST
        assertEquals("Kost Jakarta", KostRepository.sanitizeKeyword("Kost, Jakarta"));
        assertEquals("a b", KostRepository.sanitizeKeyword("a)(b"));
        assertEquals("Jl Sudirman", KostRepository.sanitizeKeyword("Jl. Sudirman*"));
    }

    @Test
    public void sanitizeKeyword_keepsLettersDigitsDashAndApostrophe() {
        assertEquals("Kost Bu Ani's 2-B", KostRepository.sanitizeKeyword("  Kost   Bu Ani's 2-B "));
    }

    @Test
    public void sanitizeKeyword_nullIsEmpty() {
        assertEquals("", KostRepository.sanitizeKeyword(null));
    }
}
