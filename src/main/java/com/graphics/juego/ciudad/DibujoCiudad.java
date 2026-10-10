package com.graphics.juego.ciudad;

import com.graphics.juego.motor.Renderizador;

/**
 * Dibuja la ciudad a partir de MapaCiudad: suelo, borde, calles, manzanas,
 * edificios y parques. Todo se recorre desde la matriz, nada esta escrito a mano.
 */
public class DibujoCiudad {

    private final MapaCiudad mapa;

    public DibujoCiudad(MapaCiudad mapa) {
        this.mapa = mapa;
    }

    /**
     * @param detalle false en el minimapa: omite objetos pequenos que no se verian.
     */
    public void dibujar(Renderizador r, boolean noche, boolean detalle) {
        dibujarSuelo(r, detalle);
        for (int f = 0; f < mapa.filas(); f++) {
            for (int c = 0; c < mapa.columnas(); c++) {
                float x = mapa.centroX(c);
                float z = mapa.centroZ(f);
                if (mapa.esCalle(f, c)) {
                    if (detalle) {
                        dibujarMarcasCalle(r, f, c, x, z);
                        if (mapa.esCruce(f, c)) {
                            dibujarPasosPeatonales(r, f, c, x, z);
                        }
                    }
                } else {
                    // Acera elevada que ocupa toda la celda
                    r.caja(x, 0.15f, z, MapaCiudad.CELDA, 0.3f, MapaCiudad.CELDA, 0.60f, 0.62f, 0.64f);
                    if (mapa.esEdificio(f, c)) {
                        dibujarEdificio(r, f, c, x, z);
                        if (detalle) {
                            dibujarVentanas(r, f, c, x, z, noche);
                        }
                    } else {
                        r.caja(x, 0.32f, z, 9, 0.1f, 9, 0.22f, 0.48f, 0.26f); // cesped
                        if (detalle) {
                            dibujarParque(r, x, z);
                        }
                    }
                }
            }
        }
    }

    /** Suelo de asfalto del tamano exacto del mapa, terreno exterior y barrera en el borde. */
    private void dibujarSuelo(Renderizador r, boolean detalle) {
        float anchoMapa = mapa.limiteX() * 2;
        float largoMapa = mapa.limiteZ() * 2;
        r.caja(0, -0.25f, 0, anchoMapa, 0.5f, largoMapa, 0.17f, 0.18f, 0.21f);
        if (!detalle) {
            return;
        }
        // Terreno exterior (no circulable), algo mas bajo que el asfalto
        r.caja(0, -0.35f, 0, anchoMapa * 4, 0.5f, largoMapa * 4, 0.20f, 0.30f, 0.18f);
        // Barrera en el limite de circulacion
        float lx = mapa.limiteX();
        float lz = mapa.limiteZ();
        r.caja(0, 0.4f, -lz - 0.25f, anchoMapa + 1, 0.8f, 0.5f, 0.75f, 0.75f, 0.72f);
        r.caja(0, 0.4f, lz + 0.25f, anchoMapa + 1, 0.8f, 0.5f, 0.75f, 0.75f, 0.72f);
        r.caja(-lx - 0.25f, 0.4f, 0, 0.5f, 0.8f, largoMapa + 1, 0.75f, 0.75f, 0.72f);
        r.caja(lx + 0.25f, 0.4f, 0, 0.5f, 0.8f, largoMapa + 1, 0.75f, 0.75f, 0.72f);
    }

    /** Lineas discontinuas en los tramos rectos; los cruces quedan despejados. */
    private void dibujarMarcasCalle(Renderizador r, int f, int c, float x, float z) {
        boolean norteSur = mapa.esCalle(f - 1, c) || mapa.esCalle(f + 1, c);
        boolean esteOeste = mapa.esCalle(f, c - 1) || mapa.esCalle(f, c + 1);
        if (mapa.esCruce(f, c)) {
            return;
        }
        // Tres marcas en el centro del tramo; los extremos quedan libres para los pasos peatonales
        for (int d = -2; d <= 2; d += 2) {
            if (esteOeste && !norteSur) {
                r.caja(x + d, 0.02f, z, 1.2f, 0.03f, 0.15f, 1, 0.84f, 0.35f);
            } else if (norteSur && !esteOeste) {
                r.caja(x, 0.02f, z + d, 0.15f, 0.03f, 1.2f, 1, 0.84f, 0.35f);
            }
        }
    }

