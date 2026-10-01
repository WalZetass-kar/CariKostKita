# CariKostKita

CariKostKita adalah aplikasi pencarian kost berbasis **Android (Mobile Native)** menggunakan bahasa **Java** untuk membantu mahasiswa, pekerja, dan pendatang menemukan informasi kost di Pekanbaru. Fokus awal pengembangan adalah Kecamatan Bukit Raya.

## Fitur Utama Mobile
- Authentication (Register, Login, Session Management via SharedPreferences)
- Bottom Navigation (Home, Cari/Filter, Favorit, Profil)
- Search kost (Nama, jalan, kelurahan, kecamatan)
- Filter interaktif berbasis Bottom Sheet (Harga, tipe kost putra/putri/campur, fasilitas, ketersediaan)
- Detail kost informatif dengan Image Slider / Gallery
- Direct Action: Hubungi Pemilik via WhatsApp Intent
- Direct Action: Buka Navigasi Rute via Google Maps Intent
- Simpan & kelola Kost Favorit (offline & online sync)
- Role Admin Mobile: Dashboard statistik cepat, CRUD kost, kelola fasilitas & foto kost langsung dari smartphone

## Tech Stack
- **Platform**: Android OS (Mobile)
- **Language**: Java (Android SDK)
- **Build System**: Gradle (Android Gradle Plugin)
- **Arsitektur**: MVVM / MVC + Repository Pattern
- **UI Framework**: Android View System (XML Layouts) + Material Design 3 (Material Components for Android)
- **Database**:
  - Lokal: SQLite / Room Database (Caching & Offline Favorit)
  - Backend/Server: MySQL Database (`carikostkita_db`) via REST API / JDBC Service
- **Libraries**: ViewBinding, Retrofit / Volley (Networking), Glide / Picasso (Image Loading)

## Setup & Menjalankan Aplikasi
1. Buka project di Android Studio.
2. Pastikan Android SDK (API 24 s/d 34) dan JDK 17 terkonfigurasi.
3. Import database MySQL dengan `database_schema.sql` dan `database_seed.sql` pada server lokal/backend.
4. Sesuaikan konfigurasi baseUrl/koneksi database pada class konfigurasi Android.
5. Sync Gradle dan jalankan pada Android Emulator atau perangkat fisik Android.

## Dokumentasi
- `01_PRD.md` — Product Requirements Document (Mobile)
- `02_SYSTEM_DESIGN.md` — Android System Design & Architecture
- `03_DATABASE_DESIGN.md` — Database & Entity Design (SQLite/Room & MySQL)
- `04_USE_CASE.md` — Mobile Use Case Flow
- `05_CLASS_DIAGRAM.md` — Mobile Class & Component Diagram
- `06_PROJECT_STRUCTURE.md` — Struktur Direktori Android Gradle
- `07_DESIGN.md` — Mobile UI/UX Design System (Material 3 & Soft Card)
- `08_IMPLEMENTATION.md` — Panduan Implementasi Android Java
- `09_TESTING.md` — Rencana Pengujian Mobile (Unit & Instrumentation)
- `10_PROMPT.md` — Master Developer & AI Prompt Guide

## Scope v1.0
Versi 1.0 difokuskan pada platform Android Mobile di Kota Pekanbaru (Kecamatan Bukit Raya). Belum mencakup sistem gateway pembayaran in-app, sistem booking kamar, in-app chat internal (menggunakan direct WhatsApp intent), rating/review, atau tracking GPS real-time pemilik kost.

## Development Principle
Pengembangan dilakukan secara terstruktur:
Database & Entity → DAO/Repository → ViewModel/Controller → Activity/Fragment Layout (XML) → Testing.
