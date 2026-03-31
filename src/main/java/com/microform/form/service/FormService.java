package com.microform.form.service;

import com.microform.form.domain.*;
import com.microform.form.persistence.FormRepository;
import com.microform.form.persistence.FormVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class FormService {

    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;

    public FormService(FormRepository formRepository, FormVersionRepository formVersionRepository) {
        this.formRepository = formRepository;
        this.formVersionRepository = formVersionRepository;
    }

    public Form createForm(String formId, List<FieldDefinition> schema, WorkflowDefinition workflow) {
        if (formRepository.existsById(formId)) {
            throw new IllegalStateException("Form already exists: " + formId);
        }
        Form form = new Form(formId);
        formRepository.save(form);
        formVersionRepository.save(new FormVersion(formId, 1, schema, workflow));
        return form;
    }

    public FormVersion addVersion(String formId, List<FieldDefinition> schema, WorkflowDefinition workflow) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new NoSuchElementException("Form not found: " + formId));
        int newVersion = form.getLatestVersion() + 1;
        form.incrementVersion();
        formRepository.save(form);
        FormVersion version = new FormVersion(formId, newVersion, schema, workflow);
        return formVersionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public FormVersion getLatestVersion(String formId) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new NoSuchElementException("Form not found: " + formId));
        return formVersionRepository.findByIdFormIdAndIdVersion(formId, form.getLatestVersion())
                .orElseThrow(() -> new NoSuchElementException("Form version not found"));
    }

    @Transactional(readOnly = true)
    public FormVersion getVersion(String formId, int version) {
        return formVersionRepository.findByIdFormIdAndIdVersion(formId, version)
                .orElseThrow(() -> new NoSuchElementException("Form version not found: " + formId + "/v" + version));
    }
}
