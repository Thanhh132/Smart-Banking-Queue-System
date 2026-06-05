package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public List<Branch> getBranchesByBank(String bankName) {
        return branchRepository.findByBankName(bankName);
    }

    public List<Branch> getNearestBranches(String bankName, double latitude, double longitude) {
        return branchRepository.findNearestBranches(latitude, longitude, bankName);
    }

    public Branch createBranch(Branch branch) {
        return branchRepository.save(branch);
    }
}