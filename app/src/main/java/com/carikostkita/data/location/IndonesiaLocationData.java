package com.carikostkita.data.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Master data hierarki administrasi Republik Indonesia untuk CariKostKita.
 * Menghubungkan seluruh 38 Provinsi -> Kabupaten/Kota -> Kecamatan -> Desa/Kelurahan
 * serta mendukung autocomplete nama jalan dan input wilayah kustom di seluruh Indonesia.
 */
public class IndonesiaLocationData {

    // 38 Provinsi Resmi di Indonesia
    private static final List<String> PROVINSI_LIST = Arrays.asList(
            "Aceh",
            "Sumatera Utara",
            "Sumatera Barat",
            "Riau",
            "Kepulauan Riau",
            "Jambi",
            "Sumatera Selatan",
            "Bangka Belitung",
            "Bengkulu",
            "Lampung",
            "DKI Jakarta",
            "Jawa Barat",
            "Banten",
            "Jawa Tengah",
            "DI Yogyakarta",
            "Jawa Timur",
            "Bali",
            "Nusa Tenggara Barat",
            "Nusa Tenggara Timur",
            "Kalimantan Barat",
            "Kalimantan Tengah",
            "Kalimantan Selatan",
            "Kalimantan Timur",
            "Kalimantan Utara",
            "Sulawesi Utara",
            "Gorontalo",
            "Sulawesi Tengah",
            "Sulawesi Barat",
            "Sulawesi Selatan",
            "Sulawesi Tenggara",
            "Maluku",
            "Maluku Utara",
            "Papua",
            "Papua Barat",
            "Papua Tengah",
            "Papua Pegunungan",
            "Papua Selatan",
            "Papua Barat Daya"
    );

    private static final Map<String, List<String>> KOTA_MAP = new HashMap<>();
    private static final Map<String, List<String>> KECAMATAN_MAP = new HashMap<>();
    private static final Map<String, List<String>> KELURAHAN_MAP = new HashMap<>();
    private static final List<String> COMMON_STREETS = new ArrayList<>();

