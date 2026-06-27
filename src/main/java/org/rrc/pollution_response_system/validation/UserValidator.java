package org.rrc.pollution_response_system.validation;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class UserValidator {

    // Phone: 10 digits, starts with 078, 079, 072, 073
    private static final String PHONE_REGEX = "^07[2389]\\d{7}$";

    // Email: Standard, but domain must start with letter and contain NO numbers
    // Structure: [local]@[domain-start][domain-rest].[tld]
    // domain-start: letter
    // domain-rest: letters, hyphens, dots (but no consecutive dots, etc -
    // simplified to [a-zA-Z-.]*)
    // The user requirement: "should not contain numbers" in domain part.
    private static final String EMAIL_REGEX = "^[\\w-\\.]+@[a-zA-Z][a-zA-Z-]*(\\.[a-zA-Z][a-zA-Z-]*)*\\.[a-zA-Z]{2,}$";

    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public boolean isValidPhone(String phone) {
        if (phone == null)
            return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }

    public boolean isValidEmail(String email) {
        if (email == null)
            return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public void validateUser(String email, String phone) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Invalid email format. Domain must start with a letter and contain no numbers.");
        }
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException(
                    "Invalid phone number. Must be 10 digits starting with 078, 079, 072, or 073.");
        }
    }
}
