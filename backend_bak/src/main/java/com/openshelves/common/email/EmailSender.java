package com.openshelves.common.email;

/// Sends plain-text emails to users, e.g. password reset links.
///
/// Callers depend on this interface only, so real delivery (SMTP, a mail API) can replace
/// the current logging implementation without touching them.
public interface EmailSender {

    /// Sends an email.
    ///
    /// @param to the recipient's address
    /// @param subject the subject line
    /// @param body the plain-text body
    void send(String to, String subject, String body);
}
