-- Execute after identity_bridge_schema.sql and 20261007_auth_identity_bridge.sql.
-- Apply 20261012_user_reports_fastapi.sql, 20261013_surveys_fastapi.sql,
-- and 20261014_reviews_fastapi.sql before these assertions.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000101', 101),
    ('00000000-0000-0000-0000-000000000102', 102),
    ('00000000-0000-0000-0000-000000000103', 103),
    ('00000000-0000-0000-0000-000000000106', 106);

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);

DO $$
DECLARE
    visible_ids bigint[];
    result_id bigint;
    stored_role text;
    stored_rating smallint;
    stored_name text;
    stored_message text;
BEGIN
    IF public.current_app_user_id() IS DISTINCT FROM 101 THEN
        RAISE EXCEPTION 'Auth UUID did not map to bigint app ID';
    END IF;

    SELECT array_agg(id ORDER BY id) INTO visible_ids FROM public.users;
    IF visible_ids IS DISTINCT FROM ARRAY[101, 102]::bigint[] THEN
        RAISE EXCEPTION 'user RLS exposed the wrong rows: %', visible_ids;
    END IF;

    IF (SELECT count(*) FROM public.chats) <> 1 OR (SELECT count(*) FROM public.messages) <> 2 THEN
        RAISE EXCEPTION 'participant chat/message read failed';
    END IF;

    INSERT INTO public.reports (kost_id, reporter_id, reason, description)
    VALUES (301, 101, 'Informasi tidak akurat', 'Alamat berbeda.');
    IF (SELECT count(*) FROM public.reports) <> 1 THEN
        RAISE EXCEPTION 'reporter could not read their own report';
    END IF;
    BEGIN
        INSERT INTO public.reports (kost_id, reporter_id, reason, description)
        VALUES (301, 103, 'Spam', 'Cross-user report');
        RAISE EXCEPTION 'cross-user report insert unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    INSERT INTO public.user_reports (reporter_id, reported_user_id, reason, details)
    VALUES (101, 102, 'Spam', 'Repeated messages');
    BEGIN
        INSERT INTO public.user_reports (reporter_id, reported_user_id, reason, details)
        VALUES (101, 103, 'Spam', 'No conversation exists');
        RAISE EXCEPTION 'user report without a shared chat unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    BEGIN
        INSERT INTO public.user_reports (reporter_id, reported_user_id, reason)
        VALUES (103, 101, 'Spoofed reporter');
        RAISE EXCEPTION 'cross-user report insert unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    BEGIN
        INSERT INTO public.user_reports (id, reporter_id, reported_user_id, reason)
        VALUES (999, 101, 102, 'Spoofed row ID');
        RAISE EXCEPTION 'client unexpectedly set the user-report row ID';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    IF has_table_privilege(current_user, 'public.user_reports', 'SELECT') THEN
        RAISE EXCEPTION 'user report table unexpectedly grants direct reads';
    END IF;

    INSERT INTO public.survey_requests (kost_id, owner_id, pencari_id, jadwal, catatan, status)
    VALUES (301, 102, 101, now() + interval '2 days', 'Datang sore', 'MENUNGGU');
    IF (SELECT count(*) FROM public.survey_requests WHERE status = 'MENUNGGU') <> 1 THEN
        RAISE EXCEPTION 'seeker cannot read their survey request';
    END IF;
    BEGIN
        INSERT INTO public.survey_requests (kost_id, owner_id, pencari_id, jadwal, status)
        VALUES (301, 102, 101, now() + interval '3 days', 'MENUNGGU');
        RAISE EXCEPTION 'duplicate active survey unexpectedly succeeded';
    EXCEPTION WHEN unique_violation THEN
        NULL;
    END;
    BEGIN
        INSERT INTO public.survey_requests (kost_id, owner_id, pencari_id, jadwal, status)
        VALUES (301, 103, 101, now() + interval '3 days', 'MENUNGGU');
        RAISE EXCEPTION 'survey with spoofed owner unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    SELECT id INTO result_id FROM public.survey_requests WHERE status = 'MENUNGGU';
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000102', false);
    UPDATE public.survey_requests SET status = 'DIKONFIRMASI' WHERE id = result_id;
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
    BEGIN
        UPDATE public.survey_requests SET status = 'SELESAI' WHERE id = result_id;
        RAISE EXCEPTION 'seeker unexpectedly marked survey completed';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000102', false);
    UPDATE public.survey_requests SET status = 'SELESAI' WHERE id = result_id;
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
    IF NOT EXISTS (
        SELECT 1 FROM public.survey_requests
        WHERE id = result_id AND status = 'SELESAI' AND catatan = 'Datang sore'
    ) THEN
        RAISE EXCEPTION 'survey transition or new migrated fields were not preserved';
    END IF;

    IF has_table_privilege(current_user, 'public.reviews', 'INSERT')
       OR has_table_privilege(current_user, 'public.reviews', 'UPDATE') THEN
        RAISE EXCEPTION 'reviews unexpectedly grants direct mutation';
    END IF;
    IF has_column_privilege(current_user, 'public.reviews', 'user_id', 'SELECT') THEN
        RAISE EXCEPTION 'public review readers can access reviewer IDs';
    END IF;
    SELECT rating, nama_user INTO stored_rating, stored_name
    FROM public.submit_app_review(301::bigint, 5::smallint, '  Kost nyaman  '::text);
    IF stored_rating <> 5 OR stored_name <> 'Tenant' THEN
        RAISE EXCEPTION 'completed survey user could not submit a review';
    END IF;
    SELECT rating INTO stored_rating FROM public.submit_app_review(301::bigint, 4::smallint, 'Koreksi ulasan'::text);
    IF stored_rating <> 4
       OR (SELECT count(*) FROM public.reviews WHERE kost_id = 301) <> 1
       OR (SELECT rating_count FROM public.kosts WHERE id = 301) <> 1
       OR (SELECT rating_avg FROM public.kosts WHERE id = 301) <> 4.00 THEN
        RAISE EXCEPTION 'review upsert or rating aggregate failed';
    END IF;
    BEGIN
        INSERT INTO public.reviews (kost_id, user_id, rating) VALUES (301, 103, 5);
        RAISE EXCEPTION 'direct review insert unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
    BEGIN
        PERFORM * FROM public.submit_app_review(301::bigint, 5::smallint, 'Belum survei'::text);
        RAISE EXCEPTION 'user without completed survey unexpectedly reviewed';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);

    INSERT INTO public.user_blocks (blocker_id, blocked_id) VALUES (101, 102);
    IF (SELECT count(*) FROM public.user_blocks) <> 1 THEN
        RAISE EXCEPTION 'blocker could not read their own block';
    END IF;
    BEGIN
        INSERT INTO public.user_blocks (blocker_id, blocked_id) VALUES (103, 101);
        RAISE EXCEPTION 'cross-user block insert unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    BEGIN
        INSERT INTO public.user_blocks (blocker_id, blocked_id) VALUES (101, 101);
        RAISE EXCEPTION 'self-block unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    BEGIN
        INSERT INTO public.user_blocks (id, blocker_id, blocked_id) VALUES (999, 101, 103);
        RAISE EXCEPTION 'client unexpectedly set the block row ID';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000102', false);
    IF (SELECT count(*) FROM public.user_blocks) <> 1 THEN
        RAISE EXCEPTION 'blocked participant could not read the block relation';
    END IF;
    DELETE FROM public.user_blocks WHERE blocker_id = 101 AND blocked_id = 102;
    IF (SELECT count(*) FROM public.user_blocks) <> 1 THEN
        RAISE EXCEPTION 'blocked participant deleted another user block';
    END IF;
    PERFORM set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);

    BEGIN
        INSERT INTO public.survey_requests (kost_id, owner_id, pencari_id, jadwal, status)
        VALUES (301, 102, 101, now() + interval '2 days', 'MENUNGGU');
        RAISE EXCEPTION 'blocked participant pair unexpectedly created a survey';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    INSERT INTO public.favorites (user_id, kost_id) VALUES (101, 301);
    BEGIN
        INSERT INTO public.favorites (user_id, kost_id) VALUES (103, 301);
        RAISE EXCEPTION 'cross-user favorite insert unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    UPDATE public.users SET role = 'developer', is_active = false WHERE id = 101;
    SELECT role INTO stored_role FROM public.users WHERE id = 101;
    IF stored_role <> 'user' THEN
        RAISE EXCEPTION 'self-service update changed a protected role';
    END IF;

    INSERT INTO public.messages (chat_id, sender_id, message) VALUES (401, 101, 'Pesan baru');
    SELECT last_message INTO stored_message FROM public.chats WHERE id = 401;
    IF stored_message <> 'Pesan baru' THEN
        RAISE EXCEPTION 'message trigger did not update the chat summary';
    END IF;

    BEGIN
        UPDATE public.messages SET message = 'Isi diubah' WHERE id = 501;
        RAISE EXCEPTION 'authenticated role unexpectedly changed message content';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    SELECT message INTO stored_message FROM public.messages WHERE id = 501;
    IF stored_message <> 'Pesan awal' THEN
        RAISE EXCEPTION 'message content protection failed';
    END IF;

    UPDATE public.messages SET is_read = true WHERE id = 502;
    IF (SELECT is_read FROM public.messages WHERE id = 502) IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'participant could not mark the inbound message as read';
    END IF;

