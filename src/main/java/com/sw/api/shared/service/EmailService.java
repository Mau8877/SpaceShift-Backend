package com.sw.api.shared.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCodigoRecuperacion(String correo, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(correo);
        message.setSubject("Código de recuperación de contraseña - SpaceShift");
        message.setText(
                "Hola,\n\n" +
                "Recibimos una solicitud para recuperar tu contraseña.\n\n" +
                "Tu código de recuperación es: " + codigo + "\n\n" +
                "Este código expira en 15 minutos.\n\n" +
                "Si no solicitaste este código, puedes ignorar este correo.\n\n" +
                "Saludos,\n" +
                "Equipo SpaceShift"
        );
        message.setFrom("noreply@spaceshift.com");

        mailSender.send(message);
    }
}