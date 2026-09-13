package com.ubidict.backend.draftdocument.infra.port;

import java.util.List;

public interface TermCheckerPort {

    List<CheckSuggestion> check(DocumentSnapshot document, List<TermSnapshot> activeTerms);
}
