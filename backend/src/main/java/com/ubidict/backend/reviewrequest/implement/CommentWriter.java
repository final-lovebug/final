package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.Comment;
import com.ubidict.backend.reviewrequest.infra.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentWriter {

    private final CommentRepository commentRepository;

    public Comment write(Comment comment) {
        return commentRepository.save(comment);
    }
}