    static {
        // =========================================================================
        // 1. KABUPATEN & KOTA LENGKAP SELURUH 38 PROVINSI INDONESIA
        // =========================================================================

        KOTA_MAP.put("Aceh", Arrays.asList(
                "Kota Banda Aceh", "Kota Sabang", "Kota Lhokseumawe", "Kota Langsa", "Kota Subulussalam",
                "Kabupaten Aceh Besar", "Kabupaten Aceh Barat", "Kabupaten Aceh Selatan", "Kabupaten Aceh Tengah",
                "Kabupaten Aceh Utara", "Kabupaten Aceh Timur", "Kabupaten Bireuen", "Kabupaten Pidie",
                "Kabupaten Pidie Jaya", "Kabupaten Bener Meriah", "Kabupaten Aceh Tenggara", "Kabupaten Gayo Lues",
                "Kabupaten Aceh Barat Daya", "Kabupaten Aceh Jaya", "Kabupaten Nagan Raya", "Kabupaten Aceh Tamiang",
                "Kabupaten Simeulue", "Kabupaten Aceh Singkil"
        ));

        KOTA_MAP.put("Sumatera Utara", Arrays.asList(
                "Kota Medan", "Kota Binjai", "Kota Pematangsiantar", "Kota Tebing Tinggi",
                "Kota Tanjungbalai", "Kota Sibolga", "Kota Padangsidimpuan", "Kota Gunungsitoli",
                "Kabupaten Deli Serdang", "Kabupaten Karo", "Kabupaten Langkat", "Kabupaten Simalungun",
                "Kabupaten Asahan", "Kabupaten Labuhanbatu", "Kabupaten Tapanuli Utara", "Kabupaten Tapanuli Tengah",
                "Kabupaten Tapanuli Selatan", "Kabupaten Toba", "Kabupaten Mandailing Natal", "Kabupaten Nias",
                "Kabupaten Dairi", "Kabupaten Batubara", "Kabupaten Serdang Bedagai", "Kabupaten Humbang Hasundutan"
        ));

        KOTA_MAP.put("Sumatera Barat", Arrays.asList(
                "Kota Padang", "Kota Bukittinggi", "Kota Payakumbuh", "Kota Pariaman",
                "Kota Solok", "Kota Padang Panjang", "Kota Sawahlunto", "Kabupaten Agam",
                "Kabupaten Tanah Datar", "Kabupaten Lima Puluh Kota", "Kabupaten Pasaman",
                "Kabupaten Pasaman Barat", "Kabupaten Pesisir Selatan", "Kabupaten Sijunjung",
                "Kabupaten Dharmasraya", "Kabupaten Solok Selatan", "Kabupaten Kepulauan Mentawai"
        ));

        KOTA_MAP.put("Riau", Arrays.asList(
                "Kota Pekanbaru", "Kota Dumai", "Kabupaten Kampar", "Kabupaten Siak",
                "Kabupaten Pelalawan", "Kabupaten Bengkalis", "Kabupaten Indragiri Hulu",
                "Kabupaten Indragiri Hilir", "Kabupaten Rokan Hulu", "Kabupaten Rokan Hilir",
                "Kabupaten Kuantan Singingi", "Kabupaten Kepulauan Meranti"
        ));

        KOTA_MAP.put("Kepulauan Riau", Arrays.asList(
                "Kota Batam", "Kota Tanjungpinang", "Kabupaten Bintan", "Kabupaten Karimun",
                "Kabupaten Natuna", "Kabupaten Lingga", "Kabupaten Kepulauan Anambas"
        ));

        KOTA_MAP.put("Jambi", Arrays.asList(
                "Kota Jambi", "Kota Sungai Penuh", "Kabupaten Muaro Jambi", "Kabupaten Batanghari",
                "Kabupaten Bungo", "Kabupaten Tebo", "Kabupaten Merangin", "Kabupaten Sarolangun",
                "Kabupaten Kerinci", "Kabupaten Tanjung Jabung Barat", "Kabupaten Tanjung Jabung Timur"
        ));

        KOTA_MAP.put("Sumatera Selatan", Arrays.asList(
                "Kota Palembang", "Kota Prabumulih", "Kota Lubuklinggau", "Kota Pagar Alam",
                "Kabupaten Ogan Ilir", "Kabupaten Ogan Komering Ilir", "Kabupaten Banyuasin",
                "Kabupaten Muara Enim", "Kabupaten Lahat", "Kabupaten Musi Banyuasin",
                "Kabupaten Musi Rawas", "Kabupaten OKU", "Kabupaten OKU Timur", "Kabupaten OKU Selatan"
        ));

        KOTA_MAP.put("Bangka Belitung", Arrays.asList(
                "Kota Pangkalpinang", "Kabupaten Bangka", "Kabupaten Bangka Barat",
                "Kabupaten Bangka Tengah", "Kabupaten Bangka Selatan", "Kabupaten Belitung", "Kabupaten Belitung Timur"
        ));

        KOTA_MAP.put("Bengkulu", Arrays.asList(
                "Kota Bengkulu", "Kabupaten Bengkulu Utara", "Kabupaten Bengkulu Selatan",
                "Kabupaten Bengkulu Tengah", "Kabupaten Rejang Lebong", "Kabupaten Kepahiang",
                "Kabupaten Lebong", "Kabupaten Seluma", "Kabupaten Kaur", "Kabupaten Mukomuko"
        ));

        KOTA_MAP.put("Lampung", Arrays.asList(
                "Kota Bandar Lampung", "Kota Metro", "Kabupaten Lampung Selatan",
                "Kabupaten Lampung Tengah", "Kabupaten Lampung Utara", "Kabupaten Lampung Timur",
                "Kabupaten Lampung Barat", "Kabupaten Pringsewu", "Kabupaten Pesawaran",
                "Kabupaten Tanggamus", "Kabupaten Tulang Bawang", "Kabupaten Way Kanan", "Kabupaten Pesisir Barat"
        ));

        KOTA_MAP.put("DKI Jakarta", Arrays.asList(
                "Jakarta Selatan", "Jakarta Pusat", "Jakarta Barat", "Jakarta Timur",
                "Jakarta Utara", "Kepulauan Seribu"
        ));

        KOTA_MAP.put("Jawa Barat", Arrays.asList(
                "Kota Bandung", "Kota Bogor", "Kota Depok", "Kota Bekasi", "Kota Cimahi",
                "Kota Cirebon", "Kota Sukabumi", "Kota Tasikmalaya", "Kota Banjar",
                "Kabupaten Bandung", "Kabupaten Bandung Barat", "Kabupaten Bogor",
                "Kabupaten Bekasi", "Kabupaten Cirebon", "Kabupaten Sumedang", "Kabupaten Garut",
                "Kabupaten Cianjur", "Kabupaten Karawang", "Kabupaten Purwakarta", "Kabupaten Subang",
                "Kabupaten Majalengka", "Kabupaten Kuningan", "Kabupaten Indramayu", "Kabupaten Ciamis", "Kabupaten Pangandaran"
        ));

        KOTA_MAP.put("Banten", Arrays.asList(
                "Kota Tangerang", "Kota Tangerang Selatan", "Kota Serang", "Kota Cilegon",
                "Kabupaten Tangerang", "Kabupaten Serang", "Kabupaten Pandeglang", "Kabupaten Lebak"
        ));

        KOTA_MAP.put("Jawa Tengah", Arrays.asList(
                "Kota Semarang", "Kota Surakarta (Solo)", "Kota Magelang", "Kota Salatiga",
                "Kota Pekalongan", "Kota Tegal", "Kabupaten Banyumas (Purwokerto)",
                "Kabupaten Sukoharjo", "Kabupaten Klaten", "Kabupaten Kudus", "Kabupaten Boyolali",
                "Kabupaten Cilacap", "Kabupaten Brebes", "Kabupaten Tegal", "Kabupaten Pemalang",
                "Kabupaten Batang", "Kabupaten Kendal", "Kabupaten Demak", "Kabupaten Jepara",
                "Kabupaten Pati", "Kabupaten Rembang", "Kabupaten Blora", "Kabupaten Grobogan",
                "Kabupaten Sragen", "Kabupaten Karanganyar", "Kabupaten Wonogiri", "Kabupaten Wonosobo",
                "Kabupaten Temanggung", "Kabupaten Purworejo", "Kabupaten Kebumen", "Kabupaten Banjarnegara", "Kabupaten Purbalingga"
        ));

        KOTA_MAP.put("DI Yogyakarta", Arrays.asList(
                "Kota Yogyakarta", "Kabupaten Sleman", "Kabupaten Bantul",
                "Kabupaten Kulon Progo", "Kabupaten Gunungkidul"
        ));

        KOTA_MAP.put("Jawa Timur", Arrays.asList(
                "Kota Surabaya", "Kota Malang", "Kota Batu", "Kota Kediri", "Kota Blitar",
                "Kota Madiun", "Kota Pasuruan", "Kota Probolinggo", "Kota Mojokerto",
                "Kabupaten Sidoarjo", "Kabupaten Malang", "Kabupaten Jember", "Kabupaten Banyuwangi",
                "Kabupaten Gresik", "Kabupaten Bojonegoro", "Kabupaten Tuban", "Kabupaten Lamongan",
                "Kabupaten Pasuruan", "Kabupaten Mojokerto", "Kabupaten Jombang", "Kabupaten Nganjuk",
                "Kabupaten Madiun", "Kabupaten Magetan", "Kabupaten Ngawi", "Kabupaten Ponorogo",
                "Kabupaten Pacitan", "Kabupaten Trenggalek", "Kabupaten Tulungagung", "Kabupaten Blitar",
                "Kabupaten Kediri", "Kabupaten Lumajang", "Kabupaten Bondowoso", "Kabupaten Situbondo",
                "Kabupaten Probolinggo", "Kabupaten Bangkalan", "Kabupaten Sampang", "Kabupaten Pamekasan", "Kabupaten Sumenep"
        ));

        KOTA_MAP.put("Bali", Arrays.asList(
                "Kota Denpasar", "Kabupaten Badung", "Kabupaten Gianyar", "Kabupaten Tabanan",
                "Kabupaten Buleleng", "Kabupaten Klungkung", "Kabupaten Karangasem", "Kabupaten Bangli", "Kabupaten Jembrana"
        ));

        KOTA_MAP.put("Nusa Tenggara Barat", Arrays.asList(
                "Kota Mataram", "Kota Bima", "Kabupaten Lombok Barat", "Kabupaten Lombok Tengah",
                "Kabupaten Lombok Timur", "Kabupaten Lombok Utara", "Kabupaten Sumbawa",
                "Kabupaten Sumbawa Barat", "Kabupaten Dompu", "Kabupaten Bima"
        ));

        KOTA_MAP.put("Nusa Tenggara Timur", Arrays.asList(
                "Kota Kupang", "Kabupaten Kupang", "Kabupaten Timor Tengah Selatan",
                "Kabupaten Timor Tengah Utara", "Kabupaten Belu", "Kabupaten Alor",
                "Kabupaten Flores Timur", "Kabupaten Sikka", "Kabupaten Ende", "Kabupaten Ngada",
                "Kabupaten Manggarai", "Kabupaten Manggarai Barat (Labuan Bajo)", "Kabupaten Sumba Timur", "Kabupaten Sumba Barat"
        ));

        KOTA_MAP.put("Kalimantan Barat", Arrays.asList(
                "Kota Pontianak", "Kota Singkawang", "Kabupaten Kubu Raya", "Kabupaten Mempawah",
                "Kabupaten Sambas", "Kabupaten Bengkayang", "Kabupaten Landak", "Kabupaten Sanggau",
                "Kabupaten Sekadau", "Kabupaten Sintang", "Kabupaten Kapuas Hulu", "Kabupaten Ketapang", "Kabupaten Kayong Utara"
        ));

        KOTA_MAP.put("Kalimantan Tengah", Arrays.asList(
                "Kota Palangka Raya", "Kabupaten Kotawaringin Timur (Sampit)", "Kabupaten Kotawaringin Barat (Pangkalan Bun)",
                "Kabupaten Kapuas", "Kabupaten Barito Selatan", "Kabupaten Barito Utara", "Kabupaten Murung Raya",
                "Kabupaten Katingan", "Kabupaten Seruyan", "Kabupaten Pulang Pisau", "Kabupaten Sukamara", "Kabupaten Lamandau"
        ));

        KOTA_MAP.put("Kalimantan Selatan", Arrays.asList(
                "Kota Banjarmasin", "Kota Banjarbaru", "Kabupaten Banjar (Martapura)", "Kabupaten Barito Kuala",
                "Kabupaten Tanah Laut", "Kabupaten Tanah Bumbu", "Kabupaten Kotabaru", "Kabupaten Tapin",
                "Kabupaten Hulu Sungai Selatan", "Kabupaten Hulu Sungai Tengah", "Kabupaten Hulu Sungai Utara", "Kabupaten Tabalong"
        ));

        KOTA_MAP.put("Kalimantan Timur", Arrays.asList(
                "Kota Samarinda", "Kota Balikpapan", "Kota Bontang", "Kabupaten Kutai Kartanegara (Tenggarong)",
                "Kabupaten Kutai Timur", "Kabupaten Kutai Barat", "Kabupaten Paser",
                "Kabupaten Penajam Paser Utara (IKN)", "Kabupaten Berau", "Kabupaten Mahakam Ulu"
        ));

        KOTA_MAP.put("Kalimantan Utara", Arrays.asList(
                "Kota Tarakan", "Kabupaten Bulungan", "Kabupaten Nunukan", "Kabupaten Malinau", "Kabupaten Tana Tidung"
        ));

        KOTA_MAP.put("Sulawesi Utara", Arrays.asList(
                "Kota Manado", "Kota Bitung", "Kota Tomohon", "Kota Kotamobagu",
                "Kabupaten Minahasa", "Kabupaten Minahasa Utara", "Kabupaten Minahasa Selatan",
                "Kabupaten Minahasa Tenggara", "Kabupaten Bolaang Mongondow", "Kabupaten Kepulauan Sangihe", "Kabupaten Kepulauan Talaud"
        ));

        KOTA_MAP.put("Gorontalo", Arrays.asList(
                "Kota Gorontalo", "Kabupaten Gorontalo", "Kabupaten Bone Bolango",
                "Kabupaten Pohuwato", "Kabupaten Boalemo", "Kabupaten Gorontalo Utara"
        ));

        KOTA_MAP.put("Sulawesi Tengah", Arrays.asList(
                "Kota Palu", "Kabupaten Donggala", "Kabupaten Sigi", "Kabupaten Parigi Moutong",
                "Kabupaten Poso", "Kabupaten Tolitoli", "Kabupaten Buol", "Kabupaten Morowali",
                "Kabupaten Morowali Utara", "Kabupaten Banggai", "Kabupaten Banggai Kepulauan", "Kabupaten Banggai Laut"
        ));

        KOTA_MAP.put("Sulawesi Barat", Arrays.asList(
                "Kabupaten Mamuju", "Kabupaten Majene", "Kabupaten Polewali Mandar",
                "Kabupaten Mamasa", "Kabupaten Pasangkayu", "Kabupaten Mamuju Tengah"
        ));

        KOTA_MAP.put("Sulawesi Selatan", Arrays.asList(
                "Kota Makassar", "Kota Parepare", "Kota Palopo", "Kabupaten Gowa",
                "Kabupaten Maros", "Kabupaten Bone", "Kabupaten Wajo", "Kabupaten Soppeng",
                "Kabupaten Sidrap", "Kabupaten Pinrang", "Kabupaten Barru", "Kabupaten Pangkep",
                "Kabupaten Takalar", "Kabupaten Jeneponto", "Kabupaten Bantaeng", "Kabupaten Bulukumba",
                "Kabupaten Sinjai", "Kabupaten Luwu", "Kabupaten Luwu Utara", "Kabupaten Luwu Timur",
                "Kabupaten Tana Toraja", "Kabupaten Toraja Utara", "Kabupaten Kepulauan Selayar"
        ));

        KOTA_MAP.put("Sulawesi Tenggara", Arrays.asList(
                "Kota Kendari", "Kota Baubau", "Kabupaten Konawe", "Kabupaten Konawe Selatan",
                "Kabupaten Konawe Utara", "Kabupaten Kolaka", "Kabupaten Kolaka Utara",
                "Kabupaten Muna", "Kabupaten Buton", "Kabupaten Wakatobi", "Kabupaten Bombana"
        ));

        KOTA_MAP.put("Maluku", Arrays.asList(
                "Kota Ambon", "Kota Tual", "Kabupaten Maluku Tengah", "Kabupaten Seram Bagian Barat",
                "Kabupaten Seram Bagian Timur", "Kabupaten Buru", "Kabupaten Maluku Tenggara",
                "Kabupaten Kepulauan Tanimbar", "Kabupaten Maluku Barat Daya", "Kabupaten Kepulauan Aru"
        ));

        KOTA_MAP.put("Maluku Utara", Arrays.asList(
                "Kota Ternate", "Kota Tidore Kepulauan", "Kabupaten Halmahera Utara",
                "Kabupaten Halmahera Selatan", "Kabupaten Halmahera Barat", "Kabupaten Halmahera Timur",
                "Kabupaten Halmahera Tengah", "Kabupaten Kepulauan Sula", "Kabupaten Pulau Morotai", "Kabupaten Pulau Taliabu"
        ));

        KOTA_MAP.put("Papua", Arrays.asList(
                "Kota Jayapura", "Kabupaten Jayapura", "Kabupaten Keerom", "Kabupaten Sarmi",
                "Kabupaten Mamberamo Raya", "Kabupaten Biak Numfor", "Kabupaten Supiori",
                "Kabupaten Kepulauan Yapen", "Kabupaten Waropen"
        ));

        KOTA_MAP.put("Papua Barat", Arrays.asList(
                "Kota Manokwari", "Kabupaten Manokwari Selatan", "Kabupaten Pegunungan Arfak",
                "Kabupaten Teluk Bintuni", "Kabupaten Teluk Wondama", "Kabupaten Fakfak", "Kabupaten Kaimana"
        ));

        KOTA_MAP.put("Papua Tengah", Arrays.asList(
                "Kabupaten Nabire", "Kabupaten Mimika (Timika)", "Kabupaten Paniai",
                "Kabupaten Puncak Jaya", "Kabupaten Puncak", "Kabupaten Dogiyai", "Kabupaten Intan Jaya", "Kabupaten Deiyai"
        ));

        KOTA_MAP.put("Papua Pegunungan", Arrays.asList(
                "Kabupaten Jayawijaya (Wamena)", "Kabupaten Pegunungan Bintang", "Kabupaten Yahukimo",
                "Kabupaten Tolikara", "Kabupaten Lanny Jaya", "Kabupaten Nduga", "Kabupaten Yalimo", "Kabupaten Mamberamo Tengah"
        ));

        KOTA_MAP.put("Papua Selatan", Arrays.asList(
                "Kabupaten Merauke", "Kabupaten Boven Digoel", "Kabupaten Mappi", "Kabupaten Asmat"
        ));

        KOTA_MAP.put("Papua Barat Daya", Arrays.asList(
                "Kota Sorong", "Kabupaten Sorong", "Kabupaten Sorong Selatan",
                "Kabupaten Raja Ampat", "Kabupaten Tambrauw", "Kabupaten Maybrat"
        ));

        // =========================================================================
        // 2. KECAMATAN REKOMENDASI DI KOTA-KOTA UTAMA
        // =========================================================================

        KECAMATAN_MAP.put("Kota Pekanbaru", Arrays.asList(
                "Bukit Raya", "Marpoyan Damai", "Tampan / Binawidya", "Tuah Madani",
                "Sukajadi", "Payung Sekaki", "Tenayan Raya", "Kulim",
                "Rumbai", "Rumbai Barat", "Rumbai Timur", "Sail",
                "Pekanbaru Kota", "Senapelan", "Lima Puluh"
        ));

        KECAMATAN_MAP.put("Jakarta Selatan", Arrays.asList(
                "Setiabudi", "Tebet", "Kebayoran Baru", "Kebayoran Lama",
                "Cilandak", "Pasar Minggu", "Pancoran", "Mampang Prapatan",
                "Pesanggrahan", "Jagakarsa"
        ));

        KECAMATAN_MAP.put("Jakarta Pusat", Arrays.asList(
                "Gambir", "Tanah Abang", "Menteng", "Senen",
                "Cempaka Putih", "Johar Baru", "Kemayoran", "Sawah Besar"
        ));

        KECAMATAN_MAP.put("Kabupaten Sleman", Arrays.asList(
                "Depok", "Ngaglik", "Mlati", "Gamping", "Kalasan", "Berbah", "Seyegan"
        ));

        KECAMATAN_MAP.put("Kota Bandung", Arrays.asList(
                "Coblong", "Sukasari", "Cidadap", "Sumur Bandung", "Lengkong",
                "Cicendo", "Batu Nunggal", "Kiaracondong", "Regol"
        ));

        KECAMATAN_MAP.put("Kota Malang", Arrays.asList(
                "Lowokwaru", "Klojen", "Blimbing", "Sukun", "Kedungkandang"
        ));

        KECAMATAN_MAP.put("Kota Surabaya", Arrays.asList(
                "Gubeng", "Sukolilo", "Wonokromo", "Rungkut", "Tegalsari", "Genteng", "Mulyorejo"
        ));

        KECAMATAN_MAP.put("Kota Tangerang Selatan", Arrays.asList(
                "Ciputat", "Ciputat Timur", "Pamulang", "Serpong", "Serpong Utara", "Pondok Aren", "Setu"
        ));

        KECAMATAN_MAP.put("Kota Padang", Arrays.asList(
                "Padang Barat", "Padang Timur", "Padang Utara", "Padang Selatan", "Kuranji", "Lubuk Begalung", "Koto Tangah"
        ));

        KECAMATAN_MAP.put("Kota Medan", Arrays.asList(
                "Medan Kota", "Medan Baru", "Medan Sunggal", "Medan Selayang", "Medan Helvetia", "Medan Petisah", "Medan Amplas"
        ));

        KECAMATAN_MAP.put("Kota Makassar", Arrays.asList(
                "Tamalanrea", "Panakkukang", "Rappocini", "Ujung Pandang", "Makassar", "Bontoala", "Mariso"
        ));

        KECAMATAN_MAP.put("Kota Denpasar", Arrays.asList(
                "Denpasar Selatan", "Denpasar Barat", "Denpasar Utara", "Denpasar Timur"
        ));

        KECAMATAN_MAP.put("Kota Semarang", Arrays.asList(
                "Semarang Barat", "Semarang Selatan", "Semarang Tengah", "Semarang Timur", "Semarang Utara", "Banyumanik", "Tembalang", "Pedurungan"
        ));

        KECAMATAN_MAP.put("Kota Surakarta (Solo)", Arrays.asList(
                "Banjarsari", "Jebres", "Laweyan", "Pasar Kliwon", "Serengan"
        ));

        // =========================================================================
        // 3. KELURAHAN / DESA REKOMENDASI DI KECAMATAN POPULER
        // =========================================================================

        KELURAHAN_MAP.put("Bukit Raya", Arrays.asList(
                "Simpang Tiga", "Tangkerang Selatan", "Tangkerang Utara", "Tangkerang Labuai"
        ));

        KELURAHAN_MAP.put("Marpoyan Damai", Arrays.asList(
                "Sidomulyo Timur", "Tangkerang Barat", "Tangkerang Tengah", "Wonorejo", "Maharatu", "Perhentian Marpoyan"
        ));

        KELURAHAN_MAP.put("Tampan / Binawidya", Arrays.asList(
                "Tuah Karya", "Simpang Baru", "Binawidya", "Delima", "Tobek Godang"
        ));

        KELURAHAN_MAP.put("Tuah Madani", Arrays.asList(
                "Tuah Madani", "Tuah Karya", "Sialang Munggu", "Air Putih"
        ));

        KELURAHAN_MAP.put("Sukajadi", Arrays.asList(
                "Kampung Melayu", "Jadirejo", "Pulau Karam", "Sukajadi", "Kedungsari"
        ));

        KELURAHAN_MAP.put("Setiabudi", Arrays.asList(
                "Karet", "Karet Semanggi", "Karet Kuningan", "Kuningan Timur", "Menteng Atas", "Pasar Manggis", "Guntur"
        ));

        KELURAHAN_MAP.put("Depok", Arrays.asList(
                "Caturtunggal", "Maguwoharjo", "Condongcatur"
        ));

        KELURAHAN_MAP.put("Coblong", Arrays.asList(
                "Dago", "Lebak Siliwangi", "Sadang Serang", "Sekeloa"
        ));

        KELURAHAN_MAP.put("Lowokwaru", Arrays.asList(
                "Ketawanggede", "Jatimulyo", "Dinoyo", "Mojolangu", "Tlogomas", "Sumbersari"
        ));

        // =========================================================================
        // 4. SARAN NAMA JALAN POPULER UNTUK AUTOCOMPLETE
        // =========================================================================

        COMMON_STREETS.addAll(Arrays.asList(
                "Jl. Taman Sari",
                "Jl. HR Soebrantas",
                "Jl. Jenderal Sudirman",
                "Jl. Tuanku Tambusai",
                "Jl. Arifin Achmad",
                "Jl. Kaharuddin Nasution",
                "Jl. Riau",
                "Jl. Garuda Sakti",
                "Jl. Paus",
                "Jl. Delima",
                "Jl. Durian",
                "Jl. KH Ahmad Dahlan",
                "Jl. Diponegoro",
                "Jl. Gajah Mada",
                "Jl. Melati",
                "Jl. Cempaka",
                "Jl. Mawar",
                "Jl. Anggrek",
                "Jl. Kenanga",
                "Jl. Kaliurang",
                "Jl. Gejayan (Affandi)",
                "Jl. Dago (Ir. H. Djuanda)",
                "Jl. Margonda Raya",
                "Jl. Fatmawati",
                "Jl. Rasuna Said",
                "Jl. Gatot Subroto",
                "Jl. M.H. Thamrin",
                "Jl. Ahmad Yani",
                "Jl. Pahlawan",
                "Jl. Veteran",
                "Jl. Pemuda"
        ));
    }

