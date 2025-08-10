# Preparation-Instructor: API & System Overview

## System Overview
Preparation-Instructor is a Spring Boot-based quiz and learning platform that:
- Syncs study notes from a GitHub repository.
- Generates quizzes based on synced topics and user-selected difficulty.
- Tracks user progress and adapts learning cycles.
- Uses OpenAI for question generation and answer evaluation.
- Supports user authentication and role-based access.

### Main Flow
1. **Sync Notes**: Fetches and parses study notes from a configured GitHub repo.
2. **Quiz Generation**: Users select a topic and difficulty; the system generates a quiz session with questions.
3. **Quiz Session**: Users answer questions; answers are evaluated (AI-assisted for open-ended questions).
4. **Progress Tracking**: User progress is tracked per topic and difficulty, with recommendations for next steps.
5. **Authentication**: Users register, login, and manage sessions securely.

---

## API Endpoints

### Auth APIs (`/api/v1/auth`)
- `POST /register` — Register a new user.
- `POST /login` — Login and receive JWT token.
- `POST /logout` — Logout (invalidate token).

### Topic APIs (`/api/topics`)
- `GET /` — List all available topics.
- `GET /availability?topic={topic}` — Get content and question availability for a topic.

### Quiz Session APIs (`/api/sessions`)
- `POST /start` — Start a new quiz session (provide topic, difficulty, question count).
- `POST /{sessionId}/submit` — Submit an answer for a question in a session.
- `POST /{sessionId}/end` — End a quiz session and get results.

### Progress APIs (`/api/progress`)
- `GET /` — Get user progress for all topics.
- `GET /active` — Get active progress (topics in progress).
- `GET /topics/{topicName}` — Get progress for a specific topic.
- `GET /topics/{topicName}/difficulty/{difficulty}` — Get progress for a topic at a specific difficulty.
- `POST /topics/{topicName}/reset` — Reset progress for a topic.

---

## Database Schema (Key Tables)

### users
- id (BIGINT, PK)
- username (VARCHAR, unique)
- email (VARCHAR, unique)
- password (VARCHAR)
- role (VARCHAR)
- enabled, account_non_expired, account_non_locked, credentials_non_expired (BOOLEAN)
- created_at, last_login (TIMESTAMP)

### topics
- id (BIGINT, PK)
- name (VARCHAR, unique)
- description (TEXT)
- parent_topic_id (BIGINT, FK)
- created_at, last_updated (TIMESTAMP)

### quiz_sessions
- id (VARCHAR, PK)
- topic (VARCHAR)
- difficulty (VARCHAR)
- start_time, end_time (TIMESTAMP)
- status (VARCHAR)
- score (DOUBLE)

### questions
- id (VARCHAR, PK)
- topic_id (BIGINT, FK)
- quiz_session_id (VARCHAR, FK)
- question_text (TEXT)
- question_type (VARCHAR)
- difficulty (VARCHAR)
- expected_answer, explanation, source_file, source_content (TEXT/VARCHAR)
- created_at, last_updated (TIMESTAMP)

### question_options
- question_id (VARCHAR, FK)
- option_value (TEXT)

### user_answers
- quiz_session_id (VARCHAR, FK)
- question_id (VARCHAR, FK)
- answer (TEXT)
- is_correct (BOOLEAN)
- answered_at (TIMESTAMP)

### topic_progress
- id (BIGINT, PK)
- user_id (BIGINT, FK)
- topic_id (BIGINT, FK)
- start_date, last_attempt_date (TIMESTAMP)
- is_active (BOOLEAN)
- overall_score (DOUBLE)
- questions_attempted, questions_correct (INTEGER)

### difficulty_progress
- id (BIGINT, PK)
- topic_progress_id (BIGINT, FK)
- difficulty (VARCHAR)
- score (DOUBLE)
- questions_attempted, questions_correct (INTEGER)
- last_attempt_date (TIMESTAMP)

### ai_model_usage
- id (BIGINT, PK)
- topic_id (BIGINT, FK)
- user_id (BIGINT, FK)
- session_id (VARCHAR, FK)
- operation_type, model_provider, model_name, status (VARCHAR)
- tokens_used (INTEGER)
- cost_in_usd (DECIMAL)
- response_time_ms (BIGINT)
- created_at (TIMESTAMP)

### ai_model_errors
- id (BIGINT, PK)
- usage_id (BIGINT, FK)
- error_code (VARCHAR)
- error_message (TEXT)
- created_at (TIMESTAMP)

---

## Notes
- All endpoints require authentication except `/auth/*` and topic listing.
- The system is extensible for new question types, topics, and AI providers.
- Rate limiting is applied to quiz session creation.

---

*For enhancement suggestions, focus on API design, extensibility, AI integration, and user experience improvements.*
