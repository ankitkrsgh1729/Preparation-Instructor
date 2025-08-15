# Preparation Instructor

An intelligent quiz system with spaced repetition and adaptive learning capabilities.

## Features

### Phase 1: Question Pre-generation
- Automatic question generation from GitHub content
- Multiple question types (Multiple Choice, True/False, Short Answer, Scenario-based)
- Difficulty-based question categorization
- Content hash tracking for efficient updates

### Phase 2: Spaced Repetition & Smart Question Selection
- **SM-2 Spaced Repetition Algorithm**: Implements the SuperMemo 2 algorithm for optimal review scheduling
- **Individual Question Performance Tracking**: Tracks user performance per question with accuracy and response time
- **Session Momentum Detection**: Real-time detection of user flow state vs struggling
- **Intelligent Question Selection**: Combines spaced repetition with difficulty progression
- **Difficulty Progression Logic**: Ensures users master easier questions before advancing

## Architecture

### Core Services

#### SpacedRepetitionService
- Implements SM-2 algorithm for optimal review intervals
- Tracks ease factors and repetition numbers
- Manages due question scheduling

#### UserQuestionPerformanceService
- Tracks individual question performance
- Calculates accuracy rates and response times
- Determines proficiency levels for difficulty progression

#### SessionMomentumService
- Real-time session performance tracking
- Flow state detection (momentum score calculation)
- Response time and accuracy trend analysis

#### QuestionSelectionService
- Intelligent question selection combining multiple factors
- Prioritizes due spaced repetition questions
- Adapts difficulty based on user performance

### Database Schema

#### New Tables (Phase 2)
- `user_question_performance`: Individual question performance tracking
- `spaced_repetition_data`: SM-2 algorithm data storage
- `session_momentum`: Real-time session performance
- `session_analytics`: Detailed session analytics

## Setup Instructions

1. Clone the repository
```bash
git clone https://github.com/ankitkrsgh1729/Preparation-Instructor.git
cd Preparation-Instructor
```

2. Configure application properties
```bash
# Copy the example properties file
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Edit the properties file and add your OpenAI API key
nano src/main/resources/application.properties
```

3. Build the project
```bash
mvn clean install
```

4. Run the application
```bash
java -jar target/quiz-system-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Quiz Sessions
- `POST /api/sessions/start` - Start a new quiz session
- `POST /api/sessions/{sessionId}/submit` - Submit an answer
- `POST /api/sessions/{sessionId}/end` - End a session

### Spaced Repetition & Performance
- `GET /api/sessions/{sessionId}/momentum` - Get session momentum data
- `GET /api/sessions/due-questions/{topic}` - Get due questions for review
- `GET /api/sessions/recommended-difficulty/{topic}` - Get recommended difficulty level
- `GET /api/sessions/performance/{topic}` - Get topic performance statistics

## Environment Configuration

The application requires several configuration properties to run. These are specified in `application.properties`:

- `openai.api.key`: Your OpenAI API key (required)
- `github.repository.url`: URL of the repository containing study materials
- `github.repository.branch`: Branch to use (default: main)
- Other properties as shown in `application.properties.example`

**Important**: Never commit your `application.properties` file with real API keys. Use the example file as a template and keep your actual configuration local.

## Algorithm Details

### SM-2 Spaced Repetition
- **Initial interval**: 1 day for first repetition
- **Second interval**: 6 days for second repetition
- **Subsequent intervals**: Previous interval × ease factor
- **Ease factor adjustment**: +0.1 for correct, -0.2 for incorrect (minimum 1.3)

### Difficulty Progression
- **Proficiency threshold**: 70% accuracy required to advance
- **Progression order**: EASY → MEDIUM → HARD
- **Adaptive selection**: Easier questions for struggling users, harder for those in flow

### Momentum Detection
- **Flow threshold**: Momentum score ≥ 70
- **Struggling threshold**: Momentum score ≤ 30
- **Score calculation**: 70% accuracy + 30% response time factor

## Testing

### Unit Tests
- `SpacedRepetitionServiceImplTest`: Tests SM-2 algorithm implementation
- `SessionMomentumServiceImplTest`: Tests momentum detection logic

### Integration Tests
- Complete quiz session flow with spaced repetition updates
- Difficulty progression validation
- Session momentum tracking

## Success Metrics

- **Due questions appear within 24 hours** of review date
- **Difficulty progression**: Users advance only after 70% accuracy
- **Session optimization**: Struggling users get shorter, easier sessions
- **Learning efficiency**: Reduced time to mastery through spaced repetition