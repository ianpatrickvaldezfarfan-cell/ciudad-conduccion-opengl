package com.graphics.juego.ciudad;

import com.graphics.juego.motor.Fuente;
import com.graphics.juego.motor.Renderizador;

/**
 * Senalizacion urbana: un letrero verde con el nombre de cada sector, sobre la acera
 * sur de la primera manzana del sector (posiciones derivadas del mapa).
 * Las letras se dibujan con cajas usando la misma fuente de pixeles del HUD.
 * De noche las letras brillan (letrero retroiluminado).
 */
public class Letreros {

    private static final float PIXEL = 0.2f;
    private final MapaCiudad mapa;

    public Letreros(MapaCiudad mapa) {
        this.mapa = mapa;
    }

    public void dibujar(Renderizador r, boolean noche) {
        for (MapaCiudad.Sector s : mapa.listaSectores()) {
            int[] manzana = mapa.primeraManzana(s.letra());
            if (manzana == null) {
                continue;
            }
            float x = mapa.centroX(manzana[1]);
            float z = mapa.centroZ(manzana[0]) + MapaCiudad.CELDA / 2 - 0.45f;
            letrero(r, x, z, s.letrero(), noche);
        }
    }

    /** Letrero mirando al sur (+Z), con el texto centrado en x. */
    private void letrero(Renderizador r, float x, float z, String texto, boolean noche) {
        float anchoTexto = texto.length() * (Fuente.ANCHO + 1) * PIXEL - PIXEL;
        float anchoPanel = anchoTexto + 0.8f;
        float altoPanel = Fuente.ALTO * PIXEL + 0.6f;
        float centroY = 3.4f;
        // Dos postes y el panel
        r.caja(x - anchoPanel / 2 + 0.3f, 0.3f + 1.5f, z, 0.15f, 3.0f, 0.15f, 0.30f, 0.32f, 0.35f);
        r.caja(x + anchoPanel / 2 - 0.3f, 0.3f + 1.5f, z, 0.15f, 3.0f, 0.15f, 0.30f, 0.32f, 0.35f);
        r.caja(x, centroY, z, anchoPanel, altoPanel, 0.12f, 0.05f, 0.40f, 0.20f);
        r.caja(x, centroY, z + 0.04f, anchoPanel - 0.2f, altoPanel - 0.2f, 0.06f, 0.07f, 0.48f, 0.25f);
        // Letras pixel a pixel, en la cara sur del panel
        r.emision(noche);
        float inicioX = x - anchoTexto / 2 + PIXEL / 2;
        float arriba = centroY + Fuente.ALTO * PIXEL / 2 - PIXEL / 2;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            for (int fila = 0; fila < Fuente.ALTO; fila++) {
                for (int col = 0; col < Fuente.ANCHO; col++) {
                    if (Fuente.pixel(c, col, fila)) {
                        float px = inicioX + (i * (Fuente.ANCHO + 1) + col) * PIXEL;
                        float py = arriba - fila * PIXEL;
                        r.caja(px, py, z + 0.09f, PIXEL, PIXEL, 0.03f, 0.95f, 0.97f, 0.92f);
                    }
                }
            }
        }
        r.emision(false);
    }
}
