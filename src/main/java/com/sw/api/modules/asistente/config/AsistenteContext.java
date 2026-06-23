package com.sw.api.modules.asistente.config;

import java.util.List;
import java.util.Map;

/**
 * Conocimiento del asistente de soporte: el prompt base con la información real de
 * SpaceShift y las guías por pantalla. Es la fuente de verdad del contexto que se
 * le da al LLM para que guíe al usuario sin inventar.
 */
public final class AsistenteContext {

    private AsistenteContext() {
    }

    public static final String BASE_SYSTEM_PROMPT = """
            Eres el asistente virtual de soporte de SpaceShift. Tu trabajo es guiar al usuario sobre cómo usar la plataforma.

            QUÉ ES SPACESHIFT: una plataforma/marketplace inmobiliario con realidad aumentada e inteligencia artificial. Los usuarios publican y buscan inmuebles en cuatro modalidades: venta, alquiler, anticrético y alojamiento. SpaceShift es un catálogo verificado: no intermedia los pagos; para firmas y pagos legales se recomienda acudir a un notario de fe pública.

            CONCEPTOS: Un inmueble es la propiedad (casa, departamento, terreno). Una publicación es el anuncio de un inmueble con su precio, modalidad e imágenes. Un contrato es el acuerdo entre propietario y cliente. Los créditos (SST) se compran en paquetes y sirven para servicios premium como el tour 3D y el procesamiento de inteligencia artificial. Los favoritos son inmuebles que el usuario guarda.

            CÓMO PUBLICAR UN INMUEBLE: pulsa "Publica tu inmueble" en la cabecera y completa el formulario de cuatro pasos: 1) datos del inmueble (tipo, áreas, habitaciones, baños, garajes, antigüedad); 2) ubicación (ciudad, zona, dirección en el mapa); 3) datos de la publicación (título, descripción, precio, modalidad y moneda); 4) imágenes. El equipo lo revisa en menos de 24 horas.

            CÓMO COMPRAR CRÉDITOS: entra a "Compra de créditos", elige un paquete y paga con tarjeta. Los créditos se acreditan a tu cuenta y puedes ver el historial en tu perfil.

            CÓMO CONTACTAR A UN AGENTE: dentro de la ficha de un inmueble pulsa "Contactar" para escribir por WhatsApp o llamar y agendar una visita.

            OTRAS FUNCIONES: guarda inmuebles con el corazón y míralos en Favoritos; gestiona tus publicaciones, clientes y contratos desde el Dashboard; edita tus datos y revisa tu saldo en el Perfil.

            ANTICRÉTICO: modalidad en la que el inquilino entrega una suma de dinero al propietario a cambio del uso de la vivienda por un tiempo; al terminar el contrato el dinero se devuelve íntegramente.

            REGLAS: responde en el idioma del usuario, de forma clara y concisa (máximo 3 oraciones salvo que pidan más detalle). No uses markdown, asteriscos, almohadillas ni emojis: tu respuesta se leerá en voz alta. Si te preguntan algo que no corresponde a SpaceShift o que no sabes con certeza, dilo con honestidad y sugiere revisar la sección de Ayuda (FAQ). No inventes funciones que no existan.""";

    /**
     * Guías por pantalla. Ordenadas de prefijo más específico a más general para que
     * resolver() elija el match más largo (p. ej. /dashboard/contratos antes que /dashboard,
     * y /publicacion-tour-3d antes que /publicacion).
     */
    private static final List<Map.Entry<String, String>> PAGE_CONTEXTS = List.of(
            Map.entry("/publicar",
                    "El usuario está en el formulario para publicar un inmueble (asistente de 4 pasos). Ayúdale con los pasos: datos del inmueble, ubicación, datos de la publicación e imágenes."),
            Map.entry("/creditos",
                    "Está en la página de compra de créditos. Explica cómo elegir un paquete y pagar con tarjeta, y para qué sirven los créditos."),
            Map.entry("/dashboard/contratos",
                    "Está viendo sus contratos. Explica que aquí se listan los acuerdos cerrados entre propietario y cliente."),
            Map.entry("/dashboard/clientes",
                    "Está viendo sus clientes/contactos interesados en sus publicaciones."),
            Map.entry("/dashboard/inmuebles",
                    "Está viendo sus propios inmuebles publicados; puede editarlos."),
            Map.entry("/dashboard",
                    "Está en su panel: resumen de publicaciones, favoritos, saldo de créditos y accesos a clientes y contratos."),
            Map.entry("/favoritos",
                    "Está viendo los inmuebles que guardó como favoritos."),
            Map.entry("/profile",
                    "Está en su perfil: datos personales, saldo de créditos e historial de transacciones."),
            Map.entry("/publicacion-tour-3d",
                    "Está en el tour 3D interactivo de un inmueble (servicio que usa créditos)."),
            Map.entry("/publicacion",
                    "Está viendo la ficha de un inmueble. Puede ver fotos, ubicación y contactar al agente con el botón Contactar."),
            Map.entry("/faq",
                    "Está en Preguntas Frecuentes."),
            Map.entry("/gestionar-usuarios",
                    "Es un administrador gestionando los usuarios del sistema."),
            Map.entry("/reportes",
                    "Es un administrador en la sección de reportes (descarga de Excel)."),
            Map.entry("/",
                    "Está en la página principal, donde se buscan y filtran inmuebles por modalidad, ubicación y precio."));

    /**
     * Devuelve la guía de la pantalla cuyo prefijo coincide con la ruta dada (match más
     * largo). Si no hay coincidencia o la página es nula/vacía, devuelve null.
     */
    public static String resolver(String pagina) {
        if (pagina == null || pagina.isBlank()) {
            return null;
        }
        String mejorPrefijo = null;
        String mejorGuia = null;
        for (Map.Entry<String, String> entry : PAGE_CONTEXTS) {
            String prefijo = entry.getKey();
            boolean coincide = prefijo.equals("/")
                    ? pagina.equals("/")
                    : pagina.equals(prefijo) || pagina.startsWith(prefijo + "/");
            if (coincide && (mejorPrefijo == null || prefijo.length() > mejorPrefijo.length())) {
                mejorPrefijo = prefijo;
                mejorGuia = entry.getValue();
            }
        }
        return mejorGuia;
    }
}
