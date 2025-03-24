package app.IT;

import app.transaction.model.Transaction;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")  // Use the test profile
@Transactional  // Ensures changes are rolled back after each test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)  // Reset context
class TransactionServiceIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        // Create test users before each test
        sender = userRepository.save(User.builder()
                .username("senderUser")
                .email("sender@example.com")
                .password("hashedpassword")
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());

        receiver = userRepository.save(User.builder()
                .username("receiverUser")
                .email("receiver@example.com")
                .password("hashedpassword")
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .build());
    }

    @Test
    void testCreateTransaction_ShouldSaveTransaction() {
        // Given
        Double credits = 100.0;
        String description = "Sent money";
        TransactionType type = TransactionType.SEND;

        // When
        transactionService.createTransaction(sender, credits, description, type);

        // Then
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(credits, transactions.get(0).getCredits());
        assertEquals(description, transactions.get(0).getDescription());
        assertEquals(type, transactions.get(0).getTransactionType());
    }

    @Test
    void testCreateTransaction_ShouldNotSaveTransaction_WhenCreditsAreZero() {
        // Given
        Double credits = 0.0;
        String description = "Invalid transaction";
        TransactionType type = TransactionType.SEND;

        // When
        transactionService.createTransaction(sender, credits, description, type);

        // Then
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(0, transactions.size());  // Should not save
    }

    @Test
    void testFindTransactionById_ShouldReturnCorrectTransaction() {
        // Given
        Transaction savedTransaction = transactionRepository.save(Transaction.builder()
                .user(sender)
                .credits(50.0)
                .description("Test Transaction")
                .transactionType(TransactionType.RECEIVE)
                .createdOn(LocalDateTime.now())
                .build());

        // When
        Transaction foundTransaction = transactionService.findTransactionById(savedTransaction.getId());

        // Then
        assertNotNull(foundTransaction);
        assertEquals(savedTransaction.getId(), foundTransaction.getId());
    }

    @Test
    void testFindTransactionById_ShouldReturnNull_WhenTransactionNotFound() {
        // When
        Transaction foundTransaction = transactionService.findTransactionById(UUID.randomUUID());

        // Then
        assertNull(foundTransaction);
    }

    @Test
    void testGetAllTransactions_ShouldReturnTransactionsInDescendingOrder() {
        // Given
        Transaction t1 = transactionRepository.save(Transaction.builder()
                .user(sender)
                .credits(50.0)
                .description("Older Transaction")
                .transactionType(TransactionType.SEND)
                .createdOn(LocalDateTime.now().minusDays(1))
                .build());

        Transaction t2 = transactionRepository.save(Transaction.builder()
                .user(receiver)
                .credits(100.0)
                .description("Newer Transaction")
                .transactionType(TransactionType.RECEIVE)
                .createdOn(LocalDateTime.now())
                .build());

        // When
        List<Transaction> transactions = transactionService.getAllTransactions();

        // Then
        assertEquals(2, transactions.size());
        assertEquals(t2.getId(), transactions.get(0).getId()); // Newest first
        assertEquals(t1.getId(), transactions.get(1).getId()); // Oldest last
    }
}
