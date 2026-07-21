package com.grafie.botjava.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PostgresTwoInstanceLiveSmokeSafetyTest {

    @Test
    void shouldNotInspectOrCleanSchemaBeforeEmptyCheckPasses() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        PostgresTwoInstanceLiveSmokeIT.clearSmokeSchemaIfAllowed(false, context);

        verifyNoInteractions(context);
    }
}