    public static List<String> getProvinsiList() {
        return new ArrayList<>(PROVINSI_LIST);
    }

    public static List<String> getKotaList(String provinsi) {
        if (provinsi == null) return Collections.emptyList();
        List<String> list = KOTA_MAP.get(provinsi.trim());
        if (list != null) return new ArrayList<>(list);

        // Cari pencocokan parsial jika ada perbedaan penamaan
        for (Map.Entry<String, List<String>> entry : KOTA_MAP.entrySet()) {
            if (provinsi.trim().equalsIgnoreCase(entry.getKey())
                    || provinsi.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return new ArrayList<>(entry.getValue());
            }
        }

        return Collections.emptyList();
    }

    public static List<String> getKecamatanList(String kota) {
        if (kota == null) return Collections.emptyList();
        List<String> list = KECAMATAN_MAP.get(kota.trim());
        if (list != null) return new ArrayList<>(list);

        // Cek jika mengandung kata kunci tertentu
        for (Map.Entry<String, List<String>> entry : KECAMATAN_MAP.entrySet()) {
            if (kota.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return new ArrayList<>(entry.getValue());
            }
        }

        return Collections.emptyList();
    }

    public static List<String> getKelurahanList(String kecamatan) {
        if (kecamatan == null) return Collections.emptyList();
        List<String> list = KELURAHAN_MAP.get(kecamatan.trim());
        if (list != null) return new ArrayList<>(list);

        // Cek jika mengandung kata kunci tertentu
        for (Map.Entry<String, List<String>> entry : KELURAHAN_MAP.entrySet()) {
            if (kecamatan.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return new ArrayList<>(entry.getValue());
            }
        }

        return Collections.emptyList();
    }

    public static List<String> getCommonStreets() {
        return new ArrayList<>(COMMON_STREETS);
    }

    public static List<String> searchStreets(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getCommonStreets();
        }
        String q = query.trim().toLowerCase();
        List<String> result = new ArrayList<>();
        for (String street : COMMON_STREETS) {
            if (street.toLowerCase().contains(q)) {
                result.add(street);
            }
        }
        if (result.isEmpty()) {
            result.add(query.trim());
        }
        return result;
    }
}
