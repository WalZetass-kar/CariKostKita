-- CariKostKita production identity bridge: Supabase Auth UUID -> legacy bigint users.id.
--
-- IMPORTANT:
--   * This migration deliberately does not link accounts by email.
--   * Existing accounts remain unable to access domain rows until an operator
--     verifies each Auth UUID <-> users.id pair and inserts it in auth_user_links.
--   * Applying this replaces RLS policies on users, kosts, favorites, chats,
--     messages, reports, reviews, user_reports, survey_requests and user_blocks. Review the policy block before applying in production.
--   * Do not run the older UUID-based migrations against the bigint production
--     schema; this migration is the corrective path for the verified schema.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
    id_default text;
    id_identity text;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('users', 'id'),
            ('kosts', 'id'), ('kosts', 'owner_id'),
            ('favorites', 'id'), ('favorites', 'user_id'), ('favorites', 'kost_id'),
            ('chats', 'id'), ('chats', 'kost_id'), ('chats', 'user_id'), ('chats', 'owner_id'),
            ('messages', 'id'), ('messages', 'chat_id'), ('messages', 'sender_id'),
            ('reports', 'id'), ('reports', 'kost_id'), ('reports', 'reporter_id'),
            ('reviews', 'id'), ('reviews', 'kost_id'), ('reviews', 'user_id'),
            ('user_reports', 'reporter_id'), ('user_reports', 'reported_user_id'),
            ('survey_requests', 'id'), ('survey_requests', 'kost_id'),
            ('survey_requests', 'owner_id'), ('survey_requests', 'pencari_id'),
            ('user_blocks', 'id'), ('user_blocks', 'blocker_id'), ('user_blocks', 'blocked_id')
        ) AS required(table_name, column_name)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = item.table_name
          AND column_name = item.column_name;

        IF actual_type IS DISTINCT FROM 'bigint' THEN
            RAISE EXCEPTION 'Identity bridge preflight failed: public.%.% must be bigint (found %)',
                item.table_name, item.column_name, COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;

    SELECT column_default, is_identity INTO id_default, id_identity
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'id';

    IF COALESCE(id_identity, 'NO') <> 'YES'
       AND COALESCE(id_default, '') NOT LIKE 'nextval(%' THEN
        RAISE EXCEPTION 'Identity bridge preflight failed: public.users.id must have an identity/sequence default';
    END IF;

    SELECT column_default, is_identity INTO id_default, id_identity
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'user_blocks' AND column_name = 'id';
    IF COALESCE(id_identity, 'NO') <> 'YES'
       AND COALESCE(id_default, '') NOT LIKE 'nextval(%' THEN
        RAISE EXCEPTION 'Identity bridge preflight failed: public.user_blocks.id must have an identity/sequence default';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'email'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'name'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'role'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'avatar_url'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'created_at'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'is_active'
    ) THEN
        RAISE EXCEPTION 'Identity bridge preflight failed: public.users profile columns are incomplete';
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS public.auth_user_links (
    auth_user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    app_user_id bigint NOT NULL UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
    linked_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.auth_user_links ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.auth_user_links FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.current_app_user_id()
RETURNS bigint
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT link.app_user_id
    FROM public.auth_user_links AS link
    JOIN public.users AS user_row ON user_row.id = link.app_user_id
    WHERE link.auth_user_id = auth.uid()
      AND COALESCE(user_row.is_active, true)
$$;
REVOKE ALL ON FUNCTION public.current_app_user_id() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.current_app_user_id() TO authenticated;

CREATE OR REPLACE FUNCTION public.current_app_role()
RETURNS text
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT user_row.role::text
    FROM public.users AS user_row
    WHERE user_row.id = public.current_app_user_id()
      AND COALESCE(user_row.is_active, true)
$$;
REVOKE ALL ON FUNCTION public.current_app_role() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.current_app_role() TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.is_app_staff()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT COALESCE(public.current_app_role() IN ('developer', 'admin', 'moderator'), false)
$$;
REVOKE ALL ON FUNCTION public.is_app_staff() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_app_staff() TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.is_app_super_admin()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT COALESCE(public.current_app_role() = 'developer', false)
$$;
REVOKE ALL ON FUNCTION public.is_app_super_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_app_super_admin() TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.provision_app_user(p_name text)
RETURNS bigint
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, auth
AS $$
DECLARE
    caller_id uuid := auth.uid();
    caller_email text;
    caller_confirmed_at timestamptz;
    app_id bigint;
    app_is_active boolean;
BEGIN
    IF caller_id IS NULL THEN
        RAISE EXCEPTION 'authentication required' USING ERRCODE = '42501';
    END IF;

    SELECT link.app_user_id, COALESCE(user_row.is_active, true)
    INTO app_id, app_is_active
    FROM public.auth_user_links AS link
    JOIN public.users AS user_row ON user_row.id = link.app_user_id
    WHERE link.auth_user_id = caller_id;
    IF app_id IS NOT NULL THEN
        IF NOT app_is_active THEN
            RAISE EXCEPTION 'account is inactive' USING ERRCODE = '55000';
        END IF;
        RETURN app_id;
    END IF;

    SELECT auth_user.email, auth_user.email_confirmed_at
    INTO caller_email, caller_confirmed_at
    FROM auth.users AS auth_user
    WHERE auth_user.id = caller_id;

    IF caller_email IS NULL OR caller_confirmed_at IS NULL THEN
        RAISE EXCEPTION 'verified account link required' USING ERRCODE = '42501';
    END IF;

    -- Serialize concurrent signup/provision calls for the same verified email.
    PERFORM pg_advisory_xact_lock(hashtextextended(lower(caller_email), 0));

    SELECT link.app_user_id, COALESCE(user_row.is_active, true)
    INTO app_id, app_is_active
    FROM public.auth_user_links AS link
    JOIN public.users AS user_row ON user_row.id = link.app_user_id
    WHERE link.auth_user_id = caller_id;
    IF app_id IS NOT NULL THEN
        IF NOT app_is_active THEN
            RAISE EXCEPTION 'account is inactive' USING ERRCODE = '55000';
        END IF;
        RETURN app_id;
    END IF;

    -- Existing legacy users must be linked by a human-verified ID pair, never guessed by email.
    IF EXISTS (
        SELECT 1 FROM public.users AS user_row
        WHERE lower(user_row.email) = lower(caller_email)
    ) THEN
        RAISE EXCEPTION 'manual identity link required' USING ERRCODE = '42501';
    END IF;

    INSERT INTO public.users (email, name, role, is_active)
    VALUES (
        caller_email,
        LEFT(COALESCE(NULLIF(BTRIM(p_name), ''), SPLIT_PART(caller_email, '@', 1), 'Pengguna'), 100),
        'user',
        true
    )
    RETURNING id INTO app_id;

    INSERT INTO public.auth_user_links (auth_user_id, app_user_id)
    VALUES (caller_id, app_id);

    RETURN app_id;
END;
$$;
REVOKE ALL ON FUNCTION public.provision_app_user(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.provision_app_user(text) TO authenticated;

-- Public owner cards expose only display fields and only while an active,
-- approved listing exists. Contact details and email are never returned.
CREATE OR REPLACE FUNCTION public.get_public_kost_owner(p_owner_id bigint)
RETURNS TABLE (id bigint, name text, role text, avatar_url text, created_at timestamptz)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT user_row.id, user_row.name, user_row.role::text,
           user_row.avatar_url, user_row.created_at
    FROM public.users AS user_row
    WHERE user_row.id = p_owner_id
      AND COALESCE(user_row.is_active, true)
      AND EXISTS (
          SELECT 1 FROM public.kosts AS kost
          WHERE kost.owner_id = user_row.id
            AND kost.verification_status::text = 'APPROVED'
            AND COALESCE(kost.status::text, '') <> 'TIDAK_AKTIF'
      )
$$;
REVOKE ALL ON FUNCTION public.get_public_kost_owner(bigint) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.get_public_kost_owner(bigint) TO anon, authenticated;

-- Replace every pre-existing policy on the verified numeric core tables so an
-- old permissive UUID/legacy policy cannot OR together with these policies.
DO $$
DECLARE
    policy_row record;
BEGIN
    FOR policy_row IN
        SELECT schemaname, tablename, policyname
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename IN ('users', 'kosts', 'favorites', 'chats', 'messages', 'reports', 'reviews', 'user_reports', 'survey_requests', 'user_blocks')
    LOOP
        EXECUTE format('DROP POLICY %I ON %I.%I',
            policy_row.policyname, policy_row.schemaname, policy_row.tablename);
    END LOOP;
END;
$$;

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.kosts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.survey_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_blocks ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.users, public.kosts, public.favorites,
    public.chats, public.messages, public.reports, public.reviews, public.user_reports, public.survey_requests, public.user_blocks FROM anon;
REVOKE ALL ON TABLE public.users, public.kosts, public.favorites,
    public.chats, public.messages, public.reports, public.reviews, public.user_reports, public.survey_requests, public.user_blocks FROM authenticated;
DO $$
DECLARE
    column_row record;
BEGIN
    FOR column_row IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name IN ('users', 'kosts', 'favorites', 'chats', 'messages', 'reports', 'reviews', 'user_reports', 'survey_requests', 'user_blocks')
    LOOP
        EXECUTE format(
            'REVOKE ALL (%I) ON TABLE public.%I FROM PUBLIC, anon, authenticated',
            column_row.column_name, column_row.table_name
        );
    END LOOP;
END;
$$;
GRANT SELECT ON TABLE public.kosts TO anon, authenticated;
GRANT SELECT, UPDATE ON TABLE public.users TO authenticated;
GRANT INSERT, UPDATE, DELETE ON TABLE public.kosts TO authenticated;
GRANT SELECT, INSERT, DELETE ON TABLE public.favorites TO authenticated;
GRANT SELECT, INSERT ON TABLE public.chats TO authenticated;
GRANT SELECT, INSERT ON TABLE public.messages TO authenticated;
GRANT UPDATE (is_read) ON TABLE public.messages TO authenticated;
GRANT SELECT, INSERT, UPDATE ON TABLE public.reports TO authenticated;
GRANT SELECT (blocker_id, blocked_id, created_at) ON TABLE public.user_blocks TO authenticated;
GRANT INSERT (blocker_id, blocked_id) ON TABLE public.user_blocks TO authenticated;
GRANT DELETE ON TABLE public.user_blocks TO authenticated;

DO $$
DECLARE
    table_name text;
    sequence_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['users', 'kosts', 'favorites', 'chats', 'messages', 'reports', 'user_blocks']
    LOOP
        sequence_name := pg_get_serial_sequence(format('public.%I', table_name), 'id');
        IF sequence_name IS NOT NULL THEN
            IF table_name = 'user_blocks' THEN
                EXECUTE format('GRANT USAGE ON SEQUENCE %s TO authenticated', sequence_name);
            ELSE
                EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE %s TO authenticated', sequence_name);
            END IF;
        END IF;
    END LOOP;
END;
$$;

-- Close legacy sequence grants for tables that remain locked until their
-- feature-specific migration installs the minimum required access path.
DO $$
DECLARE table_name text; sequence_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['reviews', 'user_reports', 'survey_requests']
    LOOP
        sequence_name := pg_get_serial_sequence(format('public.%I', table_name), 'id');
        IF sequence_name IS NOT NULL THEN
            EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM PUBLIC, anon, authenticated', sequence_name);
        END IF;
    END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION public.protect_app_user_update()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF public.is_app_super_admin() THEN
        RETURN NEW;
    END IF;

    NEW.id := OLD.id;
    NEW.email := OLD.email;
    NEW.auth_provider := OLD.auth_provider;
    NEW.created_at := OLD.created_at;

    IF NOT public.is_app_staff() THEN
        NEW.role := OLD.role;
        NEW.is_active := OLD.is_active;
        NEW.verification_status := OLD.verification_status;
    ELSIF OLD.role::text IN ('developer', 'admin', 'moderator')
       OR NEW.role::text IN ('developer', 'admin', 'moderator') THEN
        NEW.role := OLD.role;
    END IF;

    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS trg_protect_app_user_update ON public.users;
CREATE TRIGGER trg_protect_app_user_update
BEFORE UPDATE ON public.users
FOR EACH ROW EXECUTE FUNCTION public.protect_app_user_update();

CREATE OR REPLACE FUNCTION public.protect_app_message_update()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public
AS $$
BEGIN
    NEW.id := OLD.id;
    NEW.chat_id := OLD.chat_id;
    NEW.sender_id := OLD.sender_id;
    NEW.message := OLD.message;
    NEW.created_at := OLD.created_at;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS trg_protect_app_message_update ON public.messages;
CREATE TRIGGER trg_protect_app_message_update
BEFORE UPDATE ON public.messages
FOR EACH ROW EXECUTE FUNCTION public.protect_app_message_update();

CREATE OR REPLACE FUNCTION public.update_app_chat_summary()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    UPDATE public.chats
    SET last_message = NEW.message,
        last_message_at = COALESCE(NEW.created_at, now())
    WHERE id = NEW.chat_id;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION public.update_app_chat_summary() FROM PUBLIC, anon, authenticated;
DROP TRIGGER IF EXISTS trg_update_app_chat_summary ON public.messages;
CREATE TRIGGER trg_update_app_chat_summary
AFTER INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION public.update_app_chat_summary();

CREATE POLICY app_users_select ON public.users FOR SELECT TO authenticated
USING (
    id = public.current_app_user_id()
    OR public.is_app_staff()
    OR EXISTS (
        SELECT 1 FROM public.chats AS chat
        WHERE (chat.user_id = public.current_app_user_id() AND chat.owner_id = users.id)
           OR (chat.owner_id = public.current_app_user_id() AND chat.user_id = users.id)
    )
);
CREATE POLICY app_users_update ON public.users FOR UPDATE TO authenticated
USING (id = public.current_app_user_id() OR public.is_app_staff())
WITH CHECK (id = public.current_app_user_id() OR public.is_app_staff());

CREATE POLICY app_kosts_public_select ON public.kosts FOR SELECT TO anon
USING (
    verification_status::text = 'APPROVED'
    AND COALESCE(status::text, '') <> 'TIDAK_AKTIF'
);
CREATE POLICY app_kosts_select ON public.kosts FOR SELECT TO authenticated
USING (
    (verification_status::text = 'APPROVED' AND COALESCE(status::text, '') <> 'TIDAK_AKTIF')
    OR owner_id = public.current_app_user_id()
    OR public.is_app_staff()
);
CREATE POLICY app_kosts_insert ON public.kosts FOR INSERT TO authenticated
WITH CHECK (owner_id = public.current_app_user_id());
CREATE POLICY app_kosts_update ON public.kosts FOR UPDATE TO authenticated
USING (owner_id = public.current_app_user_id() OR public.is_app_staff())
WITH CHECK (owner_id = public.current_app_user_id() OR public.is_app_staff());
CREATE POLICY app_kosts_delete ON public.kosts FOR DELETE TO authenticated
USING (owner_id = public.current_app_user_id() OR public.is_app_staff());

CREATE POLICY app_favorites_select ON public.favorites FOR SELECT TO authenticated
USING (user_id = public.current_app_user_id());
CREATE POLICY app_favorites_insert ON public.favorites FOR INSERT TO authenticated
WITH CHECK (user_id = public.current_app_user_id());
CREATE POLICY app_favorites_delete ON public.favorites FOR DELETE TO authenticated
USING (user_id = public.current_app_user_id());

CREATE POLICY app_chats_select ON public.chats FOR SELECT TO authenticated
USING (
    user_id = public.current_app_user_id()
    OR owner_id = public.current_app_user_id()
    OR public.is_app_staff()
);
CREATE POLICY app_chats_insert ON public.chats FOR INSERT TO authenticated
WITH CHECK (
    user_id = public.current_app_user_id()
    AND user_id <> owner_id
    AND EXISTS (
        SELECT 1 FROM public.kosts AS kost
        WHERE kost.id = chats.kost_id
          AND kost.owner_id = chats.owner_id
          AND kost.verification_status::text = 'APPROVED'
          AND COALESCE(kost.status::text, '') <> 'TIDAK_AKTIF'
    )
);

CREATE POLICY app_messages_select ON public.messages FOR SELECT TO authenticated
USING (
    public.is_app_staff()
    OR EXISTS (
        SELECT 1 FROM public.chats AS chat
        WHERE chat.id = messages.chat_id
          AND (chat.user_id = public.current_app_user_id()
               OR chat.owner_id = public.current_app_user_id())
    )
);
CREATE POLICY app_messages_insert ON public.messages FOR INSERT TO authenticated
WITH CHECK (
    sender_id = public.current_app_user_id()
    AND EXISTS (
        SELECT 1 FROM public.chats AS chat
        WHERE chat.id = messages.chat_id
          AND (chat.user_id = public.current_app_user_id()
               OR chat.owner_id = public.current_app_user_id())
    )
);
CREATE POLICY app_messages_update ON public.messages FOR UPDATE TO authenticated
USING (
    sender_id <> public.current_app_user_id()
    AND
    EXISTS (
        SELECT 1 FROM public.chats AS chat
        WHERE chat.id = messages.chat_id
          AND (chat.user_id = public.current_app_user_id()
               OR chat.owner_id = public.current_app_user_id())
    )
)
WITH CHECK (
    sender_id <> public.current_app_user_id()
    AND
    EXISTS (
        SELECT 1 FROM public.chats AS chat
        WHERE chat.id = messages.chat_id
          AND (chat.user_id = public.current_app_user_id()
               OR chat.owner_id = public.current_app_user_id())
    )
);

CREATE POLICY app_reports_select ON public.reports FOR SELECT TO authenticated
USING (reporter_id = public.current_app_user_id() OR public.is_app_staff());
CREATE POLICY app_reports_insert ON public.reports FOR INSERT TO authenticated
WITH CHECK (reporter_id = public.current_app_user_id());
CREATE POLICY app_reports_update ON public.reports FOR UPDATE TO authenticated
USING (public.is_app_staff())
WITH CHECK (public.is_app_staff());

-- Keep reviews, user reports and surveys closed until their dedicated migrations grant
-- exact columns and install participant policies. This removes old permissive
-- UUID-era policies during the bridge migration itself.

CREATE POLICY app_user_blocks_select ON public.user_blocks FOR SELECT TO authenticated
USING (blocker_id = public.current_app_user_id() OR blocked_id = public.current_app_user_id());
CREATE POLICY app_user_blocks_insert ON public.user_blocks FOR INSERT TO authenticated
WITH CHECK (
    blocker_id = public.current_app_user_id()
    AND blocked_id <> public.current_app_user_id()
);
CREATE POLICY app_user_blocks_delete ON public.user_blocks FOR DELETE TO authenticated
USING (blocker_id = public.current_app_user_id());

COMMIT;
