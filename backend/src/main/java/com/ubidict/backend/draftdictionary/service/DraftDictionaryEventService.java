package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDictionaryEventService {

    private final DraftDictionaryReader draftDictionaryReader;

    @Transactional
    public void markReviewRequested(Long draftDictionaryId) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(draftDictionaryId);
        draftDictionary.markReviewRequested();
        log.info(
                "[DraftDictionaryEventService.markReviewRequested] Draft dictionary review requested. draftDictionaryId={}",
                draftDictionaryId);
    }

    @Transactional
    public void reopen(Long draftDictionaryId) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(draftDictionaryId);
        draftDictionary.reopen();
        log.info(
                "[DraftDictionaryEventService.reopen] Draft dictionary reopened. draftDictionaryId={}",
                draftDictionaryId);
    }

    @Transactional
    public void reopenForRedecision(Long draftDictionaryId) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(draftDictionaryId);
        draftDictionary.reopenForRedecision();
        log.info(
                "[DraftDictionaryEventService.reopenForRedecision] Draft dictionary reopened for redecision. draftDictionaryId={}",
                draftDictionaryId);
    }

    @Transactional
    public void markRevised(Long draftDictionaryId) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(draftDictionaryId);
        draftDictionary.markRevised();
        log.info(
                "[DraftDictionaryEventService.markRevised] Draft dictionary revised. draftDictionaryId={}",
                draftDictionaryId);
    }
}
