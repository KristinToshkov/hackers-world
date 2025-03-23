package app;

import app.transaction.model.Transaction;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Transaction transaction;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .credits(100.0)
                .build();

        transaction = Transaction.builder()
                .id(transactionId)
                .user(user)
                .credits(50.0)
                .description("Test Transaction")
                .transactionType(TransactionType.SEND)
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldFindTransactionById() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        Transaction foundTransaction = transactionService.findTransactionById(transactionId);

        assertThat(foundTransaction).isNotNull();
        assertThat(foundTransaction.getId()).isEqualTo(transactionId);
    }

    @Test
    void shouldReturnNullIfTransactionNotFound() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        Transaction foundTransaction = transactionService.findTransactionById(transactionId);

        assertThat(foundTransaction).isNull();
    }

    @Test
    void shouldCreateTransactionSuccessfully() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(user, 50.0, "Deposit", TransactionType.RECEIVE);

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldNotCreateTransactionWhenCreditsAreZeroOrNegative() {
        transactionService.createTransaction(user, 0.0, "Invalid", TransactionType.SEND);
        transactionService.createTransaction(user, -10.0, "Invalid", TransactionType.SEND);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldGetAllTransactionsOrderedByDate() {
        when(transactionRepository.findAllByOrderByCreatedOnDesc()).thenReturn(List.of(transaction));

        List<Transaction> transactions = transactionService.getAllTransactions();

        assertThat(transactions).isNotEmpty();
        assertThat(transactions).containsExactly(transaction);
    }
}
