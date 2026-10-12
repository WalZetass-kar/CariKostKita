-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261020_analytics_events_fastapi.sql.

INSERT INTO public.auth_user_links (auth_user_id, app_user_id)
VALUES
    ('00000000-0000-0000-0000-000000000101', 101),
    ('00000000-0000-0000-0000-000000000110', 110),
    ('00000000-0000-0000-0000-000000000111', 111)
ON CONFLICT DO NOTHING;

SET ROLE anon;
INSERT INTO public.app_events (event, kost_id) VALUES ('search', 'guest');
DO $$
BEGIN
    IF has_table_privilege(current_user, 'public.app_events', 'SELECT')
       OR has_table_privilege(current_user, 'public.app_events', 'UPDATE')
       OR has_table_privilege(current_user, 'public.app_events', 'DELETE') THEN
        RAISE EXCEPTION 'anonymous analytics role received read or mutation privileges';
    END IF;
    BEGIN
        INSERT INTO public.app_events (user_id, event) VALUES (101, 'search');
        RAISE EXCEPTION 'anonymous caller assigned an authenticated user ID';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;
RESET ROLE;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
INSERT INTO public.app_events (user_id, event) VALUES (101, 'view_detail');
DO $$
BEGIN
    BEGIN
        INSERT INTO public.app_events (user_id, event) VALUES (102, 'view_detail');
        RAISE EXCEPTION 'authenticated caller assigned another user ID';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    IF has_table_privilege(current_user, 'public.app_events', 'UPDATE')
       OR has_table_privilege(current_user, 'public.app_events', 'DELETE') THEN
        RAISE EXCEPTION 'authenticated analytics role received mutation privileges';
    END IF;
    IF (SELECT count(*) FROM public.app_events) <> 0 THEN
        RAISE EXCEPTION 'non-staff user unexpectedly read analytics events';
    END IF;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000111', false);
DO $$
BEGIN
    IF (SELECT count(*) FROM public.app_events) <> 0 THEN
        RAISE EXCEPTION 'moderator unexpectedly read admin analytics events';
    END IF;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000110', false);
DO $$
BEGIN
    IF (SELECT count(*) FROM public.app_events) <> 2 THEN
        RAISE EXCEPTION 'staff analytics read did not include guest and authenticated events';
    END IF;
END;
$$;
RESET ROLE;
