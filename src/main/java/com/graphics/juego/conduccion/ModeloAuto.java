package com.graphics.juego.conduccion;

import com.graphics.juego.motor.Renderizador;

/**
 * Modelo 3D de un auto hecho con cajas. Lo usan el jugador y el trafico autonomo.
 * Las piezas se describen en coordenadas locales del auto (frente = -Z local) y se
 * giran y trasladan juntas.
 */
public final class ModeloAuto {

    /** Posicion local de los dos faros delanteros (X lateral, Y altura, Z frente). */
    public static final float FARO_LATERAL = 0.55f;
    public static final float FARO_ALTURA = 0.68f;
    public static final float FARO_FRENTE = 1.36f;

    private ModeloAuto() {
    }

    /**
     * @param color   color de la carroceria
     * @param noche   de noche faros y luces traseras se ven encendidos (emision)
     * @param faros   faros delanteros encendidos
     */
    public static void dibujar(Renderizador r, float x, float z, float angulo, float[] color,
            boolean noche, boolean faros) {
        Pieza p = new Pieza(r, x, z, angulo);
        p.caja(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, color[0], color[1], color[2]);     // carroceria
        p.caja(0, 1.12f, 0.12f, 1.3f, 0.5f, 1.25f, 0.20f, 0.30f, 0.38f);           // cabina
        p.caja(0, 1.12f, -0.52f, 1.22f, 0.42f, 0.04f, 0.45f, 0.70f, 0.85f);        // parabrisas
        for (float lado : new float[] { -0.86f, 0.86f }) {
            for (float eje : new float[] { -0.82f, 0.82f }) {
                p.caja(lado, 0.36f, eje, 0.26f, 0.56f, 0.6f, 0.05f, 0.06f, 0.07f); // ruedas
            }
        }
        for (float lado : new float[] { -FARO_LATERAL, FARO_LATERAL }) {
            r.emision(faros);
            p.caja(lado, FARO_ALTURA, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, faros ? 0.75f : 0.55f);
            r.emision(noche);
            p.caja(lado, FARO_ALTURA, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.06f, 0.05f);
            r.emision(false);
        }
    }

    /** Transforma piezas del espacio local del auto al mundo. */
    private record Pieza(Renderizador r, float x, float z, float angulo) {
        void caja(float lx, float ly, float lz, float sx, float sy, float sz, float cr, float cg, float cb) {
            float c = (float) Math.cos(angulo);
            float s = (float) Math.sin(angulo);
            float mundoX = x + c * lx + s * lz;
            float mundoZ = z - s * lx + c * lz;
            r.cajaGirada(mundoX, ly, mundoZ, sx, sy, sz, cr, cg, cb, angulo);
        }
    }

    /** Posicion en el mundo de un faro: lado = -1 izquierdo, +1 derecho. Devuelve {x, y, z}. */
    public static float[] posicionFaro(float x, float z, float angulo, int lado) {
        float c = (float) Math.cos(angulo);
        float s = (float) Math.sin(angulo);
        float lx = lado * FARO_LATERAL;
        float lz = -FARO_FRENTE;
        return new float[] { x + c * lx + s * lz, FARO_ALTURA, z - s * lx + c * lz };
    }
}
