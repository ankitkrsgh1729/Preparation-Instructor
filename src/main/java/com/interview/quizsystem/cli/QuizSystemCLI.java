package com.interview.quizsystem.cli;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuizSession;
import com.interview.quizsystem.model.Progress;
import com.interview.quizsystem.model.QuestionType;
import com.interview.quizsystem.service.GitHubParserService;
import com.interview.quizsystem.service.QuizSessionService;
import com.interview.quizsystem.service.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Slf4j
// @Component - Disabled for REST API usage
@RequiredArgsConstructor
public class QuizSystemCLI implements CommandLineRunner {

    private final GitHubParserService gitHubParserService;
    private final QuizSessionService quizSessionService;
    private final ProgressTrackingService progressTrackingService;
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void run(String... args) {
        while (true) {
            displayMainMenu();
            int choice = readIntInput();

            try {
                switch (choice) {
                    case 1 -> syncNotes();
                    case 2 -> startQuizSession();
                    case 3 -> viewProgress();
                    case 4 -> {
                        System.out.println("Exiting...");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                log.error("Error processing choice: {}", choice, e);
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    private void displayMainMenu() {
        System.out.println("\n=== Interview Quiz System ===");
        System.out.println("1. Sync Notes from GitHub");
        System.out.println("2. Start Quiz Session");
        System.out.println("3. View Progress");
        System.out.println("4. Exit");
        System.out.print("Enter your choice: ");
    }

    private void syncNotes() {
        System.out.println("\nSyncing notes from GitHub...");
        gitHubParserService.syncRepository();
        System.out.println("Notes synced successfully!");
    }

    private void startQuizSession() {
        List<String> topics = gitHubParserService.getAvailableTopics();
        if (topics.isEmpty()) {
            System.out.println("No topics available. Please sync notes first.");
            return;
        }

        System.out.println("\nAvailable Topics:");
        for (int i = 0; i < topics.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, topics.get(i));
        }

        System.out.print("Select topic number: ");
        int topicChoice = readIntInput() - 1;
        if (topicChoice < 0 || topicChoice >= topics.size()) {
            System.out.println("Invalid topic selection.");
            return;
        }

        System.out.println("\nSelect Difficulty:");
        System.out.println("1. EASY");
        System.out.println("2. MEDIUM");
        System.out.println("3. HARD");
        System.out.print("Enter choice: ");
        
        Difficulty difficulty;
        switch (readIntInput()) {
            case 1 -> difficulty = Difficulty.EASY;
            case 2 -> difficulty = Difficulty.MEDIUM;
            case 3 -> difficulty = Difficulty.HARD;
            default -> {
                System.out.println("Invalid difficulty selection.");
                return;
            }
        }

        System.out.print("Enter number of questions (1-10): ");
        int questionCount = Math.min(10, Math.max(1, readIntInput()));

        QuizSession session = quizSessionService.startSession(topics.get(topicChoice), difficulty, questionCount);
        runQuizSession(session);
    }

    private void runQuizSession(QuizSession session) {
        System.out.println("\n=== Quiz Session Started ===");
        session.getQuestions().forEach(question -> {
            System.out.println("\n" + question.getContent());
            
            if (!question.getOptions().isEmpty()) {
                for (int i = 0; i < question.getOptions().size(); i++) {
                    System.out.printf("%d. %s%n", i + 1, question.getOptions().get(i));
                }
            }

            System.out.print("Your answer: ");
            String answer = scanner.nextLine().trim();
            
            QuizSession updatedSession = quizSessionService.submitAnswer(session.getId(), question.getId(), answer);
            boolean isCorrect = updatedSession.getAnswers().get(updatedSession.getAnswers().size() - 1).isCorrect();
            
            if (isCorrect) {
                System.out.println("Correct!");
            } else {
                System.out.println("Incorrect.");
                if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
                    // Find the index of the correct answer
                    int correctIndex = -1;
                    for (int i = 0; i < question.getOptions().size(); i++) {
                        if (question.getOptions().get(i).equals(question.getCorrectAnswer())) {
                            correctIndex = i + 1;
                            break;
                        }
                    }
                    System.out.printf("The correct answer was: Option %d - %s%n", 
                        correctIndex, question.getCorrectAnswer());
                } else {
                    System.out.println("The correct answer was: " + question.getCorrectAnswer());
                }
            }
            System.out.println("Explanation: " + question.getExplanation());
        });

        quizSessionService.endSession(session.getId());
        System.out.printf("\nSession completed! Score: %.2f%%%n", session.getScore());
    }

    private void viewProgress() {
        Progress progress = progressTrackingService.getProgress();
        System.out.println("\n=== Progress Report ===");
        System.out.printf("Total Questions Answered: %d%n", progress.getTotalQuestionsAnswered());
        System.out.printf("Total Correct Answers: %d%n", progress.getTotalCorrectAnswers());
        
        if (progress.getTotalQuestionsAnswered() > 0) {
            double overallAccuracy = (double) progress.getTotalCorrectAnswers() / progress.getTotalQuestionsAnswered() * 100;
            System.out.printf("Overall Accuracy: %.2f%%%n", overallAccuracy);
        }

        System.out.println("\nProgress by Topic:");
        progress.getTopicProgressMap().forEach((topic, topicProgress) -> {
            System.out.printf("\nTopic: %s%n", topic);
            System.out.printf("Questions Answered: %d%n", topicProgress.getQuestionsAnswered());
            System.out.printf("Accuracy: %.2f%%%n", topicProgress.getAccuracy() * 100);
            System.out.printf("Current Difficulty: %s%n", topicProgress.getCurrentDifficulty());
            System.out.printf("Last Studied: %s%n", topicProgress.getLastStudied());
        });
    }

    private int readIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
} 