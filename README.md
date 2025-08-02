# Interview Quiz System

An AI-powered quiz system that helps you revise your interview preparation notes through interactive quizzes and spaced repetition.

## Features

- Sync notes from your GitHub repository
- Generate AI-powered questions from your notes
- Interactive quiz sessions with immediate feedback
- Progress tracking and performance analytics
- Support for multiple topics and difficulty levels
- CLI interface for easy interaction

## Prerequisites

- Java 17 or higher
- Maven
- GitHub repository with interview notes (in markdown format)
- OpenAI API key

## Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd interview-quiz-system
```

2. Configure the application:
   - Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties`
   - Update the following properties:
     - `github.repository.url`: Your GitHub repository URL containing interview notes
     - `openai.api.key`: Your OpenAI API key

3. Build the application:
```bash
mvn clean install
```

4. Run the application:
```bash
java -jar target/quiz-system-0.0.1-SNAPSHOT.jar
```

## Usage

The application provides a CLI interface with the following options:

1. **Sync Notes from GitHub**
   - Pulls the latest notes from your GitHub repository
   - Parses and organizes content by topic

2. **Start Quiz Session**
   - Select a topic from available notes
   - Choose difficulty level (EASY, MEDIUM, HARD)
   - Set number of questions (1-10)
   - Answer questions and receive immediate feedback

3. **View Progress**
   - See overall performance statistics
   - View topic-wise progress
   - Track improvement over time

## Project Structure

```
interview-quiz-system/
├── src/main/java/com/interview/quizsystem/
│   ├── cli/              # CLI interface
│   ├── config/           # Configuration classes
│   ├── model/            # Domain models
│   ├── service/          # Business logic
│   │   └── impl/         # Service implementations
│   └── InterviewQuizSystemApplication.java
├── src/main/resources/
│   └── application.properties  # Configuration properties
└── pom.xml              # Maven configuration
```

## Configuration

The application can be configured through `application.properties`:

```properties
# Server Configuration
server.port=8080

# GitHub Configuration
github.repository.url=<your-repo-url>
github.repository.branch=main
github.repository.local-path=./data/repo

# OpenAI Configuration
openai.api.key=<your-api-key>
openai.model=gpt-3.5-turbo
openai.temperature=0.7
openai.max-tokens=500

# Quiz System Configuration
quiz.session.questions-per-session=5
quiz.session.default-difficulty=MEDIUM
```

## Note Format

The system expects your notes to be in markdown format, organized by topics. Each file should follow a consistent naming pattern to help with topic identification.

Example note structure:
```
interview-notes/
├── algorithms-sorting.md
├── algorithms-searching.md
├── system-design-basics.md
├── system-design-scalability.md
└── ...
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.