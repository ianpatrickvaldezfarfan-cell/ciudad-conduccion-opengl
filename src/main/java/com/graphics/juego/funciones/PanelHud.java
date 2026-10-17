package com.graphics.juego.funciones;

/**
 * Contenido de la interfaz superpuesta: panel de estado (velocidad, luces, camara,
 * sector, chata, entrega), ayuda de controles (H), menu de pausa (P), aviso de entrega,
 * pantalla de victoria y rotulos del minimapa.
 */
public class PanelHud {

    /** Datos que se muestran en este cuadro. */
    public record Datos(
            int kmh, float fraccionVelocidad, boolean faros, boolean noche, String camara, String sector,
            int completadas, int total, String destino, int distancia, String rumbo, float tiempo,
            boolean terminado, boolean avisoEntrega, boolean minimapa, int[] recuadroMinimapa,
            int vehiculosAutonomos, boolean chata, boolean avisoSinEspacio) {
    }

    private static final String[][] CONTROLES = {
            { "W / FLECHA ARRIBA", "ACELERAR" },
            { "S / FLECHA ABAJO", "FRENAR Y REVERSA" },
            { "A D / FLECHAS", "GIRAR" },
            { "ESPACIO", "FRENO FUERTE" },
            { "C", "CAMBIAR CAMARA" },
            { "Q / E", "GIRAR VISTA PANORAMICA" },
            { "N", "DIA / NOCHE" },
            { "F", "FAROS ON / OFF" },
            { "M", "MOSTRAR / OCULTAR MAPA" },
            { "T", "ENGANCHAR / SOLTAR CHATA" },
            { "R", "REINICIAR RECORRIDO" },
            { "H", "MOSTRAR / OCULTAR AYUDA" },
            { "P", "PAUSA" },
            { "ESC", "SALIR" }
    };

    private boolean ayuda = true;

    public boolean ayudaVisible() {
        return ayuda;
    }

    public void alternarAyuda() {
        ayuda = !ayuda;
    }

    /** Escala de los pixeles de la fuente segun el alto de la ventana. */
    private static float escala(int alto) {
        return Math.max(1, Math.min(4, Math.round(alto / 330f)));
    }

    public void dibujar(Hud hud, int ancho, int alto, Datos d, boolean pausado) {
        hud.comenzar(ancho, alto);
        float e = escala(alto);
        panelEstado(hud, alto, e, d);
        rotulosMinimapa(hud, e, d, alto);
        if (d.avisoEntrega() && !d.terminado()) {
            centrado(hud, ancho, alto * 0.22f, e * 2, "ENTREGA COMPLETADA!", 0.3f, 1, 0.4f, true);
        }
        if (d.avisoSinEspacio()) {
            centrado(hud, ancho, alto * 0.30f, e, "NO HAY ESPACIO PARA ENGANCHAR LA CHATA AQUI", 1, 0.6f, 0.3f, true);
        }
        if (d.terminado()) {
            victoria(hud, ancho, alto, e, d);
        }
        if (pausado) {
            pausa(hud, ancho, alto, e);
        } else if (ayuda) {
            ayuda(hud, ancho, alto, e, 16 * e, 16 * e);
        } else {
            hud.texto(12 * e / 2, 10 * e / 2, e, "H: AYUDA   P: PAUSA", 0.85f, 0.88f, 0.92f);
        }
        hud.terminar();
    }

    // ===================== Panel de estado (esquina inferior izquierda) =====================

    private void panelEstado(Hud hud, int alto, float e, Datos d) {
        String[] lineas = {
                "VELOCIDAD: " + d.kmh() + " KM/H",
                "FAROS: " + (d.faros() ? "ENCENDIDOS" : "APAGADOS") + "   " + (d.noche() ? "NOCHE" : "DIA"),
                "CAMARA: " + d.camara(),
                "CHATA: " + (d.chata() ? "SI" : "NO") + "   (T)",
                "SECTOR: " + d.sector(),
                d.terminado() ? "ENTREGAS: " + d.total() + "/" + d.total() + " COMPLETAS"
                        : "ENTREGA " + (d.completadas() + 1) + "/" + d.total() + ": " + d.destino(),
                d.terminado() ? "TIEMPO FINAL: " + (int) d.tiempo() + " S"
                        : "DESTINO A " + d.distancia() + " M AL " + d.rumbo() + "   TIEMPO " + (int) d.tiempo() + " S",
                "TRAFICO: " + d.vehiculosAutonomos() + " VEHICULOS AUTONOMOS"
        };
        float renglon = Hud.altoTexto(e) + 5 * e;
        float anchoMaximo = 0;
        for (String l : lineas) {
            anchoMaximo = Math.max(anchoMaximo, Hud.anchoTexto(l, e));
        }
        float barra = 6 * e;
        float relleno = 8 * e;
        float w = anchoMaximo + relleno * 2;
        float h = lineas.length * renglon + barra + relleno * 2 + 3 * e;
        float x = 8 * e;
        float y = alto - h - 8 * e;
        hud.rect(x, y, w, h, 0.03f, 0.05f, 0.08f, 0.72f);
        hud.rect(x, y, 3 * e / 2, h, 0.95f, 0.75f, 0.15f, 0.95f);
        float ty = y + relleno;
        for (int i = 0; i < lineas.length; i++) {
            float[] c = i == 0 ? new float[] { 1, 1, 1 }
                    : i == 5 ? new float[] { 1, 0.82f, 0.30f } : new float[] { 0.82f, 0.88f, 0.95f };
            hud.texto(x + relleno, ty, e, lineas[i], c[0], c[1], c[2]);
            ty += renglon;
            if (i == 0) {
                // Barra de velocidad
                float largo = anchoMaximo;
                hud.rect(x + relleno, ty - 2 * e, largo, barra, 0.2f, 0.22f, 0.26f, 0.9f);
                float f = Math.max(0, Math.min(1, d.fraccionVelocidad()));
                hud.rect(x + relleno, ty - 2 * e, largo * f, barra, 0.3f + 0.7f * f, 0.9f - 0.5f * f, 0.3f, 1);
                ty += barra + 3 * e;
            }
        }
    }

