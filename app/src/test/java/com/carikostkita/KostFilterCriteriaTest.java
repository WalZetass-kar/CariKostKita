package com.carikostkita;

import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.TipeKost;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class KostFilterCriteriaTest {

    @Test
    public void testFilterCriteriaReset() {
        KostFilterCriteria criteria = new KostFilterCriteria();
        criteria.setKeyword("Melati");
        criteria.setMaxHarga(1000000.0);
        criteria.setTipeKost(TipeKost.PUTRI);
        criteria.setFasilitasIds(Arrays.asList(1, 2, 4));

        assertEquals("Melati", criteria.getKeyword());
        assertEquals(Double.valueOf(1000000.0), criteria.getMaxHarga());
        assertEquals(TipeKost.PUTRI, criteria.getTipeKost());
        assertEquals(3, criteria.getFasilitasIds().size());

        criteria.reset();
        assertNull(criteria.getKeyword());
        assertNull(criteria.getMaxHarga());
        assertNull(criteria.getTipeKost());
        assertTrue(criteria.getFasilitasIds().isEmpty());
    }
}
