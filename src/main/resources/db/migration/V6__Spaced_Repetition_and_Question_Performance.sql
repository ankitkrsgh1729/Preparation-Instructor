-- Spaced repetition and question performance tracking
-- Phase 2: Spaced Repetition & Smart Question Selection

-- User question performance tracking
CREATE TABLE IF NOT EXISTS user_question_performance (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    topic_id BIGINT NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    total_attempts INTEGER DEFAULT 0,
    correct_attempts INTEGER DEFAULT 0,
    last_attempted_at TIMESTAMP,
    average_response_time_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uqp_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uqp_question_fk FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT uqp_topic_fk FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
    UNIQUE(user_id, question_id)
);

-- Spaced repetition tracking (SM-2 algorithm)
CREATE TABLE IF NOT EXISTS spaced_repetition_data (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    repetition_number INTEGER DEFAULT 0,
    ease_factor DECIMAL(4,2) DEFAULT 2.5,
    interval_days INTEGER DEFAULT 0,
    next_review_date TIMESTAMP NOT NULL,
    last_review_date TIMESTAMP,
    consecutive_correct INTEGER DEFAULT 0,
    consecutive_incorrect INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT srd_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT srd_question_fk FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE(user_id, question_id)
);

-- Session momentum tracking
CREATE TABLE IF NOT EXISTS session_momentum (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    momentum_score DECIMAL(5,2) DEFAULT 0.0,
    response_time_trend DECIMAL(5,2) DEFAULT 0.0,
    accuracy_trend DECIMAL(5,2) DEFAULT 0.0,
    questions_answered INTEGER DEFAULT 0,
    correct_answers INTEGER DEFAULT 0,
    average_response_time_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sm_session_fk FOREIGN KEY (session_id) REFERENCES quiz_sessions(id) ON DELETE CASCADE,
    CONSTRAINT sm_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Session analytics for momentum detection
CREATE TABLE IF NOT EXISTS session_analytics (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    response_time_ms BIGINT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sa_session_fk FOREIGN KEY (session_id) REFERENCES quiz_sessions(id) ON DELETE CASCADE,
    CONSTRAINT sa_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT sa_question_fk FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_uqp_user_topic ON user_question_performance(user_id, topic_id);
CREATE INDEX IF NOT EXISTS idx_uqp_user_difficulty ON user_question_performance(user_id, difficulty);
CREATE INDEX IF NOT EXISTS idx_srd_next_review ON spaced_repetition_data(next_review_date);
CREATE INDEX IF NOT EXISTS idx_srd_user_next_review ON spaced_repetition_data(user_id, next_review_date);
CREATE INDEX IF NOT EXISTS idx_sm_session ON session_momentum(session_id);
CREATE INDEX IF NOT EXISTS idx_sa_session_question ON session_analytics(session_id, question_id);