    // ===================== Minimapa =====================

    private void rotulosMinimapa(Hud hud, float e, Datos d, int alto) {
        int[] q = d.recuadroMinimapa();
        if (!d.minimapa() || q == null) {
            return;
        }
        // El recuadro viene en coordenadas OpenGL (y desde abajo): se convierte a y desde arriba
        float izquierda = q[0];
        float arriba = alto - (q[1] + q[2]);
        float abajo = alto - q[1];
        float centroX = izquierda + q[2] / 2f;
        hud.rect(centroX - 6 * e, arriba + 3 * e, 12 * e, 11 * e, 0.05f, 0.07f, 0.1f, 0.85f);
        hud.texto(centroX - Hud.anchoTexto("N", e) / 2, arriba + 5 * e, e, "N", 1, 0.3f, 0.25f);
        String etiqueta = "MAPA (M)";
        hud.texto(izquierda + q[2] - Hud.anchoTexto(etiqueta, e), abajo + 6 * e, e, etiqueta, 0.85f, 0.88f, 0.92f);
    }

    // ===================== Ayuda, pausa y victoria =====================

    private void ayuda(Hud hud, int ancho, int alto, float e, float x, float y) {
        float renglon = Hud.altoTexto(e) + 4 * e;
        float columna = 0;
        for (String[] c : CONTROLES) {
            columna = Math.max(columna, Hud.anchoTexto(c[0], e));
        }
        float anchoAccion = 0;
        for (String[] c : CONTROLES) {
            anchoAccion = Math.max(anchoAccion, Hud.anchoTexto(c[1], e));
        }
        float relleno = 8 * e;
        String titulo = "CONTROLES";
        float w = columna + anchoAccion + 14 * e + relleno * 2;
        float h = (CONTROLES.length + 1.6f) * renglon + relleno * 2 + renglon;
        hud.rect(x, y, w, h, 0.03f, 0.05f, 0.08f, 0.75f);
        hud.texto(x + relleno, y + relleno, e, titulo, 1, 0.82f, 0.3f);
        float ty = y + relleno + renglon * 1.6f;
        for (String[] c : CONTROLES) {
            hud.texto(x + relleno, ty, e, c[0], 0.6f, 0.9f, 1);
            hud.texto(x + relleno + columna + 14 * e, ty, e, c[1], 0.9f, 0.92f, 0.95f);
            ty += renglon;
        }
        hud.texto(x + relleno, ty + 2 * e, e, "LLEGA AL DESTINO DORADO Y FRENA", 1, 0.82f, 0.3f);
    }

    private void pausa(Hud hud, int ancho, int alto, float e) {
        hud.rect(0, 0, ancho, alto, 0, 0, 0, 0.35f);
        centrado(hud, ancho, alto * 0.12f, e * 3, "PAUSA", 1, 1, 1, false);
        centrado(hud, ancho, alto * 0.12f + 30 * e, e, "PRESIONA P PARA CONTINUAR", 0.85f, 0.88f, 0.92f, false);
        float anchoAyuda = 300 * e;
        ayuda(hud, ancho, alto, e, Math.max(8, (ancho - anchoAyuda) / 2), alto * 0.12f + 48 * e);
    }

    private void victoria(Hud hud, int ancho, int alto, float e, Datos d) {
        float y = alto * 0.3f;
        hud.rect(0, y - 12 * e, ancho, 52 * e, 0.02f, 0.15f, 0.06f, 0.8f);
        centrado(hud, ancho, y, e * 2.5f, "GANASTE!", 1, 0.85f, 0.25f, false);
        centrado(hud, ancho, y + 24 * e, e,
                d.total() + " ENTREGAS EN " + (int) d.tiempo() + " S  -  PRESIONA R PARA UN NUEVO RECORRIDO",
                0.9f, 1, 0.9f, false);
    }

    private void centrado(Hud hud, int ancho, float y, float escala, String texto, float r, float g, float b,
            boolean fondo) {
        float w = Hud.anchoTexto(texto, escala);
        float x = (ancho - w) / 2;
        if (fondo) {
            hud.rect(x - 8 * escala / 2, y - 6 * escala / 2, w + 8 * escala, Hud.altoTexto(escala) + 6 * escala,
                    0.03f, 0.05f, 0.08f, 0.75f);
        }
        hud.texto(x, y, escala, texto, r, g, b);
    }
}
