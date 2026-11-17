package com.openshelves.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/// [EmailSender] that only writes the email to the log, until real delivery is set up.
///
/// For development: the log then contains whatever the email carries, including one-time links,
/// so it must be replaced before going to production.
@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("Email (not sent; logging only) [to={}, subject={}]\n{}", to, subject, body);
    }
}
