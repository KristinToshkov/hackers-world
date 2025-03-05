package app.transaction.service;

import app.transaction.model.Transaction;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import app.user.model.User;
import app.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public Transaction findTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId).orElse(null);
    }

    public void createTransaction(User user, Double credits, String description, TransactionType transactionType) {
        if(credits <= 0) {
            return;
        }
        Transaction transaction = Transaction.builder().user(user).credits(credits).description(description).transactionType(transactionType).createdOn(LocalDateTime.now()).build();
        transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedOnDesc();
    }
}
