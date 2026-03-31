package com.microform.form.persistence;

import com.microform.form.domain.Form;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, String> {
}
