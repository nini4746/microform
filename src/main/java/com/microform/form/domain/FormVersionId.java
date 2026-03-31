package com.microform.form.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FormVersionId implements Serializable {

    private String formId;
    private int version;

    protected FormVersionId() {}

    public FormVersionId(String formId, int version) {
        this.formId = formId;
        this.version = version;
    }

    public String getFormId() { return formId; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormVersionId that)) return false;
        return version == that.version && Objects.equals(formId, that.formId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formId, version);
    }
}
