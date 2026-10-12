-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261022_owner_kost_resubmit_fastapi.sql.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000102', 102),
    ('00000000-0000-0000-0000-000000000103', 103)
ON CONFLICT DO NOTHING;
INSERT INTO public.kosts (id, owner_id, name, status, verification_status)
OVERRIDING SYSTEM VALUE VALUES
    (302, 102, 'Kost Revisi Pemilik', 'TERSEDIA', 'REVISION_REQUIRED'),
    (303, 101, 'Kost Milik Pengguna Lain', 'TERSEDIA', 'REJECTED');

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000102', false);
SELECT public.resubmit_kost_for_current_app_user(302);
DO $$
BEGIN
    IF (SELECT verification_status::text FROM public.kosts WHERE id = 302) <> 'PENDING' THEN
        RAISE EXCEPTION 'owner resubmission did not return owned kost to PENDING';
    END IF;
    IF (SELECT verification_status::text FROM public.kosts WHERE id = 303) <> 'REJECTED' THEN
        RAISE EXCEPTION 'owner resubmission changed another owner kost';
    END IF;
    BEGIN
        PERFORM public.resubmit_kost_for_current_app_user(302);
        RAISE EXCEPTION 'non-rejected kost was resubmitted twice';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
DO $$
BEGIN
    BEGIN
        PERFORM public.resubmit_kost_for_current_app_user(303);
        RAISE EXCEPTION 'non-owner role resubmitted a kost';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;
RESET ROLE;
