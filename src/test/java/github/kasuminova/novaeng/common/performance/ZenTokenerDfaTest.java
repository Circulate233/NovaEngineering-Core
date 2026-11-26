package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;

class ZenTokenerDfaTest {
    @Test
    void generatedLexerMatchesUpstreamAndRejectsStaleData() throws Exception {
        ZenTokenerDfaVerification.main(new String[0]);
    }
}
