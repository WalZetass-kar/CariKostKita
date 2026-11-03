package com.carikostkita;

import com.carikostkita.data.model.RoommatePost;
import org.junit.Test;
import static org.junit.Assert.*;

public class RoommateModelTest {

    @Test
    public void testRoommatePostPropertiesAndFormatting() {
        RoommatePost post = new RoommatePost(
                "Rian Pratama",
                "UNRI Panam",
                "Teknik Informatika",
                "PUTRA",
                450000,
                "Jl. Binawidya",
                "Kamar Mandi Dalam",
                "Cari teman sekamar rapi dan ramah",
                "081270008899"
        );

        assertEquals("Rian Pratama", post.getName());
        assertEquals("UNRI Panam", post.getCampus());
        assertEquals("Teknik Informatika", post.getFaculty());
        assertEquals("PUTRA", post.getGender());
        assertEquals(450000, post.getBudget());
        assertTrue(post.isAvailable());

        assertEquals("👦 Mahasiswa (Putra)", post.getGenderLabel());
        assertTrue(post.getFormattedBudget().contains("450.000"));

        RoommatePost putriPost = new RoommatePost();
        putriPost.setGender("PUTRI");
        assertEquals("👧 Mahasiswi (Putri)", putriPost.getGenderLabel());
    }
}
