-- Create users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Create topics table
CREATE TABLE topics (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    parent_topic_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_topic_id) REFERENCES topics(id)
);

-- Create quiz_sessions table
CREATE TABLE quiz_sessions (
    id VARCHAR(255) PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    score DOUBLE PRECISION
);

-- Create questions table with string ID
CREATE TABLE questions (
    id VARCHAR(255) PRIMARY KEY,
    topic_id BIGINT,
    quiz_session_id VARCHAR(255),
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    expected_answer TEXT,
    explanation TEXT,
    source_file VARCHAR(255),
    source_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions(id) ON DELETE CASCADE
);

-- Create question_options table
CREATE TABLE question_options (
    question_id VARCHAR(255) NOT NULL,
    option_value TEXT,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Create user_answers table
CREATE TABLE user_answers (
    id VARCHAR(255) PRIMARY KEY,
    quiz_session_id VARCHAR(255) NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    answer TEXT,
    is_correct BOOLEAN,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quiz_session_id) REFERENCES quiz_sessions(id) ON DELETE CASCADE
); 