    /**
     * Paso peatonal (cebra) en cada calle que llega al cruce, pegado a las esquinas
     * de las manzanas. Las franjas van paralelas al trafico y cruzan todo el ancho.
     */
    private void dibujarPasosPeatonales(Renderizador r, int f, int c, float x, float z) {
        float distancia = MapaCiudad.CELDA / 2 + 1.2f;
        for (int d = 0; d < 4; d++) {
            if (!mapa.esCalle(f + MapaCiudad.DF[d], c + MapaCiudad.DC[d])) {
                continue;
            }
            float dirX = MapaCiudad.DC[d];
            float dirZ = MapaCiudad.DF[d];
            float cx = x + dirX * distancia;
            float cz = z + dirZ * distancia;
            for (float t = -4; t <= 4.01f; t += 1) {
                // perpendicular a la calle = (-dirZ, dirX)
                float px = cx - dirZ * t;
                float pz = cz + dirX * t;
                float sx = dirX != 0 ? 1.6f : 0.5f;
                float sz = dirZ != 0 ? 1.6f : 0.5f;
                r.caja(px, 0.025f, pz, sx, 0.03f, sz, 0.88f, 0.88f, 0.85f);
            }
        }
    }

    /** Parque: senderos en cruz, cuatro arboles, dos bancos, basureros y una fuente. */
    private void dibujarParque(Renderizador r, float x, float z) {
        float suelo = 0.37f; // cara superior del cesped
        r.caja(x, suelo + 0.005f, z, 9, 0.02f, 1.3f, 0.70f, 0.64f, 0.50f); // sendero este-oeste
        r.caja(x, suelo + 0.005f, z, 1.3f, 0.02f, 9, 0.70f, 0.64f, 0.50f); // sendero norte-sur

        for (float dx : new float[] { -2.8f, 2.8f }) {
            for (float dz : new float[] { -2.8f, 2.8f }) {
                float ax = x + dx;
                float az = z + dz;
                r.caja(ax, suelo + 1.0f, az, 0.35f, 2.0f, 0.35f, 0.40f, 0.25f, 0.13f); // tronco
                r.caja(ax, suelo + 2.8f, az, 2.4f, 1.8f, 2.4f, 0.13f, 0.42f, 0.20f);  // copa
                r.caja(ax, suelo + 4.1f, az, 1.5f, 1.0f, 1.5f, 0.17f, 0.50f, 0.24f);  // copa superior
            }
        }

        banco(r, x - 2.6f, z + 1.15f, suelo, false);
        banco(r, x + 2.6f, z - 1.15f, suelo, true);
        // Basureros junto a los bancos
        r.caja(x - 1.2f, suelo + 0.4f, z + 1.15f, 0.45f, 0.8f, 0.45f, 0.20f, 0.35f, 0.25f);
        r.caja(x + 1.2f, suelo + 0.4f, z - 1.15f, 0.45f, 0.8f, 0.45f, 0.20f, 0.35f, 0.25f);

        // Fuente en el centro
        r.caja(x, suelo + 0.25f, z, 1.9f, 0.5f, 1.9f, 0.62f, 0.62f, 0.60f);
        r.caja(x, suelo + 0.47f, z, 1.5f, 0.06f, 1.5f, 0.25f, 0.50f, 0.80f);
        r.caja(x, suelo + 0.9f, z, 0.3f, 0.9f, 0.3f, 0.62f, 0.62f, 0.60f);
    }

    /** Banco a lo largo del eje X; el respaldo queda al sur o al norte del asiento. */
    private void banco(Renderizador r, float x, float z, float suelo, boolean respaldoAlSur) {
        float lado = respaldoAlSur ? 1 : -1;
        r.caja(x, suelo + 0.45f, z, 1.8f, 0.1f, 0.55f, 0.55f, 0.32f, 0.15f);                  // asiento
        r.caja(x, suelo + 0.78f, z + lado * 0.25f, 1.8f, 0.45f, 0.08f, 0.55f, 0.32f, 0.15f); // respaldo
        r.caja(x - 0.75f, suelo + 0.2f, z, 0.1f, 0.4f, 0.5f, 0.15f, 0.15f, 0.16f);          // patas
        r.caja(x + 0.75f, suelo + 0.2f, z, 0.1f, 0.4f, 0.5f, 0.15f, 0.15f, 0.16f);
    }

