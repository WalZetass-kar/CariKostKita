package com.carikostkita;

import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.model.ChatMessage;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
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
    public void testVerificationStatusParsing() {
        assertEquals(VerificationStatus.NONE, VerificationStatus.fromString("NONE"));
        assertEquals(VerificationStatus.PENDING, VerificationStatus.fromString("PENDING"));
        assertEquals(VerificationStatus.APPROVED, VerificationStatus.fromString("APPROVED"));
        assertEquals(VerificationStatus.REJECTED, VerificationStatus.fromString("REJECTED"));
        assertEquals(VerificationStatus.NONE, VerificationStatus.fromString("INVALID"));
        assertEquals(VerificationStatus.NONE, VerificationStatus.fromString(null));
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
        k.setIdPemilik(5);
        k.setPatokan("Dekat Gerbang Kampus UIR");

        assertEquals("Kost Mawar", k.getNamaKost());
        assertTrue(k.getFullLocation().contains("Simpang Tiga, Bukit Raya"));
        assertEquals("3x4 m", k.getUkuranKamar());
        assertEquals(10, k.getTotalKamar());
        assertEquals(3, k.getKamarTersedia());
        assertEquals(7, k.getKamarTerisi());
        assertEquals(5, k.getIdPemilik());
        assertEquals("Dekat Gerbang Kampus UIR", k.getPatokan());
        assertTrue(k.getFormattedHarga().contains("850.000") || k.getFormattedHarga().contains("850,000"));
    }

    @Test
    public void testUserRolesAndVerification() {
        User u = new User();
        u.setRole(Role.ADMIN);
        assertTrue(u.isAdmin());
        assertTrue(u.isDeveloper());
        assertFalse(u.isPemilikKost());

        u.setRole(Role.PEMILIK_KOST);
        assertFalse(u.isAdmin());
        assertTrue(u.isPemilikKost());

        u.setRole(Role.USER);
        u.setVerificationStatus(VerificationStatus.PENDING);
        assertFalse(u.isAdmin());
        assertFalse(u.isPemilikKost());
        assertEquals(VerificationStatus.PENDING, u.getVerificationStatus());
    }

    @Test
    public void testChatEntities() {
        ChatConversation conv = new ChatConversation();
        conv.setIdConversation(1);
        conv.setIdKost(10);
        conv.setIdPencari(2);
        conv.setIdPemilik(3);
        conv.setNamaKost("Kost Putri Melati");
        conv.setLastMessage("Apakah kamar masih ada?");

        assertEquals(1, conv.getIdConversation());
        assertEquals(10, conv.getIdKost());
        assertEquals("Kost Putri Melati", conv.getNamaKost());
        assertEquals("Apakah kamar masih ada?", conv.getLastMessage());

        ChatMessage msg = new ChatMessage(100, 1, 2, "Boleh survei besok siang?", "2026-10-01 10:00:00", false);
        assertEquals(100, msg.getIdMessage());
        assertEquals(1, msg.getIdConversation());
        assertEquals(2, msg.getIdSender());
        assertEquals("Boleh survei besok siang?", msg.getMessageText());
        assertFalse(msg.isRead());
    }

    @Test
    public void testKostFilterCriteriaSort() {
        KostFilterCriteria criteria = new KostFilterCriteria();
        assertEquals("TERBARU", criteria.getSortBy());

        criteria.setSortBy("TERMURAH");
        assertEquals("TERMURAH", criteria.getSortBy());

        criteria.setSortBy("TERMAHAL");
        assertEquals("TERMAHAL", criteria.getSortBy());
    }

    @Test
    public void testRoleUpgradeEnumsAndModels() {
        // VerificationStatus with REVISION_REQUIRED
        assertEquals(VerificationStatus.REVISION_REQUIRED, VerificationStatus.fromString("REVISION_REQUIRED"));
        assertTrue(VerificationStatus.REVISION_REQUIRED.isRevisionRequired());

        // KostVerificationStatus
        assertEquals(com.carikostkita.data.model.KostVerificationStatus.PENDING, com.carikostkita.data.model.KostVerificationStatus.fromString("PENDING"));
        assertEquals(com.carikostkita.data.model.KostVerificationStatus.APPROVED, com.carikostkita.data.model.KostVerificationStatus.fromString("APPROVED"));
        assertEquals(com.carikostkita.data.model.KostVerificationStatus.REJECTED, com.carikostkita.data.model.KostVerificationStatus.fromString("REJECTED"));
        assertEquals(com.carikostkita.data.model.KostVerificationStatus.REVISION_REQUIRED, com.carikostkita.data.model.KostVerificationStatus.fromString("REVISION_REQUIRED"));

        // ReportStatus
        assertEquals(com.carikostkita.data.model.ReportStatus.BARU, com.carikostkita.data.model.ReportStatus.fromString("BARU"));
        assertEquals(com.carikostkita.data.model.ReportStatus.DITINJAU, com.carikostkita.data.model.ReportStatus.fromString("DITINJAU"));
        assertEquals(com.carikostkita.data.model.ReportStatus.SELESAI, com.carikostkita.data.model.ReportStatus.fromString("SELESAI"));
        assertEquals(com.carikostkita.data.model.ReportStatus.DITOLAK, com.carikostkita.data.model.ReportStatus.fromString("DITOLAK"));

        // User active & revision note
        User u = new User();
        u.setActive(true);
        assertTrue(u.isActive());
        u.setActive(false);
        assertFalse(u.isActive());
        u.setCatatanRevisi("Perbaiki KTP");
        assertEquals("Perbaiki KTP", u.getCatatanRevisi());

        // Kost verification & revision note
        Kost k = new Kost();
        k.setVerificationStatus(com.carikostkita.data.model.KostVerificationStatus.REVISION_REQUIRED);
        k.setCatatanRevisi("Perjelas patokan");
        assertEquals(com.carikostkita.data.model.KostVerificationStatus.REVISION_REQUIRED, k.getVerificationStatus());
        assertEquals("Perjelas patokan", k.getCatatanRevisi());

        // KostReport
        com.carikostkita.data.model.KostReport report = new com.carikostkita.data.model.KostReport(1, 2, 3, "ALAMAT_TIDAK_SESUAI", "Alamat palsu");
        report.setIdReport(10);
        report.setNamaKost("Kost Melati");
        report.setNamaReporter("Budi");
        report.setNamaPemilik("H. Rahmat");
        assertEquals(10, report.getIdReport());
        assertEquals("ALAMAT_TIDAK_SESUAI", report.getKategoriLaporan());
        assertEquals("ALAMAT_TIDAK_SESUAI", report.getAlasan());
        assertEquals("Budi", report.getNamaPelapor());

        // SystemActivityLog
        com.carikostkita.data.model.SystemActivityLog log = new com.carikostkita.data.model.SystemActivityLog(1, "Admin", "APPROVE_OWNER", "Setujui H. Rahmat", "USER", 5);
        assertEquals(1, log.getIdUser());
        assertEquals("Admin", log.getActorName());
        assertEquals("Admin", log.getUserName());
        assertEquals("APPROVE_OWNER", log.getActionType());
        assertEquals("Setujui H. Rahmat", log.getDescription());
    }
}
