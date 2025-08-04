-- Create AI model usage tracking table
CREATE TABLE ai_model_usage (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    topic_id BIGINT REFERENCES topics(id),
    user_id BIGINT REFERENCES users(id),
    session_id VARCHAR(255) REFERENCES quiz_sessions(id),
    operation_type VARCHAR(50) NOT NULL,
    model_provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(50) NOT NULL,
    tokens_used INTEGER,
    cost_in_usd DECIMAL(10,6),
    response_time_ms BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_status CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT')),
    CONSTRAINT valid_operation_type CHECK (operation_type IN ('ANSWER_EVALUATION', 'QUESTION_GENERATION'))
);

-- Create index for common queries
CREATE INDEX idx_ai_usage_topic ON ai_model_usage(topic_id);
CREATE INDEX idx_ai_usage_user ON ai_model_usage(user_id);
CREATE INDEX idx_ai_usage_created_at ON ai_model_usage(created_at);
CREATE INDEX idx_ai_usage_model ON ai_model_usage(model_provider, model_name);

-- Create error logging table
CREATE TABLE ai_model_errors (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    usage_id BIGINT REFERENCES ai_model_usage(id) ON DELETE CASCADE,
    error_code VARCHAR(50),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for error lookup
CREATE INDEX idx_ai_errors_usage ON ai_model_errors(usage_id); 