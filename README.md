# Preparation Instructor

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

## Environment Configuration

The application requires several configuration properties to run. These are specified in `application.properties`:

- `openai.api.key`: Your OpenAI API key (required)
- `github.repository.url`: URL of the repository containing study materials
- `github.repository.branch`: Branch to use (default: main)
- Other properties as shown in `application.properties.example`

**Important**: Never commit your `application.properties` file with real API keys. Use the example file as a template and keep your actual configuration local.