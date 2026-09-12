package com.ubidict.backend.document.infra;

import com.ubidict.backend.document.domain.Label;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findAllByWorkspaceIdAndNameIn(Long workspaceId, Collection<String> names);

    List<Label> findAllByWorkspaceIdOrderByNameAsc(Long workspaceId);
}
