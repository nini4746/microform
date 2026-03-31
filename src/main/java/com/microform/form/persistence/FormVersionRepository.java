package com.microform.form.persistence;

import com.microform.form.domain.FormVersion;
import com.microform.form.domain.FormVersionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormVersionRepository extends JpaRepository<FormVersion, FormVersionId> {

    Optional<FormVersion> findByIdFormIdAndIdVersion(String formId, int version);
}
