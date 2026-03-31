package com.microform.submission.service;

import com.microform.common.pagination.PageRequest;
import com.microform.common.pagination.PageResponse;
import com.microform.submission.api.SubmissionResponse;
import com.microform.submission.domain.SubmissionFilter;
import com.microform.submission.persistence.SubmissionQueryRepository;
import com.microform.submission.persistence.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SubmissionQueryService {

    private final SubmissionQueryRepository queryRepository;
    private final SubmissionRepository submissionRepository;

    public SubmissionQueryService(SubmissionQueryRepository queryRepository,
                                  SubmissionRepository submissionRepository) {
        this.queryRepository = queryRepository;
        this.submissionRepository = submissionRepository;
    }

    public PageResponse<SubmissionResponse> list(SubmissionFilter filter, PageRequest pageRequest) {
        long total = queryRepository.countByFilter(filter);
        List<SubmissionResponse> content = queryRepository
                .findIdsByFilter(filter, pageRequest.size(), pageRequest.offset())
                .stream()
                .map(id -> submissionRepository.findById(id).orElseThrow())
                .map(SubmissionResponse::from)
                .toList();

        return PageResponse.of(content, pageRequest.page(), pageRequest.size(), total);
    }
}
