package pl.pingpong.club.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.pingpong.club.model.JoinRequest;
import pl.pingpong.club.model.Training;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Async
    public void sendTrainingConfirmation(Training training) {
        if (!StringUtils.hasText(fromAddress)) {
            log.warn("MAIL_USERNAME nie jest skonfigurowany — pomijam wysyłkę emaila.");
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            String playerEmail = training.getPlayer().getEmail();
            String playerName  = training.getPlayer().getFirstName();
            String coachName   = training.getCoach().getFirstName() + " " + training.getCoach().getLastName();
            String date = training.getScheduledAt()
                    .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'o' HH:mm",
                            new java.util.Locale("pl")));
            String duration = training.getDurationMinutes() + " min";
            String price = training.getTotalPrice()
                    .stripTrailingZeros().toPlainString() + " zł";

            helper.setFrom(fromAddress);
            helper.setTo(playerEmail);
            helper.setSubject("Nowy trening zaplanowany — " + date);
            helper.setText(buildHtml(playerName, coachName, date, duration, price), true);

            mailSender.send(message);
            log.info("Email wysłany do: {}", playerEmail);
        } catch (Exception e) {
            log.error("Błąd wysyłki emaila do {}: {}", training.getPlayer().getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendJoinRequestNotification(JoinRequest request) {
        if (!StringUtils.hasText(fromAddress)) {
            log.warn("MAIL_USERNAME nie jest skonfigurowany — pomijam wysyłkę emaila.");
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            String playerEmail = request.getPlayer().getEmail();
            String playerName  = request.getPlayer().getFirstName();
            String coachName   = request.getCoach().getFirstName() + " " + request.getCoach().getLastName();

            helper.setFrom(fromAddress);
            helper.setTo(playerEmail);
            helper.setSubject("Zaproszenie od trenera " + coachName);
            helper.setText(buildJoinRequestHtml(playerName, coachName), true);

            mailSender.send(message);
            log.info("Email z zaproszeniem wysłany do: {}", playerEmail);
        } catch (Exception e) {
            log.error("Błąd wysyłki emaila z zaproszeniem do {}: {}", request.getPlayer().getEmail(), e.getMessage());
        }
    }

    private String buildJoinRequestHtml(String player, String coach) {
        return """
                <!DOCTYPE html>
                <html lang="pl">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#1e293b;border-radius:12px;border:1px solid #334155;overflow:hidden;">
                        <tr>
                          <td style="background:#22c55e;padding:24px 32px;">
                            <p style="margin:0;color:#fff;font-size:11px;font-weight:600;
                                      letter-spacing:2px;text-transform:uppercase;">TTManager</p>
                            <h1 style="margin:4px 0 0;color:#fff;font-size:22px;font-weight:700;">
                              Masz nowe zaproszenie!
                            </h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;">
                            <p style="margin:0 0 24px;color:#94a3b8;font-size:15px;">
                              Cześć <strong style="color:#f8fafc;">%s</strong>,<br>
                              trener <strong style="color:#f8fafc;">%s</strong> zaprasza Cię do swojego zespołu.
                            </p>
                            <p style="margin:0;color:#64748b;font-size:13px;">
                              Zaloguj się do aplikacji, przejdź do zakładki <strong style="color:#94a3b8;">Zaproszenia</strong>
                              i zaakceptuj lub odrzuć zaproszenie.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(player, coach);
    }

    private String buildHtml(String player, String coach, String date, String duration, String price) {
        return """
                <!DOCTYPE html>
                <html lang="pl">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#1e293b;border-radius:12px;border:1px solid #334155;overflow:hidden;">

                        <!-- Header -->
                        <tr>
                          <td style="background:#22c55e;padding:24px 32px;">
                            <p style="margin:0;color:#fff;font-size:11px;font-weight:600;
                                      letter-spacing:2px;text-transform:uppercase;">TTManager</p>
                            <h1 style="margin:4px 0 0;color:#fff;font-size:22px;font-weight:700;">
                              Masz nowy trening!
                            </h1>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="padding:32px;">
                            <p style="margin:0 0 24px;color:#94a3b8;font-size:15px;">
                              Cześć <strong style="color:#f8fafc;">%s</strong>,<br>
                              trener <strong style="color:#f8fafc;">%s</strong> zaplanował dla Ciebie trening.
                            </p>

                            <!-- Details card -->
                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="background:#0f172a;border-radius:8px;border:1px solid #334155;">
                              <tr>
                                <td style="padding:20px 24px;border-bottom:1px solid #334155;">
                                  <p style="margin:0;color:#94a3b8;font-size:11px;
                                            text-transform:uppercase;letter-spacing:1px;">Termin</p>
                                  <p style="margin:4px 0 0;color:#f8fafc;font-size:16px;
                                            font-weight:600;text-transform:capitalize;">%s</p>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:16px 24px;border-bottom:1px solid #334155;">
                                  <p style="margin:0;color:#94a3b8;font-size:11px;
                                            text-transform:uppercase;letter-spacing:1px;">Czas trwania</p>
                                  <p style="margin:4px 0 0;color:#f8fafc;font-size:15px;">%s</p>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:16px 24px;">
                                  <p style="margin:0;color:#94a3b8;font-size:11px;
                                            text-transform:uppercase;letter-spacing:1px;">Kwota</p>
                                  <p style="margin:4px 0 0;color:#22c55e;font-size:18px;font-weight:700;">%s</p>
                                </td>
                              </tr>
                            </table>

                            <p style="margin:24px 0 0;color:#64748b;font-size:12px;text-align:center;">
                              Zaloguj się do aplikacji aby zobaczyć szczegóły treningu.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(player, coach, date, duration, price);
    }
}
