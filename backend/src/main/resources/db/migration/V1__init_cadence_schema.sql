-- ========================================================
-- Cadence Schema Initialization (Phase 1)
-- Anchored to TIMESTAMPTZ with Asia/Kolkata default
-- ========================================================

-- 1. Users table (Stores chronotype and protected leisure windows)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    timezone VARCHAR(64) DEFAULT 'Asia/Kolkata' NOT NULL,
    chronotype_peak_hours_json JSONB,
    protected_leisure_json JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Learning Goals table
CREATE TABLE learning_goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. Topic Cards table (Stores FSRS-4.5 state, intervals and rubrics)
CREATE TABLE topic_cards (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id UUID REFERENCES learning_goals(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    concept_summary TEXT NOT NULL,
    topic_tag VARCHAR(64) NOT NULL,
    stability DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
    difficulty DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
    elapsed_days INTEGER DEFAULT 0 NOT NULL,
    scheduled_days INTEGER DEFAULT 0 NOT NULL,
    reps INTEGER DEFAULT 0 NOT NULL,
    lapses INTEGER DEFAULT 0 NOT NULL,
    state VARCHAR(32) DEFAULT 'NEW' NOT NULL,
    last_review_at TIMESTAMPTZ,
    next_review_at TIMESTAMPTZ,
    emergency_floor_prompt TEXT NOT NULL,
    rubric_json JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_topic_cards_user_next_review ON topic_cards(user_id, next_review_at);
CREATE INDEX idx_topic_cards_topic_tag ON topic_cards(topic_tag);

-- 4. Schedule Constraints table (Weekly recurring and one-off anchors)
CREATE TABLE schedule_constraints (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    constraint_type VARCHAR(64) NOT NULL,
    window_start VARCHAR(8) NOT NULL,
    window_end VARCHAR(8) NOT NULL,
    days_of_week_json JSONB,
    reason VARCHAR(255) NOT NULL,
    is_hard_block BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 5. Time Blocks table (Scheduled learning blocks on laptop/phone)
CREATE TABLE time_blocks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id UUID REFERENCES topic_cards(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    topic_tag VARCHAR(64) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) DEFAULT 'SCHEDULED' NOT NULL,
    device_type VARCHAR(32) DEFAULT 'LAPTOP_DEEP' NOT NULL,
    planned_duration_min INTEGER NOT NULL,
    actual_duration_min INTEGER,
    is_emergency_floor BOOLEAN DEFAULT FALSE NOT NULL,
    warmup_brief_json JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_time_blocks_user_time ON time_blocks(user_id, start_time, end_time);

-- 6. Schedule Proposals table (Two-phase replanning state)
CREATE TABLE schedule_proposals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
    diff_json JSONB,
    tradeoff_cards_json JSONB,
    schedule_version INTEGER DEFAULT 1 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMPTZ
);

-- 7. Topic Duration Stats table (Duration bias calibrator EMA multipliers)
CREATE TABLE topic_duration_stats (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_tag VARCHAR(64) NOT NULL,
    ema_multiplier DOUBLE PRECISION DEFAULT 1.0 NOT NULL,
    sample_count INTEGER DEFAULT 0 NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_topic_tag UNIQUE (user_id, topic_tag)
);

-- 8. Burnout Telemetry table (Flex-time banking & recovery status)
CREATE TABLE burnout_telemetry (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rolling_completion_rate DOUBLE PRECISION DEFAULT 1.0 NOT NULL,
    flex_credits_minutes INTEGER DEFAULT 0 NOT NULL,
    recovery_mode_active BOOLEAN DEFAULT FALSE NOT NULL,
    consecutive_skipped_days INTEGER DEFAULT 0 NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_burnout UNIQUE (user_id)
);
