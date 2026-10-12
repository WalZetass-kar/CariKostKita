-- Run after the identity bridge and 20261015_owner_verification_fastapi.sql.
INSERT INTO public.users (id, email, name, role, is_active)
OVERRIDING SYSTEM VALUE
VALUES (109, 'admin@example.test', 'Admin', 'admin', true);
INSERT INTO auth.users (id, email, email_confirmed_at)
VALUES ('00000000-0000-0000-0000-000000000109', 'admin@example.test', now());
INSERT INTO public.auth_user_links (auth_user_id, app_user_id)
VALUES
    ('00000000-0000-0000-0000-000000000101', 101),
    ('00000000-0000-0000-0000-000000000103', 103),
    ('00000000-0000-0000-0000-000000000109', 109)
ON CONFLICT (auth_user_id) DO NOTHING;
SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);

SELECT public.submit_owner_verification_application(
    'Nama kost: Kost Uji | Alamat: Jalan Uji | WA: +628123456789'
);

DO $$
DECLARE current_status text; application_count bigint; stored_note text;
BEGIN
    SELECT verification_status INTO current_status
    FROM public.users WHERE id = public.current_app_user_id();
    IF current_status IS DISTINCT FROM 'PENDING' THEN
        RAISE EXCEPTION 'owner application did not set pending status';
    END IF;

    SELECT count(*), max(submission_note)
    INTO application_count, stored_note
    FROM public.owner_verification_applications
    WHERE user_id = public.current_app_user_id();
    IF application_count <> 1 OR stored_note NOT LIKE 'Nama kost:%' THEN
        RAISE EXCEPTION 'application note was not stored for the verified caller';
    END IF;

    BEGIN
        PERFORM public.submit_owner_verification_application('Duplicate attempt');
        RAISE EXCEPTION 'duplicate active application unexpectedly succeeded';
    EXCEPTION WHEN unique_violation THEN
        NULL;
    END;

    IF has_table_privilege(current_user, 'public.owner_verification_applications', 'INSERT')
       OR has_table_privilege(current_user, 'public.owner_verification_applications', 'UPDATE') THEN
        RAISE EXCEPTION 'client unexpectedly has direct write access to applications';
    END IF;
    IF has_sequence_privilege(current_user, 'public.owner_verification_applications_id_seq', 'USAGE') THEN
        RAISE EXCEPTION 'client unexpectedly has direct sequence access';
    END IF;
END;
$$;

-- Only a trusted staff identity can approve, and approval promotes the linked user atomically.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000109', false);
SELECT public.review_owner_verification_application(
    (SELECT id FROM public.owner_verification_applications WHERE user_id = 101),
    'APPROVED',
    'Dokumen sesuai'
);
DO $$
DECLARE profile_role text; profile_status text; application_status text; reviewer_id bigint;
BEGIN
    SELECT role, verification_status INTO profile_role, profile_status
    FROM public.users WHERE id = 101;
    SELECT status, reviewed_by INTO application_status, reviewer_id
    FROM public.owner_verification_applications WHERE user_id = 101;
    IF profile_role IS DISTINCT FROM 'owner'
       OR profile_status IS DISTINCT FROM 'APPROVED'
       OR application_status IS DISTINCT FROM 'APPROVED'
       OR reviewer_id IS DISTINCT FROM 109 THEN
        RAISE EXCEPTION 'approval mismatch: role %, status %, app %, reviewer %',
            profile_role, profile_status, application_status, reviewer_id;
    END IF;
    BEGIN
        PERFORM public.review_owner_verification_application(
            (SELECT id FROM public.owner_verification_applications WHERE user_id = 101),
            'REJECTED', 'Second review'
        );
        RAISE EXCEPTION 'reviewed application unexpectedly accepted a second decision';
    EXCEPTION WHEN unique_violation THEN
        NULL;
    END;
END;
$$;

-- A caller-set custom GUC must not bypass the protected profile trigger.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
SELECT set_config('app.owner_verification_rpc', 'on', false);
UPDATE public.users SET verification_status = 'PENDING' WHERE id = 103;
DO $$
BEGIN
    IF (SELECT verification_status FROM public.users WHERE id = 103) IS DISTINCT FROM 'NONE'
       OR (SELECT role FROM public.users WHERE id = 103) IS DISTINCT FROM 'user' THEN
        RAISE EXCEPTION 'caller-set RPC flag bypassed protected user columns';
    END IF;
END;
$$;
RESET ROLE;
DELETE FROM public.auth_user_links
WHERE auth_user_id IN (
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000103',
    '00000000-0000-0000-0000-000000000109'
);
