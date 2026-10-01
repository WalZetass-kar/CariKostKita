# Class Diagram & Architecture — CariKostKita Mobile v1.0 (Java)

## 1. Domain Models & Entities (Java POJO / Room Entity)

```text
+-----------------------------------------------------+
|                        User                         |
+-----------------------------------------------------+
| - idUser: int                                       |
| - nama: String                                      |
| - email: String                                     |
| - passwordHash: String                              |
| - role: Role                                        |
| - createdAt: String                                 |
+-----------------------------------------------------+
| + getters & setters                                 |
+-----------------------------------------------------+

+-----------------------------------------------------+
|                        Kost                         |
+-----------------------------------------------------+
| - idKost: int                                       |
| - idWilayah: int                                    |
| - namaKost: String                                  |
| - alamat: String                                    |
| - harga: double                                     |
| - tipeKost: TipeKost                                |
| - deskripsi: String                                 |
| - noWhatsapp: String                                |
| - latitude: double                                  |
| - longitude: double                                 |
| - status: StatusKost                                |
| - kelurahan: String                                 |
| - kecamatan: String                                 |
| - thumbnailPath: String                             |
| - listFasilitas: List<Fasilitas>                    |
| - listFoto: List<FotoKost>                          |
| - isFavorite: boolean                               |
+-----------------------------------------------------+
| + getFormattedHarga(): String                       |
| + getFullAddress(): String                          |
| + getters & setters                                 |
+-----------------------------------------------------+

+-----------------------------------------------------+
|                       Wilayah                       |
+-----------------------------------------------------+
| - idWilayah: int                                    |
| - kecamatan: String                                 |
| - kelurahan: String                                 |
| - kota: String                                      |
+-----------------------------------------------------+

+-----------------------------------------------------+
|                      Fasilitas                      |
+-----------------------------------------------------+
| - idFasilitas: int                                  |
| - namaFasilitas: String                             |
| - iconResId: int                                    |
| - isSelected: boolean                               |
+-----------------------------------------------------+

+-----------------------------------------------------+
|                      FotoKost                       |
+-----------------------------------------------------+
| - idFoto: int                                       |
| - idKost: int                                       |
| - namaFile: String                                  |
| - pathFile: String                                  |
| - isThumbnail: boolean                              |
+-----------------------------------------------------+
```

---

## 2. Enums

```text
enum Role {
    USER, ADMIN
}

enum TipeKost {
    PUTRA, PUTRI, CAMPUR
}

enum StatusKost {
    TERSEDIA, PENUH, TIDAK_AKTIF
}
```

---

## 3. Data Access Object (DAO) & Repository Interfaces

```text
interface UserDAO {
    User findByEmail(String email);
    User findById(int id);
    long insert(User user);
    int update(User user);
}

interface KostDAO {
    List<Kost> findAllActive();
    Kost findById(int id);
    List<Kost> search(String keyword);
    List<Kost> filter(KostFilterCriteria criteria);
    long insert(Kost kost);
    int update(Kost kost);
    int updateStatus(int id, StatusKost status);
    int countByStatus(StatusKost status);
}

interface FavoritDAO {
    boolean isFavorite(int userId, int kostId);
    List<Kost> findFavoritesByUserId(int userId);
    long insertFavorite(int userId, int kostId);
    int deleteFavorite(int userId, int kostId);
}

class KostRepository {
    - kostDAO: KostDAO
    - favoritDAO: FavoritDAO
    + getActiveKostList(Callback<List<Kost>> callback)
    + searchKost(String query, Callback<List<Kost>> callback)
    + filterKost(KostFilterCriteria criteria, Callback<List<Kost>> callback)
    + getKostDetail(int idKost, Callback<Kost> callback)
    + toggleFavorite(int userId, int idKost, Callback<Boolean> callback)
    + saveKost(Kost kost, Callback<Boolean> callback)
}
```

---

## 4. UI Layer: Activities, Fragments, & Adapters

### Activities & Fragments
- `SplashActivity`: Splash screen, inisialisasi session dan pengecekan role.
- `LoginActivity`: Input email & password, tombol login & register.
- `RegisterActivity`: Pendaftaran user baru dengan validasi client-side.
- `MainActivity`: Container utama dengan `BottomNavigationView`:
  - `HomeFragment`: Banner header, horizontal category chips, vertical recommendation list.
  - `SearchFragment`: Floating search bar, trigger Filter Bottom Sheet, hasil pencarian.
  - `FavoriteFragment`: Daftar bookmark kost favorit.
  - `ProfileFragment`: Info profil, aksi ganti akun, logout.
- `DetailKostActivity`: ViewPager foto, chip fasilitas, deskripsi, floating action Hubungi WhatsApp & Buka Maps.
- `FilterBottomSheetDialogFragment`: Bottom sheet filter interaktif (Range harga, Tipe, Fasilitas).
- `AdminMainActivity`: Dashboard metrik mobile dan ringkasan data kost.
- `AdminKostFormActivity`: Form tambah/ubah properti kost dengan integrasi camera/gallery picker.
- `AdminFasilitasActivity`: Pengelolaan fasilitas kost.

### RecyclerView Adapters
- `KostAdapter`: Render kartu kost (`item_kost_card.xml`) dengan info foto, nama, lokasi, harga, chip tipe, dan tombol favorit.
- `FotoSliderAdapter`: Render slider foto di `ViewPager2` (`item_foto_slider.xml`).
- `FasilitasAdapter`: Render grid fasilitas berikon (`item_fasilitas_chip.xml`).
- `AdminKostAdapter`: Render item daftar kost untuk admin dengan tombol edit dan toggle status.

---

## 5. Helper & Utility Classes

```text
SessionManager
- prefs: SharedPreferences
+ createLoginSession(User user)
+ isLoggedIn(): boolean
+ getUserId(): int
+ getUserRole(): Role
+ logoutUser()

IntentHelper
+ openWhatsApp(Context ctx, String noWhatsapp, String message)
+ openGoogleMaps(Context ctx, double lat, double lng, String label)
+ shareKost(Context ctx, Kost kost)

FormatUtil
+ formatRupiah(double amount): String
+ formatPhoneToWhatsApp(String phone): String
+ sanitizeKeyword(String input): String

DatabaseHelper (SQLiteOpenHelper / Room)
+ onCreate(SQLiteDatabase db)
+ onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
```

---

## 6. Dependency Flow Android

```text
View (Activity/Fragment) 
         │ 
         ▼ (User Actions)
Controller / ViewModel / Presenter
         │ 
         ▼ (Background Worker Thread)
Repository
         │ 
   ┌─────┴──────────────┐
   ▼                    ▼
Local DAO (SQLite)    Remote Service (MySQL API)
   │                    │
   └─────────┬──────────┘
             ▼
      Model Objects
             │ (Data Update to UI)
             ▼
     RecyclerView Adapter
```