    private void dibujarEdificio(Renderizador r, int f, int c, float x, float z) {
        MapaCiudad.Edificio e = mapa.edificio(f, c);
        float base = 0.3f; // altura de la acera
        r.caja(x, base + e.alturaBase() / 2, z, 7, e.alturaBase(), 7, e.r(), e.g(), e.b());
        float techo = base + e.alturaBase();
        if (e.tieneTorre()) {
            float alto = e.altura() - e.alturaBase();
            r.caja(x, techo + 0.15f, z, 7.3f, 0.3f, 7.3f, e.r() * 0.6f, e.g() * 0.6f, e.b() * 0.6f);
            r.caja(x, techo + alto / 2, z, 5, alto, 5, e.r() * 0.9f, e.g() * 0.9f, e.b() * 0.95f);
            techo += alto;
        }
        // Cornisa del techo
        float lado = e.tieneTorre() ? 5.3f : 7.3f;
        r.caja(x, techo + 0.15f, z, lado, 0.3f, lado, 0.22f, 0.24f, 0.28f);
    }

    /**
     * Ventanas en las cuatro fachadas, por pisos de 2.2 unidades. De noche la mayoria
     * se ven encendidas (emision: color calido propio); de dia son vidrio que recibe luz.
     */
    private void dibujarVentanas(Renderizador r, int f, int c, float x, float z, boolean noche) {
        MapaCiudad.Edificio e = mapa.edificio(f, c);
        float base = 0.3f;
        // Cuerpo principal: fachada de 7 unidades, 3 ventanas por piso
        ventanasDeBloque(r, f, c, x, z, 3.5f, base + 1.4f, base + e.alturaBase() - 0.6f, 3, noche, 0);
        if (e.tieneTorre()) {
            // Torre: fachada de 5 unidades, 2 ventanas por piso
            ventanasDeBloque(r, f, c, x, z, 2.5f, base + e.alturaBase() + 1.2f, base + e.altura() - 0.5f, 2, noche, 100);
        }
    }

    private void ventanasDeBloque(Renderizador r, int f, int c, float x, float z, float mitad,
            float yInicio, float yFin, int porFachada, boolean noche, int semilla) {
        float separacion = mitad * 2 / porFachada;
        float cara = mitad + 0.03f; // apenas por fuera de la pared para que no parpadee
        int piso = 0;
        for (float y = yInicio; y + 0.5f <= yFin; y += 2.2f, piso++) {
            for (int i = 0; i < porFachada; i++) {
                float d = -mitad + separacion * (i + 0.5f);
                for (int fachada = 0; fachada < 4; fachada++) {
                    boolean encendida = noche && encendida(f, c, piso + semilla, i, fachada);
                    r.emision(encendida);
                    float cr;
                    float cg;
                    float cb;
                    if (encendida) {
                        cr = 1.0f;
                        cg = 0.82f;
                        cb = 0.45f;
                    } else if (noche) {
                        cr = 0.08f;
                        cg = 0.10f;
                        cb = 0.14f;
                    } else {
                        cr = 0.32f;
                        cg = 0.45f;
                        cb = 0.58f;
                    }
                    switch (fachada) {
                        case 0 -> r.caja(x + d, y, z - cara, 0.95f, 1.1f, 0.05f, cr, cg, cb); // norte
                        case 1 -> r.caja(x + d, y, z + cara, 0.95f, 1.1f, 0.05f, cr, cg, cb); // sur
                        case 2 -> r.caja(x - cara, y, z + d, 0.05f, 1.1f, 0.95f, cr, cg, cb); // oeste
                        default -> r.caja(x + cara, y, z + d, 0.05f, 1.1f, 0.95f, cr, cg, cb); // este
                    }
                }
            }
        }
        r.emision(false);
    }

    /** Patron reproducible: alrededor del 70% de las ventanas encendidas. */
    private static boolean encendida(int f, int c, int piso, int i, int fachada) {
        int h = f * 92821 + c * 68917 + piso * 7919 + i * 104729 + fachada * 31337;
        h ^= h >>> 11;
        h *= 0x45d9f3b;
        h ^= h >>> 16;
        return Math.floorMod(h, 10) < 7;
    }
}
