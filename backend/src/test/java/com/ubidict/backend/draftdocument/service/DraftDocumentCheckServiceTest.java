package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.infra.ai.LlmJobOutboxService;
import com.ubidict.backend.common.infra.ai.LlmMode;
import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.implement.CheckJobCreationPolicyValidator;
import com.ubidict.backend.draftdocument.implement.CheckJobReader;
import com.ubidict.backend.draftdocument.implement.CheckJobWriter;
import com.ubidict.backend.draftdocument.implement.DraftDocumentAccessValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentCreationPolicyValidator;
import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import com.ubidict.backend.draftdocument.service.model.CreateCheckJobCommand;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DraftDocumentCheckServiceTest {

    private final CheckJobReader checkJobReader = mock(CheckJobReader.class);
    private final CheckJobWriter checkJobWriter = mock(CheckJobWriter.class);
    private final CheckJobCreationPolicyValidator checkJobCreationPolicyValidator =
            mock(CheckJobCreationPolicyValidator.class);
    private final DraftDocumentAccessValidator accessValidator = mock(DraftDocumentAccessValidator.class);
    private final DraftDocumentCreationPolicyValidator draftCreationPolicyValidator =
            mock(DraftDocumentCreationPolicyValidator.class);
    private final DictionaryTermQueryPort dictionaryTermQueryPort = mock(DictionaryTermQueryPort.class);
    private final LlmJobOutboxService outboxService = mock(LlmJobOutboxService.class);
    private final LlmProperties llmProperties = mock(LlmProperties.class);
    private final DraftDocumentCheckService service = new DraftDocumentCheckService(
            checkJobReader,
            checkJobWriter,
            checkJobCreationPolicyValidator,
            accessValidator,
            draftCreationPolicyValidator,
            dictionaryTermQueryPort,
            outboxService,
            llmProperties);

    @DisplayName("문서 대조를 요청하면 대기 작업을 저장하고 요청 이벤트를 발행한다.")
    @Test
    void request() {
        DocumentSnapshot document = new DocumentSnapshot(10L, 20L, 1, "본문");
        CheckJob checkJob = CheckJob.create(10L, 30L);
        ReflectionTestUtils.setField(checkJob, "id", 40L);
        given(accessValidator.validateCreation(10L, 30L)).willReturn(document);
        given(dictionaryTermQueryPort.activeVersionNo(20L)).willReturn(OptionalInt.of(3));
        given(checkJobWriter.append(10L, 30L)).willReturn(checkJob);
        given(llmProperties.mode()).willReturn(LlmMode.STUB);

        CheckJobResult result = service.request(new CreateCheckJobCommand(10L, 30L));

        assertThat(result.checkJobId()).isEqualTo(40L);
        assertThat(result.status()).isEqualTo(CheckJobStatus.PENDING);
        verify(draftCreationPolicyValidator).validate(10L);
        verify(checkJobCreationPolicyValidator).validate(10L);
        verify(outboxService).enqueue(org.mockito.ArgumentMatchers.any());
    }
}
