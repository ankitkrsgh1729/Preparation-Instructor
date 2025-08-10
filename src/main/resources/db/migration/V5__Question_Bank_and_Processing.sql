-- Drop existing data as allowed by requirements (Phase 1 reset)
TRUNCATE TABLE user_answers RESTART IDENTITY CASCADE;
TRUNCATE TABLE questions RESTART IDENTITY CASCADE;
TRUNCATE TABLE quiz_sessions RESTART IDENTITY CASCADE;
TRUNCATE TABLE topic_progress RESTART IDENTITY CASCADE;
TRUNCATE TABLE difficulty_progress RESTART IDENTITY CASCADE;

-- Question bank
CREATE TABLE IF NOT EXISTS question_bank (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    topic_id BIGINT NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    expected_answer TEXT,
    explanation TEXT,
    source_file VARCHAR(255),
    source_content TEXT,
    content_hash VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT question_bank_topic_fk FOREIGN KEY (topic_id) REFERENCES topics(id)
);

CREATE TABLE IF NOT EXISTS question_bank_options (
    question_bank_id BIGINT NOT NULL,
    option_value TEXT,
    CONSTRAINT question_bank_options_fk FOREIGN KEY (question_bank_id) REFERENCES question_bank(id) ON DELETE CASCADE
);

-- Topic processing history
CREATE TABLE IF NOT EXISTS topic_processing_history (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    topic_id BIGINT NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    questions_generated INTEGER,
    status VARCHAR(50),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tph_topic_fk FOREIGN KEY (topic_id) REFERENCES topics(id)
);

-- Add FK to questions
ALTER TABLE questions ADD COLUMN IF NOT EXISTS question_bank_id BIGINT;
ALTER TABLE questions ADD CONSTRAINT questions_question_bank_fk FOREIGN KEY (question_bank_id) REFERENCES question_bank(id);


