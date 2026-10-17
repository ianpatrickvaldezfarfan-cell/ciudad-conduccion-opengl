package com.graphics.juego.conduccion;

import com.graphics.juego.motor.Renderizador;

/**
 * Modelo 3D de la chata hecho solo con cajas.
 *
 * Basado en una chata real de 1,30 x 2 m (1 tonelada), ajustada a la escala del auto del
 * juego (1.65 x 2.6): caja de 1.30 x 1.60, lanza de 0.8 -> largo total 2.4 (un poco mas
 * corta que el auto) y ancho con ruedas 1.7.
 *
 * Coordenadas locales: origen en el eje de las ruedas, frente = -Z local (hacia el auto).
 */
public final class ModeloChata {

    // Colores
    private static final float[] ROJO = { 0.78f, 0.10f, 0.08f };       // tubo metalico
    private static final float[] MADERA = { 0.78f, 0.60f, 0.38f };     // barandas
    private static final float[] PISO = { 0.62f, 0.45f, 0.26f };       // piso de tablas
    private static final float[] LLANTA = { 0.05f, 0.05f, 0.06f };
    private static final float[] ARO = { 0.62f, 0.64f, 0.67f };
    private static final float[] ACOPLE = { 0.50f, 0.52f, 0.55f };

    // Medidas de la caja
    private static final float MEDIO_ANCHO = 0.65f;   // caja de 1.30
    private static final float MEDIO_LARGO = 0.80f;   // caja de 1.60
    private static final float PISO_Y = 0.50f;        // centro del piso
    private static final float BARANDA_ARRIBA = 0.97f;

    private ModeloChata() {
    }

    /** Dibuja la chata en su posicion y angulo. De noche las luces traseras brillan. */
    public static void dibujar(Renderizador r, Chata chata, boolean noche) {
        Pieza p = new Pieza(r, chata.ejeX, chata.ejeZ, chata.angulo);
        float largo = MEDIO_LARGO * 2;
        float ancho = MEDIO_ANCHO * 2;

        // 1) Chasis de tubo rojo: dos largueros y dos travesanos debajo del piso
        for (float lado : new float[] { -0.58f, 0.58f }) {
            p.caja(lado, 0.43f, 0, 0.08f, 0.08f, largo, ROJO);
        }
        for (float z : new float[] { -0.72f, 0.72f }) {
            p.caja(0, 0.43f, z, ancho - 0.1f, 0.08f, 0.08f, ROJO);
        }

        // 2) Piso de madera
        p.caja(0, PISO_Y, 0, ancho, 0.06f, largo, PISO);

        // 3) Barandas de tablas: dos tablas por lado (laterales, delantera y trasera)
        for (float y : new float[] { 0.64f, 0.85f }) {
            for (float lado : new float[] { -1, 1 }) {
                p.caja(lado * (MEDIO_ANCHO - 0.025f), y, 0, 0.05f, 0.17f, largo - 0.04f, MADERA); // laterales
                p.caja(0, y, lado * (MEDIO_LARGO - 0.025f), ancho - 0.04f, 0.17f, 0.05f, MADERA); // frente y atras
            }
        }

        // 4) Postes rojos verticales entre las tablas y tubo rojo superior
        float altoPoste = BARANDA_ARRIBA - PISO_Y;
        float centroPoste = PISO_Y + altoPoste / 2;
        for (float lado : new float[] { -1, 1 }) {
            for (float z : new float[] { -MEDIO_LARGO, 0, MEDIO_LARGO }) {
                p.caja(lado * (MEDIO_ANCHO + 0.01f), centroPoste, z * 0.97f, 0.07f, altoPoste, 0.07f, ROJO);
            }
            p.caja(0, centroPoste, lado * (MEDIO_LARGO + 0.01f), 0.07f, altoPoste, 0.07f, ROJO); // centro frente/atras
            p.caja(lado * (MEDIO_ANCHO + 0.01f), BARANDA_ARRIBA, 0, 0.06f, 0.06f, largo, ROJO);
            p.caja(0, BARANDA_ARRIBA, lado * (MEDIO_LARGO + 0.01f), ancho, 0.06f, 0.06f, ROJO);
        }

        // 5) Un eje con dos ruedas (llanta negra y aro gris) y guardabarros rojos
        p.caja(0, 0.25f, 0, 1.45f, 0.07f, 0.07f, ACOPLE); // eje
        for (float lado : new float[] { -0.76f, 0.76f }) {
            p.caja(lado, 0.25f, 0, 0.18f, 0.5f, 0.5f, LLANTA);
            p.caja(lado, 0.25f, 0, 0.2f, 0.24f, 0.24f, ARO);
            p.caja(lado, 0.56f, 0, 0.24f, 0.05f, 0.62f, ROJO);
        }

        // 6) Lanza en V: dos tubos desde las esquinas delanteras hasta el enganche
        float puntaZ = -Chata.LANZA + 0.08f;
        for (float lado : new float[] { -1, 1 }) {
            float inicioX = lado * 0.5f;
            float inicioZ = -MEDIO_LARGO;
            float dx = 0 - inicioX;
            float dz = puntaZ - inicioZ;
            float largoTubo = (float) Math.hypot(dx, dz);
            float giroTubo = (float) Math.atan2(dx, dz); // gira el eje Z local de la caja hacia (dx, dz)
            p.cajaGirada(inicioX + dx / 2, 0.47f, inicioZ + dz / 2, 0.07f, 0.07f, largoTubo, ROJO, giroTubo);
        }
        p.caja(0, 0.47f, -MEDIO_LARGO - 0.05f, 1.0f, 0.07f, 0.07f, ROJO); // travesano de la lanza

        // 7) Acople gris en la punta (queda justo sobre el enganche del auto)
        p.caja(0, 0.52f, -Chata.LANZA + 0.05f, 0.2f, 0.12f, 0.24f, ACOPLE);

        // 8) Dos luces traseras rojas (emision de noche)
        r.emision(noche);
        for (float lado : new float[] { -0.45f, 0.45f }) {
            p.caja(lado, 0.62f, MEDIO_LARGO + 0.05f, 0.27f, 0.13f, 0.04f, 1.0f, 0.12f, 0.08f);
        }
        r.emision(false);
    }

