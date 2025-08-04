-- Create topic_progress table
CREATE TABLE topic_progress (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    last_attempt_date TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    overall_score DOUBLE PRECISION DEFAULT 0.0,
    questions_attempted INTEGER DEFAULT 0,
    questions_correct INTEGER DEFAULT 0,
    CONSTRAINT topic_progress_user_fk FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT topic_progress_topic_fk FOREIGN KEY (topic_id) REFERENCES topics(id),
    CONSTRAINT topic_progress_user_topic_unique UNIQUE (user_id, topic_id)
);

-- Create difficulty_progress table
CREATE TABLE difficulty_progress (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    topic_progress_id BIGINT NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    score DOUBLE PRECISION DEFAULT 0.0,
    questions_attempted INTEGER DEFAULT 0,
    questions_correct INTEGER DEFAULT 0,
    last_attempt_date TIMESTAMP,
    CONSTRAINT difficulty_progress_topic_progress_fk FOREIGN KEY (topic_progress_id) REFERENCES topic_progress(id) ON DELETE CASCADE,
    CONSTRAINT difficulty_progress_topic_difficulty_unique UNIQUE (topic_progress_id, difficulty)
); 