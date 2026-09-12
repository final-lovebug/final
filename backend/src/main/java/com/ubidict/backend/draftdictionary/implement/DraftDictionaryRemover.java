package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import org.springframework.stereotype.Component;

@Component
public class DraftDictionaryRemover {

    public void remove(DraftDictionary draftDictionary) {
        draftDictionary.delete();
    }
}
