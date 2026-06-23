package com.sw.api.modules.publicacion.model;

/**
 * Formato de salida del modelo 3D generado a partir del video.
 *
 * <ul>
 *   <li>{@link #SPLAT}: máxima calidad, archivo más pesado y más caro.</li>
 *   <li>{@link #SOG}: más ligero, algo menos de calidad y más barato.</li>
 * </ul>
 *
 * Cada formato se procesa en un endpoint de Runpod distinto (ver RunpodService).
 */
public enum Formato3D {
    SPLAT,
    SOG
}
