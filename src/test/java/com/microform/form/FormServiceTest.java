package com.microform.form;

import com.microform.form.domain.*;
import com.microform.form.persistence.FormRepository;
import com.microform.form.persistence.FormVersionRepository;
import com.microform.form.service.FormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock FormRepository formRepository;
    @Mock FormVersionRepository formVersionRepository;

    FormService formService;

    private static final List<FieldDefinition> SCHEMA = List.of(
            new FieldDefinition("email", FieldType.STRING, true, FieldConstraints.empty(), "Email", null)
    );
    private static final WorkflowDefinition WORKFLOW = new WorkflowDefinition(
            List.of("DRAFT", "SUBMITTED", "APPROVED"),
            List.of(new TransitionRule("DRAFT", "SUBMITTED", "submitter"))
    );

    @BeforeEach
    void setUp() {
        formService = new FormService(formRepository, formVersionRepository);
    }

    @Test
    void createForm_success() {
        when(formRepository.existsById("signup")).thenReturn(false);
        when(formRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(formVersionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Form form = formService.createForm("signup", SCHEMA, WORKFLOW);

        assertThat(form.getFormId()).isEqualTo("signup");
        assertThat(form.getLatestVersion()).isEqualTo(1);
        verify(formVersionRepository).save(any(FormVersion.class));
    }

    @Test
    void createForm_duplicateId_throws() {
        when(formRepository.existsById("signup")).thenReturn(true);

        assertThatThrownBy(() -> formService.createForm("signup", SCHEMA, WORKFLOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("signup");
    }

    @Test
    void addVersion_incrementsVersion() {
        Form existing = new Form("signup");
        when(formRepository.findById("signup")).thenReturn(Optional.of(existing));
        when(formRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(formVersionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FormVersion v2 = formService.addVersion("signup", SCHEMA, WORKFLOW);

        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(existing.getLatestVersion()).isEqualTo(2);
    }

    @Test
    void getVersion_notFound_throws() {
        when(formVersionRepository.findByIdFormIdAndIdVersion("unknown", 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.getVersion("unknown", 1))
                .isInstanceOf(NoSuchElementException.class);
    }
}
