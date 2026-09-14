package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.revisionlog.domain.RevisionLogChangeType;
import com.ubidict.backend.revisionlog.infra.port.TermSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 사전집 버전 간 diff 규칙(D-60)을 한 곳에 둔다. */
@Component
public class DictionaryDiffCalculator {

    public DictionaryDiff calculate(List<TermSnapshot> previousTerms, List<TermSnapshot> currentTerms) {
        Map<String, TermSnapshot> previousByForm = byPreferredForm(previousTerms);
        Map<String, TermSnapshot> currentByForm = byPreferredForm(currentTerms);
        List<DictionaryTermChange> changes = new ArrayList<>();

        currentTerms.forEach(term -> appendCurrentChange(previousByForm, changes, term));
        previousTerms.stream()
                .filter(term -> !currentByForm.containsKey(term.preferredForm()))
                .forEach(term -> changes.add(new DictionaryTermChange(RevisionLogChangeType.REMOVED, term, null)));

        int addedCount = count(changes, RevisionLogChangeType.ADDED);
        int changedCount = count(changes, RevisionLogChangeType.CHANGED);
        int removedCount = count(changes, RevisionLogChangeType.REMOVED);
        return new DictionaryDiff(List.copyOf(changes), addedCount, changedCount, removedCount);
    }

    private static Map<String, TermSnapshot> byPreferredForm(List<TermSnapshot> terms) {
        return terms.stream().collect(Collectors.toMap(TermSnapshot::preferredForm, Function.identity()));
    }

    private static void appendCurrentChange(
            Map<String, TermSnapshot> previousByForm, List<DictionaryTermChange> changes, TermSnapshot current) {
        TermSnapshot previous = previousByForm.get(current.preferredForm());
        if (previous == null) {
            changes.add(new DictionaryTermChange(RevisionLogChangeType.ADDED, current, null));
            return;
        }
        if (!previous.equals(current)) {
            changes.add(
                    new DictionaryTermChange(RevisionLogChangeType.CHANGED, current, changeDetail(previous, current)));
        }
    }

    private static String changeDetail(TermSnapshot previous, TermSnapshot current) {
        boolean definitionChanged = !java.util.Objects.equals(previous.definition(), current.definition());
        boolean englishNameChanged = !java.util.Objects.equals(previous.englishName(), current.englishName());
        if (definitionChanged && englishNameChanged) {
            return "정의·영문명 수정";
        }
        return definitionChanged ? "정의 수정" : "영문명 수정";
    }

    private static int count(List<DictionaryTermChange> changes, RevisionLogChangeType type) {
        return (int)
                changes.stream().filter(change -> change.changeType() == type).count();
    }
}