END;
$$;

-- A genuinely new verified Auth identity may receive a new least-privilege row.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000104', false);
DO $$
DECLARE
    result_id bigint;
BEGIN
    result_id := public.provision_app_user('New Person');
    IF result_id IS DISTINCT FROM 104 OR public.current_app_user_id() IS DISTINCT FROM 104 THEN
        RAISE EXCEPTION 'new-account provisioning did not create/link the expected bigint ID';
    END IF;
    IF (SELECT role FROM public.users WHERE id = result_id) <> 'user' THEN
        RAISE EXCEPTION 'new account did not receive the least-privilege role';
    END IF;
END;
$$;

-- An existing legacy email is never auto-linked to a new Auth UUID.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000105', false);
DO $$
DECLARE
    denied boolean := false;
BEGIN
    BEGIN
        PERFORM public.provision_app_user('Tenant account takeover');
    EXCEPTION WHEN insufficient_privilege THEN
        denied := true;
    END;
    IF NOT denied THEN
        RAISE EXCEPTION 'existing legacy email was automatically linked';
    END IF;
    IF public.current_app_user_id() IS NOT NULL THEN
        RAISE EXCEPTION 'unmapped Auth user received a domain ID';
    END IF;
END;
$$;

-- An existing mapping is not enough to access data when the domain account is disabled.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000106', false);
DO $$
DECLARE
    denied boolean := false;
