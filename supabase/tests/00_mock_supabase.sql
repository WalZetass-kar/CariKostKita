CREATE ROLE anon NOLOGIN; CREATE ROLE authenticated NOLOGIN;
CREATE SCHEMA auth; CREATE SCHEMA storage;
CREATE TABLE auth.users (id uuid PRIMARY KEY, email text);
CREATE FUNCTION auth.uid() RETURNS uuid LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
CREATE TABLE storage.buckets (id text PRIMARY KEY, name text, public boolean);
CREATE TABLE storage.objects (id serial PRIMARY KEY, bucket_id text, name text);
CREATE FUNCTION storage.foldername(name text) RETURNS text[] LANGUAGE sql AS
$$ SELECT (string_to_array(name, '/'))[1:array_length(string_to_array(name, '/'),1)-1] $$;
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;
CREATE PUBLICATION supabase_realtime;

CREATE TABLE public.users (id uuid PRIMARY KEY, nama text, email text, role text DEFAULT 'user',
  no_hp text, avatar_url text, bio text, verification_status text, pengajuan_catatan text,
  catatan_revisi text, is_active boolean DEFAULT true, auth_provider text, created_at timestamptz DEFAULT now());
CREATE TABLE public.kosts (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), owner_id uuid, nama_kost text, alamat text,
  patokan text, harga numeric, tipe_kost text, deskripsi text, no_whatsapp text, latitude float8, longitude float8,
  status text DEFAULT 'TERSEDIA', verification_status text DEFAULT 'PENDING', catatan_revisi text, provinsi text, kota text,
  kecamatan text, kelurahan text, ukuran_kamar text, total_kamar int, kamar_tersedia int, thumbnail_url text,
  image_urls text[], fasilitas text[], created_at timestamptz DEFAULT now(), updated_at timestamptz DEFAULT now());
CREATE TABLE public.favorites (id serial PRIMARY KEY, user_id uuid, kost_id uuid, created_at timestamptz DEFAULT now());
CREATE TABLE public.chats (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), kost_id uuid, pencari_id uuid, owner_id uuid,
  nama_kost text, thumbnail_url text, last_message text, last_message_at timestamptz, unread_pencari int DEFAULT 0,
  unread_owner int DEFAULT 0, created_at timestamptz DEFAULT now());
CREATE TABLE public.messages (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), chat_id uuid, sender_id uuid, message text,
  is_read boolean DEFAULT false, created_at timestamptz DEFAULT now());
CREATE TABLE public.activity_logs (id serial PRIMARY KEY, user_id text, actor_name text, action_type text, description text,
  target_type text, target_id text, created_at timestamptz DEFAULT now());
CREATE TABLE public.reports (id serial PRIMARY KEY, kost_id uuid, reporter_id uuid, owner_id uuid, kategori_laporan text,
  deskripsi text, status text, tindakan_admin text, created_at timestamptz DEFAULT now(), updated_at timestamptz DEFAULT now());

GRANT USAGE ON SCHEMA public, auth, storage TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON storage.objects, storage.buckets TO authenticated, anon;
GRANT ALL ON SEQUENCE storage.objects_id_seq TO authenticated;
GRANT EXECUTE ON FUNCTION auth.uid() TO anon, authenticated;
