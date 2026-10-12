-- Execute after identity_bridge_schema.sql and 20261007_auth_identity_bridge.sql.
-- Mirrors the message predicates used by GET /api/v1/me/unread-messages.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000101', 101),
    ('00000000-0000-0000-0000-000000000102', 102),
    ('00000000-0000-0000-0000-000000000103', 103)
ON CONFLICT DO NOTHING;

INSERT INTO public.chats (id, kost_id, user_id, owner_id)
OVERRIDING SYSTEM VALUE VALUES (402, 301, 103, 102);
INSERT INTO public.messages (id, chat_id, sender_id, message, is_read, created_at)
OVERRIDING SYSTEM VALUE VALUES
    (503, 402, 102, 'Pesan privat untuk pengguna lain', false, '2026-10-12T10:01:00Z');
UPDATE public.messages SET created_at = '2026-10-12T09:59:00Z' WHERE id = 501;
UPDATE public.messages SET created_at = '2026-10-12T10:00:00Z' WHERE id = 502;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
DO $$
DECLARE
    unread_count bigint;
    latest_id bigint;
BEGIN
    SELECT count(*) INTO unread_count FROM public.messages
    WHERE is_read = false AND sender_id <> public.current_app_user_id();
    SELECT id INTO latest_id FROM public.messages
    WHERE is_read = false AND sender_id <> public.current_app_user_id()
    ORDER BY created_at DESC LIMIT 1;
    IF unread_count <> 1 OR latest_id <> 502 THEN
        RAISE EXCEPTION 'participant unread summary leaked or miscounted messages: count %, latest %', unread_count, latest_id;
    END IF;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
DO $$
DECLARE
    unread_count bigint;
    latest_id bigint;
BEGIN
    SELECT count(*) INTO unread_count FROM public.messages
    WHERE is_read = false AND sender_id <> public.current_app_user_id();
    SELECT id INTO latest_id FROM public.messages
    WHERE is_read = false AND sender_id <> public.current_app_user_id()
    ORDER BY created_at DESC LIMIT 1;
    IF unread_count <> 1 OR latest_id <> 503 THEN
        RAISE EXCEPTION 'second participant unread summary returned unexpected messages: count %, latest %', unread_count, latest_id;
    END IF;
END;
$$;
RESET ROLE;
