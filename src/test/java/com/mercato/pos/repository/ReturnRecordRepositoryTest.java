package com.mercato.pos.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.mercato.pos.model.ReturnRecord;

/**
 * Tests de integración para ReturnRecordRepository.
 */
@DataJpaTest
class ReturnRecordRepositoryTest {

    @Autowired
    private ReturnRecordRepository returnRecordRepository;

    private ReturnRecord record1;
    private ReturnRecord record2;
    private ReturnRecord record3;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        record1 = new ReturnRecord("SALE001", "PROD001", 2, "Defective", now);
        record2 = new ReturnRecord("SALE001", "PROD002", 1, "Wrong size", now);
        record3 = new ReturnRecord("SALE002", "PROD001", 3, "Damaged", now);

        returnRecordRepository.save(record1);
        returnRecordRepository.save(record2);
        returnRecordRepository.save(record3);
    }

    @Test
    void testFindBySaleIdAndProductId() {
        // Act
        List<ReturnRecord> results = returnRecordRepository.findBySaleIdAndProductId("SALE001", "PROD001");

        // Assert
        assertEquals(1, results.size());
        assertEquals(record1.getId(), results.get(0).getId());
    }

    @Test
    void testFindBySaleIdAndProductId_NoResults() {
        // Act
        List<ReturnRecord> results = returnRecordRepository.findBySaleIdAndProductId("SALE001", "PROD999");

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindBySaleId() {
        // Act
        List<ReturnRecord> results = returnRecordRepository.findBySaleId("SALE001");

        // Assert
        assertEquals(2, results.size());
    }

    @Test
    void testFindBySaleId_SingleRecord() {
        // Act
        List<ReturnRecord> results = returnRecordRepository.findBySaleId("SALE002");

        // Assert
        assertEquals(1, results.size());
        assertEquals(record3.getId(), results.get(0).getId());
    }

    @Test
    void testFindBySaleId_NoResults() {
        // Act
        List<ReturnRecord> results = returnRecordRepository.findBySaleId("SALE999");

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testSaveReturnRecord() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        ReturnRecord newRecord = new ReturnRecord("SALE003", "PROD003", 1, "Changed mind", now);

        // Act
        ReturnRecord saved = returnRecordRepository.save(newRecord);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("SALE003", saved.getSaleId());
    }
}
