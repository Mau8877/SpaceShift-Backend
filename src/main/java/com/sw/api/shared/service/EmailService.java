package com.sw.api.shared.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

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

    public void enviarReciboPago(String correo, String nombreCliente, byte[] pdfContent, String concepto) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(correo);
        helper.setSubject("Comprobante de Pago Exitoso — SpaceShift");
        helper.setText("Hola " + nombreCliente + ",\n\nConfirmamos tu pago con éxito. Adjunto a este correo encontrarás el comprobante de tu transacción en formato PDF.\n\nSaludos,\nEl equipo de SpaceShift");
        helper.setFrom("noreply@spaceshift.com");
        
        ByteArrayResource attachment = new ByteArrayResource(pdfContent);
        helper.addAttachment("Comprobante_Pago_" + concepto.replace(" ", "_") + ".pdf", attachment);
        
        mailSender.send(message);
    }
}