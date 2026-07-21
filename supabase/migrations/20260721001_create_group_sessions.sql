CREATE TABLE IF NOT EXISTS group_sessions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         TEXT        NOT NULL,
    subtitle      TEXT,
    spot_slug     TEXT        REFERENCES spots(slug) ON DELETE SET NULL,
    created_by    UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS group_session_members (
    session_id  UUID NOT NULL REFERENCES group_sessions(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    role        TEXT NOT NULL DEFAULT 'member',
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (session_id, user_id)
);

ALTER TABLE group_sessions        ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_session_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY "members can view session"
    ON group_sessions FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM group_session_members m
            WHERE m.session_id = id AND m.user_id = auth.uid()
        )
    );

CREATE POLICY "authenticated users can create sessions"
    ON group_sessions FOR INSERT
    WITH CHECK (auth.uid() = created_by);

CREATE POLICY "owner can update session"
    ON group_sessions FOR UPDATE
    USING (created_by = auth.uid());

CREATE POLICY "members can view membership"
    ON group_session_members FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM group_session_members m2
            WHERE m2.session_id = session_id AND m2.user_id = auth.uid()
        )
    );

CREATE POLICY "owner can add members"
    ON group_session_members FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM group_sessions s
            WHERE s.id = session_id AND s.created_by = auth.uid()
        )
    );

CREATE POLICY "members can leave or owner can remove"
    ON group_session_members FOR DELETE
    USING (
        user_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM group_sessions s
            WHERE s.id = session_id AND s.created_by = auth.uid()
        )
    );

CREATE INDEX IF NOT EXISTS idx_group_session_members_user
    ON group_session_members (user_id);

CREATE INDEX IF NOT EXISTS idx_group_sessions_created_by
    ON group_sessions (created_by);

CREATE INDEX IF NOT EXISTS idx_group_sessions_ended_at
    ON group_sessions (ended_at)
    WHERE ended_at IS NULL;
