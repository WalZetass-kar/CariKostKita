# Project Structure — CariKostKita Mobile v1.0 (Android Java)

```text
CariKostKita/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/
│       │   │   └── com/carikostkita/
│       │   │       ├── CariKostApp.java               # Application subclass
│       │   │       ├── data/
│       │   │       │   ├── local/
│       │   │       │   │   ├── DatabaseHelper.java    # SQLite OpenHelper
│       │   │       │   │   └── dao/                   # Local DAOs
│       │   │       │   ├── remote/
│       │   │       │   │   ├── ApiClient.java
│       │   │       │   │   └── ApiService.java        # Retrofit / HTTP calls
│       │   │       │   ├── model/                     # Java POJOs / Entities
│       │   │       │   │   ├── User.java
│       │   │       │   │   ├── Kost.java
│       │   │       │   │   ├── Wilayah.java
│       │   │       │   │   ├── Fasilitas.java
│       │   │       │   │   └── FotoKost.java
│       │   │       │   └── repository/
│       │   │       │       ├── KostRepository.java
│       │   │       │       └── UserRepository.java
│       │   │       ├── ui/
│       │   │       │   ├── splash/
│       │   │       │   │   └── SplashActivity.java
│       │   │       │   ├── auth/
│       │   │       │   │   ├── LoginActivity.java
│       │   │       │   │   └── RegisterActivity.java
│       │   │       │   ├── main/
│       │   │       │   │   ├── MainActivity.java      # Bottom Navigation Container
│       │   │       │   │   ├── home/HomeFragment.java
│       │   │       │   │   ├── search/SearchFragment.java
│       │   │       │   │   ├── search/FilterBottomSheetFragment.java
│       │   │       │   │   ├── favorite/FavoriteFragment.java
│       │   │       │   │   └── profile/ProfileFragment.java
│       │   │       │   ├── detail/
│       │   │       │   │   └── DetailKostActivity.java
│       │   │       │   ├── admin/
│       │   │       │   │   ├── AdminMainActivity.java
│       │   │       │   │   ├── AdminKostFormActivity.java
│       │   │       │   │   └── AdminFasilitasActivity.java
│       │   │       │   └── adapter/
│       │   │       │       ├── KostAdapter.java
│       │   │       │       ├── FotoSliderAdapter.java
│       │   │       │       └── FasilitasAdapter.java
│       │   │       └── util/
│       │   │           ├── SessionManager.java        # SharedPreferences wrapper
│       │   │           ├── IntentHelper.java          # WA & Maps intents
│       │   │           └── FormatUtil.java            # Rupiah & phone parser
│       │   └── res/
│       │       ├── drawable/                          # Vector icons & shapes
│       │       ├── layout/                            # XML Screen Layouts
│       │       │   ├── activity_splash.xml
│       │       │   ├── activity_login.xml
│       │       │   ├── activity_register.xml
│       │       │   ├── activity_main.xml
│       │       │   ├── fragment_home.xml
│       │       │   ├── fragment_search.xml
│       │       │   ├── fragment_favorite.xml
│       │       │   ├── fragment_profile.xml
│       │       │   ├── activity_detail_kost.xml
│       │       │   ├── item_kost_card.xml
│       │       │   ├── item_foto_slider.xml
│       │       │   └── bottom_sheet_filter.xml
│       │       ├── menu/
│       │       │   └── bottom_nav_menu.xml
│       │       ├── mipmap/                            # App launcher icons
│       │       └── values/
│       │           ├── colors.xml                     # Brand & Semantic palette
│       │           ├── strings.xml                    # Localized labels
│       │           └── themes.xml                     # Material 3 Theme
│       └── test/                                      # Unit tests
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle                                       # Top-level build file
├── settings.gradle
├── gradle.properties
├── database_schema.sql                                # Backend MySQL schema
├── database_seed.sql                                  # Backend seed data
├── 10_PROMPT.md                                       # Master Developer & AI Prompt
└── README.md
```

## Konvensi Penamaan Android
- **Activity**: Suffix `Activity` (`DetailKostActivity`, `LoginActivity`)
- **Fragment**: Suffix `Fragment` (`HomeFragment`, `SearchFragment`)
- **Adapter**: Suffix `Adapter` (`KostAdapter`, `FotoSliderAdapter`)
- **Layout XML**: Prefix tipe layar snake_case (`activity_login.xml`, `fragment_home.xml`, `item_kost_card.xml`)
- **ID XML**: Snake_case atau camelCase konsisten (`btn_login`, `tv_nama_kost`, `iv_thumbnail`)
- **Color & Dimens**: Snake_case (`color_primary`, `spacing_medium`, `card_corner_radius`)

## Git
Gunakan commit kecil dan jelas:
- `feat: add mobile splash and auth activity`
- `feat: add bottom navigation and kost recycler view`
- `feat: add whatsapp and google maps intent`
- `feat: add filter bottom sheet dialog`
