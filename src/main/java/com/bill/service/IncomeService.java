package com.bill.service;

import com.bill.model.Income;
import com.bill.repository.IncomeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;

    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    public List<Income> findAll() {
        return incomeRepository.findAllByOrderByYearDescMonthDesc();
    }

    public Optional<Income> findByMonthAndYear(int month, int year) {
        return incomeRepository.findByMonthAndYear(month, year);
    }

    public Income save(Income income) {
        return incomeRepository.save(income);
    }

    public void deleteById(String id) {
        incomeRepository.deleteById(id);
    }
}
