package com.microform.form.api;

import com.microform.form.service.FormService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/forms")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @PostMapping
    public ResponseEntity<FormResponse> createForm(@Valid @RequestBody CreateFormRequest request) {
        var form = formService.createForm(request.formId(), request.fields(), request.workflow());
        return ResponseEntity.status(HttpStatus.CREATED).body(FormResponse.from(form));
    }

    @PostMapping("/{formId}/versions")
    public ResponseEntity<FormVersionResponse> addVersion(
            @PathVariable String formId,
            @Valid @RequestBody CreateVersionRequest request) {
        var version = formService.addVersion(formId, request.fields(), request.workflow());
        return ResponseEntity.status(HttpStatus.CREATED).body(FormVersionResponse.from(version));
    }

    @GetMapping("/{formId}")
    public ResponseEntity<FormVersionResponse> getLatestVersion(@PathVariable String formId) {
        return ResponseEntity.ok(FormVersionResponse.from(formService.getLatestVersion(formId)));
    }

    @GetMapping("/{formId}/versions/{version}")
    public ResponseEntity<FormVersionResponse> getVersion(
            @PathVariable String formId,
            @PathVariable int version) {
        return ResponseEntity.ok(FormVersionResponse.from(formService.getVersion(formId, version)));
    }
}