BEGIN
    IF public.current_app_user_id() IS NOT NULL THEN
        RAISE EXCEPTION 'inactive account received a domain ID';
    END IF;
    BEGIN
        PERFORM public.provision_app_user('Inactive');
    EXCEPTION WHEN SQLSTATE '55000' THEN
        denied := true;
    END;
    IF NOT denied THEN
        RAISE EXCEPTION 'inactive account was provisioned';
    END IF;
END;
$$;

RESET ROLE;

-- Anonymous callers see only the minimal card for an owner with an approved listing.
SET ROLE anon;
DO $$
DECLARE
    owner_name text;
    visible_count integer;
BEGIN
    SELECT card.name INTO owner_name FROM public.get_public_kost_owner(102) AS card;
    IF owner_name <> 'Owner' THEN
        RAISE EXCEPTION 'approved listing owner card was not available to anon';
    END IF;
    SELECT count(*) INTO visible_count FROM public.get_public_kost_owner(103);
    IF visible_count <> 0 THEN
        RAISE EXCEPTION 'owner without an approved listing was exposed';
    END IF;
    IF has_column_privilege('anon', 'public.reviews', 'user_id', 'SELECT') THEN
        RAISE EXCEPTION 'anon role can read private reviewer IDs';
    END IF;
    IF (SELECT count(*) FROM public.reviews WHERE kost_id = 301) <> 1 THEN
        RAISE EXCEPTION 'public review list did not expose the approved listing rating';
    END IF;
    IF (SELECT count(*) FROM public.kosts WHERE id = 301) <> 1 THEN
        RAISE EXCEPTION 'anonymous public kost policy failed';
    END IF;
END;
$$;
RESET ROLE;
