package com.team.TeamUp.service;

import com.team.TeamUp.domain.ResetRequest;
import com.team.TeamUp.persistence.ResetRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
public class ResetRequestService {

    private ResetRequestRepository resetRequestRepository;

    @Autowired
    public ResetRequestService(ResetRequestRepository resetRequestRepository){
        this.resetRequestRepository = resetRequestRepository;
    }

    public ResetRequest getByID(int id) {
        return resetRequestRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    public ResetRequest save(ResetRequest resetRequest) {
        return resetRequestRepository.save(resetRequest);
    }
}