    /** Barra de enganche (tiro) con bola en la parte trasera del auto. */
    public static void dibujarEnganche(Renderizador r, Auto auto) {
        Pieza p = new Pieza(r, auto.x, auto.z, auto.angulo);
        // En el auto la parte trasera es +Z local; el enganche esta a Chata.ENGANCHE del centro
        p.caja(0, 0.45f, 1.38f, 0.12f, 0.1f, 0.24f, ACOPLE);
        p.caja(0, 0.55f, Chata.ENGANCHE, 0.13f, 0.13f, 0.13f, ACOPLE);
    }

    /** Transforma piezas del espacio local (posicion + giro) al mundo. */
    private record Pieza(Renderizador r, float x, float z, float angulo) {

        void caja(float lx, float ly, float lz, float sx, float sy, float sz, float[] color) {
            cajaGirada(lx, ly, lz, sx, sy, sz, color, 0);
        }

        void caja(float lx, float ly, float lz, float sx, float sy, float sz, float cr, float cg, float cb) {
            cajaGirada(lx, ly, lz, sx, sy, sz, new float[] { cr, cg, cb }, 0);
        }

        /** giroLocal: giro extra de la pieza respecto a la chata (para los tubos de la lanza). */
        void cajaGirada(float lx, float ly, float lz, float sx, float sy, float sz, float[] color, float giroLocal) {
            float c = (float) Math.cos(angulo);
            float s = (float) Math.sin(angulo);
            float mundoX = x + c * lx + s * lz;
            float mundoZ = z - s * lx + c * lz;
            r.cajaGirada(mundoX, ly, mundoZ, sx, sy, sz, color[0], color[1], color[2], angulo + giroLocal);
        }
    }
}
