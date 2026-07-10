package org.igye.remem3.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.state.StateRepository;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/state")
public class StateController extends HtmlBuilder {

    private final StateRepository stateRepository;

    @RequestMapping("/{stateId}")
    public ResponseEntity<?> process(HttpServletRequest req, @PathVariable String stateId) {
        RequestParams params = new RequestParamsImpl(req);
        String actualStateId = stateRepository.getActualStateId(stateId);
        if (stateId.equals(actualStateId) && params.isEmpty()) {
            return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<!doctype html>\n" + stateRepository.rednerState(actualStateId).toString());
        } else {
            if (!params.isEmpty()) {
                stateRepository.updateState(actualStateId, params);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/state/" + actualStateId));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
}
