package com.focess.util.logger;

import com.focess.commands.util.ChatConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FocessLogger {

    private static final Logger LOG = LoggerFactory.getLogger(FocessLogger.class);

    private static FocessLogger FOCESS_LOG;

    public FocessLogger() {
        if (FOCESS_LOG != null)
            throw new UnsupportedOperationException();
        FOCESS_LOG = this;
    }

    public void info(String message) {
        LOG.info(ChatConstants.CONSOLE_OUTPUT_HEADER + message);
    }

    public void consoleInput(String message) {
        LOG.debug(ChatConstants.CONSOLE_INPUT_HEADER + message);
    }
}
